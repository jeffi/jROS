package edu.unc.cs.robotics.ros.topic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

import edu.unc.cs.robotics.ros.Name;
import edu.unc.cs.robotics.ros.Publisher;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Publication<M extends Message> {
    private static final Logger LOG = LoggerFactory.getLogger(Publication.class);

    private final TopicManager _topicManager;
    private final MetaMessage<M> _meta;
    private final Name _topic;
    private final int _queueSize;
    private final boolean _latching;

    private int _seqNo;
    private SerializedMessage<M> _latchedMessage;

    private final List<PublisherImpl> _publishers = new CopyOnWriteArrayList<>();
    private final List<SubscriberLink<M>> _subscriberLinks = new ArrayList<>();

    @Singleton
    static class Factory {

        <M extends Message> Publication<M> create(
            TopicManager topicManager,
            MetaMessage<M> meta, Name topic, int queueSize, boolean latching)
        {
            return new Publication<>(
                topicManager,
                meta,
                topic,
                queueSize,
                latching
            );
        }
    }

    private Publication(TopicManager topicManager, MetaMessage<M> meta, Name topic,
                        int queueSize, boolean latching)
    {
        _topicManager = topicManager;
        _meta = meta;
        _topic = topic;
        _queueSize = queueSize;
        _latching = latching;
    }

    public boolean isLatch() {
        return _latching;
    }

    public Name getTopic() {
        return _topic;
    }

    public int getMaxQueue() {
        return _queueSize;
    }

    public MetaMessage<M> getMeta() {
        return _meta;
    }

    public boolean isDropped() {
        // TODO
        return false;
    }

    void register(int retry) {
        if (retry > 0 && _topicManager.isStopped()) {
            return;
        }

        _topicManager.masterCall("registerPublisher",
            _topicManager.getCallerId(),
            this.getTopic().toString(),
            this.getMeta().getDataType(),
            _topicManager.getVassalUri())
            .onSuccess(this::registerSuccess)
            .onFault(this::registerFault)
            .onError((ex) -> {
                LOG.warn(
                    String.format("register publisher for topic %s failed with exception (retry %d)",
                    this.getTopic(), retry),
                    ex);
                if (retry < TopicManager.MAX_RETRIES) {
                    this.register(retry+1);
                }
            })
            // retries are delayed
            // 0 = immediate
            // 1 = 5 seconds
            // 2 = 2*2*5 = 20 seconds
            // 3 = 3*3*5 = 80 seconds
            .invokeLater(retry * retry * 5, TimeUnit.SECONDS);
    }

    private void registerSuccess(Object result) {
        LOG.debug("successfully registered publication for {} registered with master", getTopic());
    }

    private void registerFault(int code, String status) {
        LOG.warn("register publisher {} failed with code ({}): {}",
            this.getTopic(), code, status);
    }

    public void close() {
        // TODO: check if there is a registration in process
        // if so, handle appropriately (e.g., allow the registration to complete,
        // then call the unregister)
        unregister(0);
    }

    /**
     * Unregisters this publication with the master.
     *
     * @param retry which retry attempt this is
     */
    private void unregister(int retry) {
        if (retry > 0 && _topicManager.isStopped()) {
            return;
        }

        _topicManager.masterCall("unregisterPublisher",
            _topicManager.getCallerId(),
            this.getTopic().toString(),
            _topicManager.getVassalUri())
            .onSuccess(this::unregisterSuccess)
            .onFault(this::unregisterFault)
            .onError((ex) -> {
                // If there is an error, it means that the XML-RPC call
                // could not be completed due to a network or I/O error
                // or similar error.  The master likely did not get the
                // call, so we retry.
                LOG.warn(
                    String.format("unregister publication for %s failed with exception (retry %d)",
                    this.getTopic(), retry),
                    ex);
                if (retry < TopicManager.MAX_RETRIES) {
                    unregister(retry + 1);
                }
            })
            .invokeLater(retry*retry*5, TimeUnit.SECONDS);
    }

    /**
     * Callback for master unregisterPublisher call.
     *
     * @param result the result (ignored)
     */
    private void unregisterSuccess(Object result) {
        LOG.debug("unregister publisher for {}, success", this.getTopic());
    }

    /**
     * Callback for master unregisterPublisher call when there is a fault.
     * When there is a fault, the master let us know that it got the message,
     * but could not unregister the publisher.  This means that the another
     * unregisterPublisher call would also fault, so we do not retry.
     *
     * @param code the fault code
     * @param status the fault status message
     */
    private void unregisterFault(int code, String status) {
        LOG.warn("unregister publication for {} failed ({}): {}",
            this.getTopic(), code, status);
    }

    void addSubscriberLink(SubscriberLink<M> link) {
        synchronized (_subscriberLinks) {
            _subscriberLinks.add(link);
            if (_latchedMessage != null) {
                link.enqueue(_latchedMessage);
            }
        }

        // Note: we do not need to lock the listeners since this is a COW list.
        // this must happen last in case the listener calls publish()
        for (PublisherImpl publisher : _publishers) {
            publisher.connect(link);
        }
    }

    public void removeSubscriberLink(SubscriberLink<M> link) {
        synchronized (_subscriberLinks) {
            if (!_subscriberLinks.remove(link)) {
                throw new IllegalStateException(
                    "attempt to remove link that was not added");
            }
        }

        for (PublisherImpl publisher : _publishers) {
            publisher.disconnect(link);
        }
    }

    private void publish(M message) {
        synchronized (_subscriberLinks) {
            SerializedMessage<M> sm = new SerializedMessage<>(++_seqNo, message);
            for (SubscriberLink<M> link : _subscriberLinks) {
                link.enqueue(sm);
            }
            if (_latching) {
                _latchedMessage = sm;
            }
        }
    }

    int publisherCount() {
        return _publishers.size();
    }

    Publisher<M> newPublisher(Executor executor, PublicationListener<? super M> listener) {
        PublisherImpl publisher = new PublisherImpl(executor, listener);

        synchronized (_publishers) {
            for (SubscriberLink<M> link : _subscriberLinks) {
                publisher.connect(link);
            }

            _publishers.add(publisher);
        }

        return publisher;
    }

    private void removePublisher(PublisherImpl publisher) {
        boolean unadvertise;

        synchronized (_publishers) {
            _publishers.remove(publisher);
            unadvertise = _publishers.isEmpty();
        }

        // do not call unadvertise under the synchronized block above
        // that would cause the locking order:
        //   this._publishers -> topicManager._publicationMap
        // which is the opposite order in which locking is peformed
        // when adding a newListener, and thus could cause a
        // deadlock.
        //
        // instead this means that there is a possible window in which
        // there are no listeners and another listener will come along
        // and thus not unregister with the server
        if (unadvertise) {
            _topicManager.checkAndUnadvertise(this);
        }
    }

    private class PublisherImpl implements Publisher<M> {
        private final Executor _executor;
        private final PublicationListener<? super M> _listener;
        private boolean _closed;

        private PublisherImpl(Executor executor, PublicationListener<? super M> listener) {
            _executor = executor;
            _listener = listener;
        }

        @Override
        public synchronized void publish(M msg) {
            if (_closed) {
                throw new IllegalStateException("closed");
            }
            Publication.this.publish(msg);
        }

        @Override
        public synchronized void close() {
            if (!_closed) {
                _closed = true;
                removePublisher(this);
            }
        }

        void connect(SubscriberLink<M> link) {
            if (_listener != null) {
                _executor.execute(() -> _listener.connect(link));
            }
        }

        void disconnect(SubscriberLink<M> link) {
            if (_listener != null) {
                _executor.execute(() -> _listener.disconnect(link));
            }
        }
    }
}
