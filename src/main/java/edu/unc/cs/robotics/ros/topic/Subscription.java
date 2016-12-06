package edu.unc.cs.robotics.ros.topic;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import edu.unc.cs.robotics.ros.Name;
import edu.unc.cs.robotics.ros.Subscriber;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.unc.cs.robotics.ros.topic.TopicManager.MAX_RETRIES;

public class Subscription<M extends Message> {
    private static final Logger LOG = LoggerFactory.getLogger(Subscription.class);

    private final MetaMessage<M> _meta;
    private final Name _topic;

    private final NetworkServer _networkServer;
    private final TopicManager _topicManager;

    private final List<PublisherLink<M>> _links = new ArrayList<>();
    private final List<SubscriberImpl> _subscribers = new CopyOnWriteArrayList<>();
    private int _queueSize;

    @Singleton
    static class Factory {
        private final Provider<NetworkServer> _networkServer;

        @Inject
        public Factory(Provider<NetworkServer> networkServer) {
            _networkServer = networkServer;
        }

        <M extends Message> Subscription<M> create(
            TopicManager topicManager, MetaMessage<M> meta, Name topic, int queueSize)
        {
            return new Subscription<>(
                _networkServer.get(),
                topicManager,
                meta,
                topic,
                queueSize);
        }
    }

    private Subscription(
        NetworkServer networkServer,
        TopicManager topicManager,
        MetaMessage<M> meta,
        Name topic,
        int queueSize)
    {
        _networkServer = networkServer;
        _topicManager = topicManager;
        _meta = meta;
        _topic = topic;
        _queueSize = queueSize;
    }

    public MetaMessage<M> getMeta() {
        return _meta;
    }

    Subscriber<M> newSubscriber(Executor executor, SubscriptionListener<? super M> listener) {
        SubscriberImpl subscriber = new SubscriberImpl(executor, listener);
        synchronized (_subscribers) {
            _subscribers.add(subscriber);
        }
        return subscriber;
    }

    int subscriberCount() {
        return _subscribers.size();
    }

    private void removeSubscriber(SubscriberImpl sub) {
        boolean unsubscribe;

        synchronized (_subscribers) {
            _subscribers.remove(sub);
            unsubscribe = _subscribers.isEmpty();
        }

        if (unsubscribe) {
            _topicManager.checkAndUnsubscribe(this);
        }
    }

    public Name getTopic() {
        return _topic;
    }

    void register(int retry) {
        if (retry > 0 && _topicManager.isStopped()) {
            return;
        }

        _topicManager.masterCall(
            "registerSubscriber",
            _topicManager.getCallerId(),
            this.getTopic().toString(),
            this.getMeta().getDataType(),
            _topicManager.getVassalUri())
            .onSuccess(this::registerSuccess)
            .onFault(this::registerFault)
            .onError((ex) -> {
                LOG.warn(
                    String.format("register subscriber for %s failed with exception (retry %d)",
                        this.getTopic(), retry),
                    ex);
                if (retry < MAX_RETRIES) {
                    this.register(retry + 1);
                }
            })
            .invokeLater(retry*retry*5, TimeUnit.SECONDS);
    }

    private void registerSuccess(Object result) {
        String vassalUri =_topicManager.getVassalUri();


        Object[] resultArray = (Object[])result;
        Object[] payloadUris = (Object[])resultArray[2];

        List<URI> pubUris = new ArrayList<>();
        for (Object pubUri : payloadUris) {
            if (!vassalUri.equals(pubUri)) {
                pubUris.add(URI.create((String)pubUri));
            }
        }

        // TODO: check for and handle local publisher
        // TODO: this includes adding a IPC local connection

        this.pubUpdate(pubUris);
    }

    private void registerFault(int code, String message) {
        LOG.warn("register subscriber for {} failed with fault ({}): {}",
            this.getTopic(), code, message);
    }

