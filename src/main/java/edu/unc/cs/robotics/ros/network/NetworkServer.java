package edu.unc.cs.robotics.ros.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import edu.unc.cs.robotics.ros.HostBindingService;
import edu.unc.cs.robotics.ros.HostNameMap;
import edu.unc.cs.robotics.ros.Names;
import edu.unc.cs.robotics.ros.Service;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.topic.PublisherLink;
import edu.unc.cs.robotics.ros.topic.Subscription;
import edu.unc.cs.robotics.ros.topic.TopicManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NetworkServer implements Runnable, Service {
    /**
     * ROS does NOT use standard network byte order.
     */
    static final ByteOrder ROS_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    static final Logger LOG = LoggerFactory.getLogger(NetworkServer.class);

    private static final int SHARED_READ_BUFFER_SIZE = 65536;
    private static final int MAX_TCPROS_CONN_QUEUE = 100;

    enum State {
        IDLE,
        STARTING,
        RUNNING,
        STOPPING
    }

    private final Names _names;
    private final Provider<TopicManager> _topicManager;
    private final HostBindingService _hostBindingService;
    private final HostNameMap _hostNameMap;

    private final Lock _lock = new ReentrantLock();
    private final Condition _startedCondition = _lock.newCondition();
    private final Condition _stoppedCondition = _lock.newCondition();

    private final Queue<Runnable> _actionQueue = new ArrayDeque<>();

    private final ByteBuffer _sharedReadBuffer = ByteBuffer.allocateDirect(SHARED_READ_BUFFER_SIZE)
        .order(ROS_BYTE_ORDER);

    private Selector _selector;
    private Thread _thread;

    // TODO: support multiple?
    private ServerSocketChannel _serverSocketChannel;
    private String _host;
    private int _port;

    private State _state = State.IDLE;

    @Inject
    public NetworkServer(Names names, Provider<TopicManager> topicManagerProvider, HostBindingService hostBindingService, HostNameMap hostNameMap) {
        _names = names;
        _topicManager = topicManagerProvider;
        _hostBindingService = hostBindingService;
        _hostNameMap = hostNameMap;
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    Names getNames() { return _names; }

    TopicManager getTopicManager() {
        return _topicManager.get();
    }

    ByteBuffer sharedReadBuffer() {
        return _sharedReadBuffer;
    }

    @Override
    public void start() {
        _lock.lock();
        try {
            if (_state != State.IDLE) {
                throw new IllegalStateException(
                    "attempt to start server that is "+_state);
            }

            _state = State.STARTING;

            listen(_hostBindingService.host(), 0);

            _selector = Selector.open();
            Thread thread = new Thread(this, "NetworkService::selector");
            thread.start();
            while (_state == State.STARTING) {
                _startedCondition.await();
            }
            LOG.info("started");
        } catch (IOException ex) {
            LOG.error("Failed to start", ex);
        } catch (InterruptedException ex) {
            // NOTE: interruptions may leave this in an invalid _state
            LOG.warn("Interrupted while waiting for start", ex);
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public void stop() {
        _lock.lock();
        try {
            if (_state == State.IDLE) {
                return;
            }

            if (_state != State.RUNNING) {
                LOG.error("stop called while "+_state);
                return;
            }

            try {
                _serverSocketChannel.close();
            } catch (IOException e) {
                LOG.warn("Error closing server socket");
            }

            runOnSelectorThread(() -> {
                _lock.lock();
                try {
                    if (_state != State.RUNNING) {
                        LOG.error("bad state while stopping: "+_state);
                    }

                    _state = State.STOPPING;
                } finally {
                    _lock.unlock();
                }

                // need to another wakeup since actions
                // are processed before the select call.
                _selector.wakeup();
            });

            while (_state != State.IDLE) {
                _stoppedCondition.await();
            }

            try {
                _selector.close();
            } catch (IOException ex) {
                LOG.warn("Error closing selector", ex);
            } finally {
                _selector = null;
            }

            LOG.info("stopped");
        } catch (InterruptedException ex) {
            LOG.warn("Interrupted while waiting to stop", ex);
        } finally {
            _serverSocketChannel = null;
            _host = null;
            _port = 0;
            _lock.unlock();
        }
    }

    private void listen(String host, int port) throws IOException {
        listen(new InetSocketAddress(host, port));
    }

    private void listen(InetSocketAddress bindAddress) throws IOException {
        assert _serverSocketChannel == null;

        _serverSocketChannel = ServerSocketChannel.open();
        _serverSocketChannel.configureBlocking(false);
        _serverSocketChannel.bind(bindAddress, MAX_TCPROS_CONN_QUEUE);

        runOnSelectorThread(() -> {
            try {
                TCPROSServerSocketAttachment att = new TCPROSServerSocketAttachment(NetworkServer.this);
                _serverSocketChannel.register(_selector, SelectionKey.OP_ACCEPT, att);
            } catch (ClosedChannelException ex) {
                LOG.error("Failed to create server socket attachment", ex);
            }
        });

        InetSocketAddress localAddress = (InetSocketAddress)_serverSocketChannel.getLocalAddress();
        _port = localAddress.getPort();
        _host = localAddress.getHostString();

        LOG.info("listening for TCPROS on "+_host+":"+_port);
    }

    SelectionKey register(SelectableChannel channel, int ops, Object att)
        throws ClosedChannelException
    {
        return channel.register(_selector, ops, att);
    }

    SelectionKey keyFor(SelectableChannel channel) {
        return channel.keyFor(_selector);
    }

    private boolean isSelectorThread() {
        return Thread.currentThread() == _thread;
    }

    void runOnSelectorThread(Runnable r) {
        LOG.debug("runOnSelectorThread");
        _lock.lock();
        try {
            _actionQueue.add(r);
            LOG.debug("action queue size = "+_actionQueue.size());
            if (_selector != null && !isSelectorThread()) {
                LOG.debug("wakeup!");
                _selector.wakeup();
            }
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public void run() {
        _lock.lock();
        try {
            _thread = Thread.currentThread();
            _state = State.RUNNING;
            _startedCondition.signal();
        } finally {
            _lock.unlock();
        }

        try {
            selectLoop();
        } catch (Throwable ex) {
            LOG.error("select loop terminated with exception", ex);
        } finally {
            LOG.debug("select loop terminated");
            _lock.lock();
            try {
                _thread = null;
                _state = State.IDLE;
                _stoppedCondition.signal();
            } finally {
                _lock.unlock();;
            }
        }
    }

    private void selectLoop() throws IOException {
        while (_state == State.RUNNING) {
            processActions();

            int count = _selector.select();
            LOG.debug("Selector returned with {}", count);

            processKeys();
        }
    }

    private void processKeys() {
        final Set<SelectionKey> keys = _selector.selectedKeys();
        for (SelectionKey key : keys) {
            SelectorAttachment attachment = (SelectorAttachment)key.attachment();
            if (key.isValid()) {
                LOG.debug("ready ops: "+
                    (key.isAcceptable()?"A":"_")+
                    (key.isConnectable()?"C":"_")+
                    (key.isWritable()?"W":"_")+
                    (key.isReadable()?"R":"_"));
                try {
                    if (key.isReadable()) {
                        attachment.readable(key);
                    }
                    if (key.isWritable()) {
                        attachment.writable(key);
                    }
                    if (key.isConnectable()) {
                        attachment.connectable(key);
                    }
                    if (key.isAcceptable()) {
                        attachment.acceptable(key);
                    }
                } catch (IOException | CancelledKeyException ex) {
                    LOG.warn("non-fatal exception processing key", ex);
                }
            }
        }
        keys.clear();
    }

    private void processActions() {
        _lock.lock();
        try {
            LOG.debug("processing "+_actionQueue.size()+" actions");

            Runnable action;
            while ((action = _actionQueue.poll()) != null) {
                _lock.unlock();
                try {
                    // do not run actions under lock
                    action.run();
                } catch (Throwable ex) {
                    LOG.error("Caught exception running action", ex);
                } finally {
                    _lock.lock();
                }
            }
        } finally {
            _lock.unlock();
        }
    }

    public <M extends Message> PublisherLink<M> connect(
        Subscription<M> subscription, String host, int port,
        Consumer<M> messageConsumer)
        throws IOException
    {
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(false);
        TCPROSSubscriberSelectorAttachment<M> att = new TCPROSSubscriberSelectorAttachment<>(
            subscription, this, ch, messageConsumer);
        String hostRemap = _hostNameMap.remap(host);

        runOnSelectorThread(() -> {
            try {
                final int initialInterests = SelectionKey.OP_CONNECT | SelectionKey.OP_READ;
                SelectionKey key = ch.register(_selector, initialInterests, att);

                if (ch.connect(new InetSocketAddress(hostRemap, port))) {
                    att.connectable(key);
                }
            } catch (IOException e) {
                LOG.error("connect failed");
                att.close();
            }
        });

        return att;
    }

}
