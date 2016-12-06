package edu.unc.cs.robotics.ros.topic;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.inject.Inject;

import com.google.inject.Singleton;
import edu.unc.cs.robotics.ros.Name;
import edu.unc.cs.robotics.ros.Names;
import edu.unc.cs.robotics.ros.Publisher;
import edu.unc.cs.robotics.ros.ROSMaster;
import edu.unc.cs.robotics.ros.Service;
import edu.unc.cs.robotics.ros.Subscriber;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.network.NetworkServer;
import edu.unc.cs.robotics.ros.xmlrpc.XmlrpcClient;
import edu.unc.cs.robotics.ros.xmlrpc.XmlrpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TopicManager implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(TopicManager.class);
    public static final int MAX_RETRIES = 3;

    private final Map<String, Publication<?>> _publicationMap = new HashMap<>();
    private final Map<String, Subscription<?>> _subscriptionMap = new HashMap<>();

    private final XmlrpcServer _xmlrpcServer;
    private final XmlrpcClient _xmlrpcClient;
    private final NetworkServer _networkServer;
    private final TopicManagerAPIBinding _vassalService;
    private final Names _names;
    private final URI _masterUri;

    private volatile boolean _stopped;

    private final Subscription.Factory _subscriptionFactory;
    private final Publication.Factory _publicationFactory;

    @Inject
    TopicManager(
        XmlrpcServer xmlrpcServer,
        XmlrpcClient xmlrpcClient,
        NetworkServer networkServer,
        Names names,
        @ROSMaster URI masterUri,
        Subscription.Factory subscriptionFactory, Publication.Factory publicationFactory)
    {
        _xmlrpcServer = xmlrpcServer;
        _xmlrpcClient = xmlrpcClient;
        _networkServer = networkServer;
        _vassalService = new TopicManagerAPIBinding(this);
        _names = names;
        _masterUri = masterUri;
        _subscriptionFactory = subscriptionFactory;
        _publicationFactory = publicationFactory;
    }

    String getCallerId() {
        return _names.getName();
    }

    String getVassalUri() {
        return _xmlrpcServer.getUri();
    }

    @Override
    public void start() {
        _xmlrpcServer.bind(_vassalService);
        LOG.info("started");
    }

    @Override
    public void stop() {
        _stopped = true;

        synchronized (_publicationMap) {
            for (Publication<?> pub : _publicationMap.values()) {
                pub.close();
            }
        }

        synchronized (_subscriptionMap) {
            for (Subscription<?> sub : _subscriptionMap.values()) {
                sub.close();
            }
        }

        _xmlrpcServer.unbind(_vassalService);
        LOG.info("stopped");
    }

    @SuppressWarnings("unchecked")
    private <M extends Message> Publication<M> uncheckedPublicationLookup(String topic) {
        return (Publication<M>)_publicationMap.get(topic);
    }

    /**
     * Advertises a topic and returns a publisher for it.  The topic
     * will be registered with the master upon first advertise call,
     * and will remain open until all publishers for it are closed.
     *
     * @param meta the information about the message to publish
     * @param topic the topic name
     * @param queueSize the maximum queue size for outgoing messages.
     *    The queueSize only has an effect on the first advertise
     * @param latch true of the last published message should be
     *    latched, false otherwise.  This only has an effect on the
     *    first advertise.
     * @param executor The executor with which the listener callbacks
     *    will take effect.  May only be null if the listener is null.
     * @param listener Events for connect/disconnect of subscribers
     *    will be sent to this listener.  May be null.
     * @param <M> the message type
     * @return a publisher for the message
     */
    public <M extends Message> Publisher<M> advertise(
        MetaMessage<M> meta, Name topic, int queueSize, boolean latch,
        Executor executor, PublicationListener<? super M> listener)
    {
        Publisher<M> pub;
        Publication<M> publication;

        boolean register;

        synchronized (_publicationMap) {
            publication = uncheckedPublicationLookup(topic.toString());
            register = (publication == null);

            if (register) {
                publication = _publicationFactory.create(this, meta, topic, queueSize, latch);
                _publicationMap.put(topic.toString(), publication);
            } else if (!publication.getMeta().getMd5sum().equals(meta.getMd5sum())) {
                throw new IllegalStateException(
                    "tried to advertise topic with md5sum " + meta.getMd5sum() +
                        ", but topic is already advertised with md5sum " + publication.getMeta());
            }

            pub = publication.newPublisher(executor, listener);
        }

        if (register) {
            publication.register(0);
        }

        return pub;
    }

    /**
     * Helper method for Publication and Subscription to make register/unregister
     * calls with the master API.  Importantly this method makes it so that the
     * Publication and Subscription do not have to carry an XmlrpcClient and
     * masterUri.
     *
     * @param methodName method to call on master
     * @param params params for the method
     * @return the dispatch
     */
    XmlrpcClient.Dispatch masterCall(String methodName, Object... params) {
        return _xmlrpcClient.prepare(_masterUri, methodName, params);
    }

    XmlrpcClient.Dispatch prepareCall(URI uri, String methodName, Object... params) {
        return _xmlrpcClient.prepare(uri, methodName, params);
    }

    /**
     * Called from a publication when its publisher count reaches 0.
     *
     * @param publication the publication.
     */
    void checkAndUnadvertise(Publication<?> publication) {
        synchronized (_publicationMap) {
            if (publication.publisherCount() > 0) {
                // it is possible that another thread advertised
                // between this thread closing the publication
                // and acquiring the lock here.
                return;
            }

            _publicationMap.remove(publication.getTopic().toString());
        }

        LOG.debug("Last publisher for {} closed, unregistering",
            publication.getTopic().toString());

        publication.close();
    }

    void checkAndUnsubscribe(Subscription<?> subscription) {
        synchronized (_subscriptionMap) {
            if (subscription.subscriberCount() > 0) {
                return;
            }

            _subscriptionMap.remove(subscription.getTopic().toString());
        }

        LOG.debug("Last subscriber for {} closed, unsubscribing",
            subscription.getTopic().toString());

        subscription.close();
    }

    public <M extends Message> Publication<M> addSubscriberLink(
        String topic, String md5sum, SubscriberLink<M> link)
    {
        synchronized (_publicationMap) {
            Publication<M> pub = uncheckedPublicationLookup(topic);
            if (pub == null || pub.isDropped()) {
                throw new IllegalStateException("non-existant topic: " + topic);
            }

            if (!md5sum.equals(pub.getMeta().getMd5sum()) &&
                !"*".equals(md5sum) &&
                !"*".equals(pub.getMeta().getMd5sum()))
            {
                throw new IllegalStateException("client requested topic [" + topic +
                                                "] with md5sum [" + md5sum + "], but publication has md5sum of [" +
                                                pub.getMeta().getMd5sum() + "]");
            }

            pub.addSubscriberLink(link);
            return pub;
        }
    }

    public boolean pubUpdate(String topic, List<URI> pubs) {
        LOG.debug("received update for topic {}, ({} publishers)",
            topic, pubs.size());

        Subscription<?> subscription;

        synchronized (_subscriptionMap) {
            subscription = _subscriptionMap.get(topic);
        }

        if (subscription != null) {
            subscription.pubUpdate(pubs);
        }

        return true; // TODO: return something else?
    }

    /**
     * Method exposed for XML-RPC Vassal API.
     *
     * @param topic
     * @param protocols
     * @return
     */
    public Object[] requestTopic(String topic, Object[] protocols) {
        for (Object protocol : protocols) {
            if (!(protocol instanceof Object[])) {
                throw new IllegalArgumentException();
            }

            Object[] array = (Object[])protocol;
            if (array.length == 0) {
                throw new IllegalArgumentException();
            }

            if ("TCPROS".equals(array[0])) {
                return new Object[] {
                    "TCPROS",
                    _networkServer.getHost(),
                    _networkServer.getPort()
                };
            } else {
                LOG.debug("unsupported protocol: "+array[0]);
                return null;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <M extends Message> Subscription<M> uncheckedSubscriptionLookup(Name topic) {
        return (Subscription<M>)_subscriptionMap.get(topic.toString());
    }

    public <M extends Message> Subscriber<M> subscribe(
        MetaMessage<M> meta,
        Name topic,
        int queueSize,
        Executor executor,
        SubscriptionListener<? super M> listener)
    {
        Subscriber<M> subscriber;
        Subscription<M> subscription;
        boolean register;

        synchronized (_subscriptionMap) {
            subscription = uncheckedSubscriptionLookup(topic);
            register = (subscription == null);
            if (register) {
                subscription = _subscriptionFactory.create(this, meta, topic, queueSize);
                _subscriptionMap.put(topic.toString(), subscription);
            } else if (!subscription.getMeta().getMd5sum().equals(meta.getMd5sum())) {
                throw new IllegalStateException(
                    "attempt to subscribe with mismatching md5sums");
            }

            subscriber = subscription.newSubscriber(executor, listener);
        }

        if (register) {
            subscription.register(0);
        }

        return subscriber;
    }

    public boolean isStopped() {
        return _stopped;
    }
}
