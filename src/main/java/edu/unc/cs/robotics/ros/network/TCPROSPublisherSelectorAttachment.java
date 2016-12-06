package edu.unc.cs.robotics.ros.network;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.topic.Publication;
import edu.unc.cs.robotics.ros.topic.SerializedMessage;
import edu.unc.cs.robotics.ros.topic.SubscriberLink;

class TCPROSPublisherSelectorAttachment<M extends Message>
    extends TCPROSSelectorAttachment
    implements SubscriberLink<M>
{
    private boolean _tcpNodelay;
    private String _topic;
    private String _service;
    private String _callerId;
    private String _md5sum;

    TCPROSPublisherSelectorAttachment(NetworkServer server, SocketChannel ch) throws ClosedChannelException {
        super(server, ch);
    }

    @Override
    public void headerRecv(String name, String value) {
        switch (name) {
        case "tcp_nodelay":
            _tcpNodelay = "1".equals(value);
            break;
        case "topic":
            _topic = value;
            break;
        case "service":
            _service = value;
            break;
        case "callerid":
            _callerId = value;
            break;
        case "md5sum":
            _md5sum = value;
            break;
        case "type":
            break;
        default:
            LOG.warn("unknown header received: [{}] = [{}]", name, value);
            break;
        }
    }

    @Override
    public void headersDone() {
        if (_tcpNodelay) {
            LOG.debug("setting TCP_NODELAY");
            try {
                _channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            } catch (IOException ex) {
                LOG.warn("failed to set TCP_NODELAY", ex);
            }
        }

        if (_topic != null) {
            addSubscriberLink();
        } else if (_service != null) {
            LOG.debug("creating service client link for service [{}]", _service);
        } else {
            LOG.warn("got connection without topic or service header");
            close();
        }
    }

    @Override
    public void messageRecv(ByteBuffer buf) {
        LOG.error("received message on publisher channel!");
    }

    private void addSubscriberLink() {
        LOG.debug("creating transport subscriber link for topic [{}]", _topic);

        if (_md5sum == null || _callerId == null) {
            sendErrorHeader("missing required elements: md5sum, topic, callerId");
            return;
        }

        Publication<M> pub;

        try {
            pub = _server.getTopicManager().addSubscriberLink(
                _topic,
                _md5sum,
                this
            );
        } catch (IllegalStateException ex) {
            LOG.warn("connection error", ex);
            sendErrorHeader(ex.getMessage());
            return;
        }

        sendHeaders(
            () -> setMaxQueueSize(pub.getMaxQueue()),
            "type", pub.getMeta().getDataType(),
            "md5sum", pub.getMeta().getMd5sum(),
            "message_definition", pub.getMeta().getMessageDefinition(),
            "callerid", _server.getNames().getName(),
            "latching", pub.isLatch() ? "1" : "0",
            "topic", _topic);
    }


    @Override
    public void enqueue(SerializedMessage<M> msg) {
        enqueueWrite(msg.buffer(), null);
    }
}
