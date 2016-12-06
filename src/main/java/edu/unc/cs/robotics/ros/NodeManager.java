package edu.unc.cs.robotics.ros;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.topic.PublicationListener;
import edu.unc.cs.robotics.ros.topic.SubscriptionListener;
import edu.unc.cs.robotics.ros.topic.TopicManager;

@Singleton
public class NodeManager {
    private final Map<String, Reference<NodeHandle>> _nodeHandleMap = new HashMap<>();
    private final ReferenceQueue<NodeHandle> _refQueue = new ReferenceQueue<>();
    private final Names _names;
    private final TopicManager _topicManager;

    @Inject
    NodeManager(Names names, TopicManager topicManager) {
        _names = names;
        _topicManager = topicManager;
    }

    public NodeHandle node(Name name) {
        synchronized (_nodeHandleMap) {
            Reference<NodeHandle> ref = _nodeHandleMap.get(name.toString());
            if (ref != null) {
                NodeHandle handle = ref.get();
                if (handle != null) {
                    return handle;
                }
            }

            NodeHandleImpl nodeHandle = new NodeHandleImpl(name);
            _nodeHandleMap.put(name.toString(), new WeakReference<>(nodeHandle, _refQueue));
            return nodeHandle;
        }
    }

    private class NodeHandleImpl implements NodeHandle {
        private final Name _name;

        NodeHandleImpl(Name name) {
            _name = name;
        }

        @Override
        public Name name() {
            return _name;
        }

        @Override
        public <M extends Message> Publisher<M> advertise(
            MetaMessage<M> meta, String topic, int queueSize, boolean latch,
            Executor executor, PublicationListener<? super M> listener)
        {
            return _topicManager.advertise(
                meta, _name.resolveNS(topic), queueSize, latch, executor, listener);
        }

        @Override
        public <M extends Message> Subscriber<M> subscribe(
            MetaMessage<M> meta, String topic, int queueSize,
            Executor executor, SubscriptionListener<? super M> listener)
        {
            return _topicManager.subscribe(
                meta, _name.resolveNS(topic), queueSize, executor, listener);
        }
    }
}