    private void unregister(int retry) {
        if (retry > 0 && _topicManager.isStopped()) {
            return;
        }

        _topicManager.masterCall(
            "unregisterSubscriber",
            _topicManager.getCallerId(),
            getTopic().toString(),
            _topicManager.getVassalUri())
            .onSuccess(this::unregisterSuccess)
            .onFault(this::unregisterFault)
            .onError((ex) -> {
                LOG.warn("unregister subscriber for "+getTopic()+" failed with exception (retry "+retry+")", ex);
                if (retry < MAX_RETRIES) {
                    this.unregister(retry + 1);
                }
            })
            .invokeLater(retry*retry*5, TimeUnit.SECONDS);
    }

    private void unregisterSuccess(Object result) {
        LOG.info("unregister of "+getTopic()+" successful");
    }

    private void unregisterFault(int code, String status) {
        LOG.warn("unregister failed ("+code+"): "+status);
    }

    void pubUpdate(List<URI> pubUris) {
        LOG.debug("pubUpdate: {}", pubUris);

        for (URI pubUri : pubUris) {
            // TODO: additions/subtractions
        }

        List<URI> additions = pubUris;

        for (URI addition : additions) {
            // TODO: avoid connections to self
            requestTopic(addition, 0);
        }
    }

    private void requestTopic(URI remoteUri, int retry) {
        Object[][] protos = {
            { "TCPROS" }
        };

        _topicManager.prepareCall(remoteUri, "requestTopic",
            _topicManager.getCallerId(),
            _topic.toString(),
            protos)
            .onSuccess(this::requestTopicSuccess)
            .onFault(this::requestTopicFault)
            .onError((ex) -> {
                LOG.error("requestTopic (retry "+retry+")", ex);
                if (retry < MAX_RETRIES) {
                    requestTopic(remoteUri, retry+1);
                }
            })
            .invokeLater(retry*retry*5, TimeUnit.SECONDS);
    }

    private void requestTopicSuccess(Object result) {
        if (!(result instanceof Object[])) {
            LOG.warn("invalid response, not an array");
            return;
        }
        Object[] resultTuple = (Object[])result;

        if (resultTuple.length != 3 || !(resultTuple[2] instanceof Object[])) {
            LOG.warn("invalid response, invalid response tuple");
            return;
        }
        Object[] protoList = (Object[])resultTuple[2];

        if (protoList.length == 0) {
            LOG.debug("negotiation resulted in empty protocol list");
            return;
        }

        if ("TCPROS".equals(protoList[0])) {
            connectTCPROS(protoList);
        } else {
            LOG.error("negotiated unsupported protocol: "+protoList[0]);
        }
    }

    private void requestTopicFault(int code, String status) {
        LOG.error("requestTopic failed with fault ("+code+"): "+status);
    }

    private void connectTCPROS(Object[] proto) {
        if (proto.length != 3 ||
            !(proto[1] instanceof String) ||
            !(proto[2] instanceof Integer))
        {
            LOG.warn("invalid parameters for TCPROS");
            return;
        }

        LOG.debug("Connecting via TCPROS to {}:{}", proto[1], proto[2]);

        String host = (String)proto[1];
        int port = (Integer)proto[2];

        try {
            PublisherLink<M> link = _networkServer.connect(
                this, host, port, this::messageRecv);
            synchronized (_links) {
                _links.add(link);
            }
        } catch (IOException e) {
            LOG.error("unable to initiate TCPROS connection to "+host+":"+port, e);
        }
    }

    /**
     * This is a callback from the PublisherLink
     *
     * @param msg the message received
     */
    private void messageRecv(M msg) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("messageRecv = {}", msg);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("messageRecv = {}", msg.getClass().getName());
        }

        synchronized (_subscribers) {
            for (SubscriberImpl subscriber : _subscribers) {
                subscriber.message(msg);
            }
        }
    }


    void close() {
        unregister(0);

        synchronized (_links) {
            for (PublisherLink<M> link : _links) {
                link.close();
            }
        }
    }

    private class SubscriberImpl implements Subscriber<M> {
        private final Executor _executor;
        private final SubscriptionListener<? super M> _listener;
        boolean _closed;

        SubscriberImpl(Executor executor, SubscriptionListener<? super M> listener) {
            _executor = executor;
            _listener = listener;
        }

        private void message(M msg) {
            _executor.execute(() -> _listener.message(msg));
        }

        @Override
        public synchronized void close() {
            if (!_closed) {
                _closed = true;
                removeSubscriber(this);
            }
        }
    }
}
