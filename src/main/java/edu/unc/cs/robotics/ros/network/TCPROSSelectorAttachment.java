package edu.unc.cs.robotics.ros.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.unc.cs.robotics.ros.protocol.Protocol;
import edu.unc.cs.robotics.ros.protocol.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class TCPROSSelectorAttachment
    implements SelectorAttachment, TCPROSProtocol.Delegate
{
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * The maximum number of bytes to send during a gathering write.
     * TODO: use the socket's buffer size instead of a constant.
     */
    private static final int MAX_WRITE = 65535;

    private final Lock _lock = new ReentrantLock();

    protected final NetworkServer _server;
    protected final SocketChannel _channel;
//    protected final SelectionKey _key;
    protected final ByteBuffer _readBuffer;

    private final Protocol _protocol = new TCPROSProtocol(this);

    /**
     * This is our write queue.  This is filled with ByteBuffers
     * in the order in which they need to be written to the socket.
     * Each buffer is marked upon insertion to allow us to determine
     * if a buffer has been partially written.
     */
    private final Deque<ByteBuffer> _writeQueue = new ArrayDeque<>();
    /**
     * Actions to perform when a write is complete.  This is dubiously
     * useful, since we only know when a buffer is copied to the network
     * stack, and not when it has reached its destination.
     */
    private final Map<ByteBuffer, Runnable> _writeCompleteMap = new HashMap<>();
    /**
     * This is the total number of bytes in all buffers in the write queue.
     */
    private long _writeQueueBytes;
    /**
     * This is true when the head of the writeQueue is a partially
     * written buffer.  (In which case it's mark != position)
     */
    private boolean _partialWrite;
    /**
     * Buffers (re-)used to do gathering writes.
     */
    private ByteBuffer[] _writeBuffers = new ByteBuffer[16];
    /**
     * The maximum queue size.  Buffers will be removed from the queue
     * when the number of buffers exceeds this size.
     */
    private int _maxQueueSize = Integer.MAX_VALUE;

    public TCPROSSelectorAttachment(
        NetworkServer server,
        SocketChannel ch)
        throws ClosedChannelException
    {
        _server = server;
        _channel = ch;
        _readBuffer = server.sharedReadBuffer();
    }

    protected void sendErrorHeader(String msg) {
        sendHeaders(this::close, "error", msg);
    }

    protected void sendHeaders(Runnable headerSentAction, String... kvPairs) {
        if ((kvPairs.length & 1) != 0) {
            throw new AssertionError("headers must come in pairs");
        }

        int len = 0;
        for (int i = 0; i < kvPairs.length; i += 2) {
            String name = kvPairs[i];
            String value = kvPairs[i + 1];
            assert name.indexOf('=') == -1;
            // + 5 for '=' and 4-byte field length
            len += name.length() + value.length() + 5;
        }

        ByteBuffer buf = ByteBuffer.allocate(len + 4);
        buf.order(NetworkServer.ROS_BYTE_ORDER);
        buf.putInt(len);
        for (int i = 0; i < kvPairs.length; i += 2) {
            String name = kvPairs[i];
            String value = kvPairs[i + 1];
            buf.putInt(name.length() + value.length() + 1);
            appendASCII(buf, name);
            buf.put((byte)'=');
            appendASCII(buf, value);
        }
        assert !buf.hasRemaining();
        buf.flip();
        enqueueWrite(buf, headerSentAction);
    }

    private void appendASCII(ByteBuffer buf, String str) {
        for (int i=0, n=str.length() ; i<n ; ++i) {
            buf.put((byte)str.charAt(i));
        }
    }

    public void setMaxQueueSize(int maxSize) {
        _maxQueueSize = maxSize <= 0 ? Integer.MAX_VALUE : maxSize;
    }

    void enqueueWrite(ByteBuffer buf, Runnable writeCompleteAction) {
        if (!buf.hasRemaining()) {
            if (writeCompleteAction != null) {
                writeCompleteAction.run();
            }
            return;
        }

        _lock.lock();
        try {

            // we mark the buffer when it is enqueued so we can
            // know how much we've written.
            buf.mark();

            if (_writeQueue.isEmpty()) {
                // if the queue is empty we may be able to write
                // immediately.  If we can, we save a few OS calls.
                try {
                    int n = _channel.write(buf);
                    LOG.debug("write immediate "+n);
                } catch (IOException ex) {
                    LOG.warn("Error writing", ex);
                    close();
                    return;
                }

                if (!buf.hasRemaining()) {
                    if (writeCompleteAction != null) {
                        writeCompleteAction.run();
                    }
                    return;
                }

                // the queue was empty, so we need to update the
                // interests of the selector.  Updating interests
                // must happen on the selector thread otherwise an
                // active select will not get the updated interests
                // until another operation wakes it.
                _server.runOnSelectorThread(this::updateInterests);
            } else if (_maxQueueSize != Integer.MAX_VALUE) {
                // if the write queue was not empty, we may need to discard
                // the oldest entry

                int size = _writeQueue.size();
                if (_partialWrite) {
                    --size;
                }

                if (size >= _maxQueueSize) {
                    LOG.info("outgoing queue is full, discarding oldest message");
                    ByteBuffer oldest = _writeQueue.remove();

                    if (!_partialWrite) {
                        _writeQueueBytes -= oldest.remaining();
                        runWriteCompleteAction(oldest);
                    } else {
                        // if we have partially written the head of the
                        // write queue, we need to complete the write
                        // we thus discard the next-to-oldest instead.
                        ByteBuffer nextToOldest;
                        nextToOldest = _writeQueue.remove();
                        _writeQueueBytes -= nextToOldest.remaining();
                        _writeQueue.offerFirst(oldest);
                        runWriteCompleteAction(nextToOldest);
                    }
                }
            }

            LOG.debug("appending buffer with "+buf.remaining()+" bytes to write queue");
            _writeQueue.offer(buf);
            _writeQueueBytes += buf.remaining();
            if (writeCompleteAction != null) {
                _writeCompleteMap.put(buf, writeCompleteAction);
            }

        } finally {
            _lock.unlock();
        }
    }

    private void runWriteCompleteAction(ByteBuffer oldest) {
        Runnable action = _writeCompleteMap.remove(oldest);
        if (action != null) {
            action.run();
        }
    }

    void updateInterests() {
        _lock.lock();
        try {
            SelectionKey _key = _server.keyFor(_channel);
            int interests = _key.interestOps();
            if (_writeQueue.isEmpty()) {
                interests &= ~SelectionKey.OP_WRITE;
            } else {
                interests |= SelectionKey.OP_WRITE;
            }
            _key.interestOps(interests);
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public void readable(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel)key.channel();
        boolean bufferFilled;
        do {
            _readBuffer.clear();
            int n = ch.read(_readBuffer);
            LOG.debug("read "+n);
            if (n < 0) {
                LOG.debug("Read closed");
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                break;
            }
            bufferFilled = !_readBuffer.hasRemaining();

            _readBuffer.flip();
            try {
                _protocol.process(_readBuffer);
            } catch (ProtocolException ex) {
                LOG.warn("protocol error", ex);
                close();
                return;
            }

            assert !_readBuffer.hasRemaining();

            // keep reading while we fill the buffer.
            // if the read did not fill the buffer, then
            // there's no bytes remaining to read.
            // if the read filled the buffer, then its
            // possible there are more bytes to read.
        } while (bufferFilled);
    }

    @Override
    public void writable(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel)key.channel();
        _lock.lock();
        try {
            assert !_writeQueue.isEmpty();

            ByteBuffer buf;
            int nBufs = 0;
            int nBytes = 0;

            while ((buf = _writeQueue.poll()) != null && (nBytes += buf.remaining()) <= MAX_WRITE) {
                if (nBufs == _writeBuffers.length) {
                    _writeBuffers = Arrays.copyOf(_writeBuffers, nBufs*2);
                }
                _writeBuffers[nBufs++] = buf;
            }

            if (nBufs == 0) {
                // can happen if the first buffer has more bytes than MAX_WRITE
                _writeBuffers[0] = buf;
                nBufs = 1;
            } else if (buf != null) {
                // put back the last buf removed, it is not in the _writeBuffers
                _writeQueue.offerFirst(buf);
            }

            // do the gathering write.
            long n = ch.write(_writeBuffers, 0, nBufs);
            _writeQueueBytes -= n;

            // put buffers with any bytes remaining back to the head of the queue
            while (--nBufs >= 0 && (buf = _writeBuffers[nBufs]).hasRemaining()) {
                _writeQueue.offerFirst(buf);
                _writeBuffers[nBufs] = null;
            }

            // for all the buffers that did complete,
            // run their completion action.
            for (int i=0 ; i<=nBufs ; ++i) {
                assert !_writeBuffers[i].hasRemaining();
                runWriteCompleteAction(_writeBuffers[i]);
                _writeBuffers[i] = null;
            }

            if (_writeQueue.isEmpty()) {
                // if the queue is now empty, remove write interest.
                _partialWrite = false;
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            } else {
                // if there is still something left in the queue,
                // determine if we've partially written the head buffer
                // in the queue.  This is done by examining the buffer's mark().
                // which unfortunately can only be done via a reset().
                int pos = (buf = _writeQueue.peek()).position();
                _partialWrite = (buf.reset().position() != pos);
                buf.position(pos);
            }
        } finally {
            _lock.unlock();
        }

    }

    public void close() {
        LOG.info("closing channel {}", _channel);
        try {
            _channel.close();
        } catch (IOException ex) {
            LOG.warn("error closing channel", ex);
        }
    }
}
