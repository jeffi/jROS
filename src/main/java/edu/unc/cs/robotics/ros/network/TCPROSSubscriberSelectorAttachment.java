package edu.unc.cs.robotics.ros.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import edu.unc.cs.robotics.ros.msg.ByteBufferDeserializer;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.topic.PublisherLink;
import edu.unc.cs.robotics.ros.topic.Subscription;

class TCPROSSubscriberSelectorAttachment<M extends Message>
    extends TCPROSSelectorAttachment
    implements PublisherLink<M>
{
//    private static final Logger LOG = LoggerFactory.getLogger(TCPROSSubscriberSelectorAttachment.class);

    private final Subscription<M> _subscription;
    private final Consumer<M> _messageConsumer;

    // these are set upon headers recieved
    private String _publisherCallerId;
    private String _md5sum;
    private String _dataType;
    private boolean _latching;

    TCPROSSubscriberSelectorAttachment(
        Subscription<M> subscription, NetworkServer server, SocketChannel ch,
        Consumer<M> messageConsumer)
        throws ClosedChannelException
    {
        super(server, ch);
        _subscription = subscription;
        _messageConsumer = messageConsumer;
    }

    @Override
    public void headerRecv(String name, String value) {
        switch (name) {
        case "callerid":
            _publisherCallerId = value;
            break;
        case "type":
            _dataType = value;
            break;
        case "latching":
            _latching = "1".equals(value);
            break;
        case "md5sum":
            _md5sum = value;
            break;
        case "message_definition":
            break;
        case "topic":
            break;
        default:
            LOG.warn("received unknown header: [{}] = [{}]", name, value);
            break;
        }
    }

    @Override
    public void headersDone() {
        //_publisherCallerId = headers.get("callerid");
        if (null == _md5sum) {
            LOG.error("publisher didn't send required header: md5sum");
            close();
            return;
        }
        if (null == _dataType) {
            LOG.error("publisher didn't send required header: type");
            close();
            return;
        }

        String submd5 = _subscription.getMeta().getMd5sum();
        if (!("*".equals(submd5) || submd5.equals(_md5sum))) {
            LOG.error("publisher md5 does not match expected");
            close();
            return;
        }
    }

    @Override
    public void messageRecv(ByteBuffer buf) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("messageRecv " + buf.remaining() + "\n" + HexDump.hexDump(buf));
        }
        ByteBufferDeserializer deserializer = new ByteBufferDeserializer(buf);
        M msg = _subscription.getMeta().deserialize(deserializer);
        _messageConsumer.accept(msg);
    }


    @Override
    public void connectable(SelectionKey key) throws IOException {
        LOG.info("finishConnect from {}", _channel.getRemoteAddress());

        _channel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);

        // TODO: "tcp_nodelay" = "0" or "1"
        sendHeaders(this::headersSent,
            "topic", _subscription.getTopic().toString(),
            "md5sum", _subscription.getMeta().getMd5sum(),
            "callerid", _server.getNames().getName(),
            "type", _subscription.getMeta().getDataType());


        // TODO: recv header
        // check valid: callerid, md5sum, type, latching

        // Then recv 4 byte message length
        // then recv message
        // loop.
    }

    void headersSent() {

    }
}
