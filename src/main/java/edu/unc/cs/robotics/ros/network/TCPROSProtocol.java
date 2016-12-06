package edu.unc.cs.robotics.ros.network;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import edu.unc.cs.robotics.ros.protocol.Protocol;
import edu.unc.cs.robotics.ros.protocol.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@see http://wiki.ros.org/ROS/TCPROS}
 */
class TCPROSProtocol implements Protocol {
    private static final Logger LOG = LoggerFactory.getLogger(TCPROSProtocol.class);

    private static final int DEFAULT_INITIAL_PACKET_BUFFER_SIZE = 512;
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    private enum State {
        INITIAL,
        HEADER,
        FIELD,
        STREAM,
        MESSAGE,
        ERROR,
    }
    interface Delegate {
        void headerRecv(String name, String value);
        void headersDone();
        void messageRecv(ByteBuffer buf);
    }


    private State _state = State.INITIAL;
    private Delegate _delegate;

    private int _headerLength;
    private int _fieldLength;
    private int _messageLength;

    private boolean _packetBuffered;

    private CharsetDecoder _charsetDecoder = US_ASCII.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);

    private ByteBuffer _packetBuffer;

    TCPROSProtocol(Delegate delegate) {
        this(delegate, DEFAULT_INITIAL_PACKET_BUFFER_SIZE);
    }

    TCPROSProtocol(Delegate delegate, int initialPacketBufferSize) {
        _delegate = delegate;
        _packetBuffer = ByteBuffer.allocate(initialPacketBufferSize)
            .order(NetworkServer.ROS_BYTE_ORDER);
    }

    private ProtocolException error(ByteBuffer buf, String msg) {
        LOG.error(msg);
        _state = State.ERROR;
        buf.position(buf.limit());
        return new ProtocolException(msg);
    }


    private ByteBuffer fillBuffer(ByteBuffer readBuffer, int count) {
        if (_packetBuffered) {
            // the packet buffer is in use, we need to fill it with exactly count bytes
            // then return it.

//            LOG.debug("filling packet buffer");

            int needed = count - _packetBuffer.position();
            if (readBuffer.remaining() <= needed) {
                _packetBuffer.put(readBuffer);
                if (_packetBuffer.position() < count) {
                    return null;
                }
            } else if (needed < 16) {
                do {
                    _packetBuffer.put(readBuffer.get());
                } while (_packetBuffer.position() < count);
            } else {
                int oldLim = readBuffer.limit();
                readBuffer.limit(readBuffer.position() + needed);
                _packetBuffer.put(readBuffer);
                readBuffer.limit(oldLim);
            }

            _packetBuffer.flip();
            assert _packetBuffer.remaining() == count;
            _packetBuffered = false;
            return _packetBuffer;
        } else if (readBuffer.remaining() < count) {
            if (readBuffer.remaining() == 0) {
                // no need to use the packet buffer when there is
                // nothing to buffer
                return null;
            }

            // reading directly from the readBuffer, but not enough bytes
            // are available.  We need to store in the packet buffer.

            _packetBuffer.clear();

            int capacity = _packetBuffer.capacity();

            if (capacity < count) {
                do {
                    capacity <<= 1;
                } while (capacity < count);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("growing packet buffer " + _packetBuffer.capacity() + " -> " + capacity);
                }
                _packetBuffer = ByteBuffer.allocate(capacity)
                    .order(NetworkServer.ROS_BYTE_ORDER);
            }

            _packetBuffered = true;
            _packetBuffer.put(readBuffer);

//            LOG.debug("using packet buffer for "+_packetBuffer.position()+" bytes (need "+count+")");
            return null;
        } else {
            return readBuffer;
        }
    }

    @Override
    public void process(ByteBuffer readBuffer) throws ProtocolException {
        assert readBuffer.order() == ByteOrder.LITTLE_ENDIAN;

        ByteBuffer buf;

        for (;;) {
            switch (_state) {
            case INITIAL:
                // initial state is looking for a 4-byte length of the
                // header

                if ((buf = fillBuffer(readBuffer, 4)) == null)
                    return;

                _headerLength = buf.getInt();

//                LOG.debug("header length: "+_headerLength);
                if (_headerLength < 0 || _headerLength > 1024*1024*1024) {
                    throw error(readBuffer, "invalid header length: " + _headerLength);
                }

                if (_headerLength == 0) {
                    headersDone();
                    continue;
                }

                _state = State.HEADER;
                // fall through
            case HEADER:
                // in header state, we're essentially looping through
                // fields until we've consumed the entire header
                // each field is preceded by a 4-byte length of header

                if (_headerLength < 4) {
                    throw error(readBuffer, "invalid header");
                }

                if ((buf = fillBuffer(readBuffer, 4)) == null)
                    return;

                _fieldLength = buf.getInt();
//                LOG.debug("header field length: "+_fieldLength+" header bytes remaining: "+(_headerLength-4)+", bytes remaining = "+readBuffer.remaining());

                if (_fieldLength < 0 || _fieldLength > 1024*1024) {
                    throw error(readBuffer, "invalid field length: "+_fieldLength);
                }
                if (_fieldLength > _headerLength) {
                    throw error(readBuffer, "field overruns past end of header");
                }
                _headerLength -= _fieldLength + 4;
                _state = State.FIELD;

                // fall through
            case FIELD:
                if ((buf = fillBuffer(readBuffer, _fieldLength)) == null)
                    return;

                processField(readBuffer, buf);
                continue;
            case STREAM:
                if ((buf = fillBuffer(readBuffer, 4)) == null) {
                    return;
                }

                _messageLength = buf.getInt();
//                LOG.debug("message length="+_messageLength+", remaining="+readBuffer.remaining());
                if (_messageLength < 0 || _messageLength > 10*1024*1024) {
                    throw error(readBuffer, "invalid message length");
                }
                _state = State.MESSAGE;
                // fall through
            case MESSAGE:
                if ((buf = fillBuffer(readBuffer, _messageLength)) == null) {
                    return;
                }

//                LOG.debug("message ready, length="+_messageLength+", "+buf);
                int oldLimit = buf.limit();
                buf.limit(buf.position() + _messageLength);
                _delegate.messageRecv(buf);
                if (buf.hasRemaining()) {
                    throw error(readBuffer, "delegate did not consume entire message buffer");
                }
                buf.limit(oldLimit);
                _state = State.STREAM;
                break;
            case ERROR:
                throw error(readBuffer, "protocol in error state");
            }
        }
    }

    private void headersDone() {
        _state = State.STREAM;
        // the charsetDecoder is only used for the headers
        // we set it to null to let it get GC'd
        _charsetDecoder = null;
        _delegate.headersDone();
    }

    private void processField(ByteBuffer readBuffer, ByteBuffer buf) throws ProtocolException {
        try {
            final int limit = buf.limit();
            buf.limit(buf.position() + _fieldLength);
            CharBuffer cbuf = _charsetDecoder.decode(buf);
            buf.limit(limit);
            final char[] array = cbuf.array();
            assert cbuf.position() == 0;
            for (int i = 0, n = cbuf.limit(); i < n; ++i) {
                if (array[i] == '=') {
                    String name = new String(array, 0, i++);
                    String value = new String(array, i, n - i);
                    headerRecv(name, value);
                    return;
                }
            }
        } catch (CharacterCodingException e) {
            throw error(readBuffer, e.toString());
        }
        throw error(readBuffer, "field is missing value");
    }

    private void headerRecv(String name, String value) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("header: [{}] = [{}]", name, value);
        }

        _delegate.headerRecv(name, value);

        if (_headerLength == 0) {
            headersDone();
        } else {
            _state = State.HEADER;
        }
    }

}
