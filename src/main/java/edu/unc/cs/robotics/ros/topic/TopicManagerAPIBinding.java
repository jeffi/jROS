package edu.unc.cs.robotics.ros.topic;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.cs.robotics.ros.xmlrpc.XmlrpcMethodBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the XML-RPC API binding to the TopicManager.
 * It implements the poorly named "Slave" API.
 */
public class TopicManagerAPIBinding {
    // TODO: make this package private if possible
    // Currently XmlrpcBinding requires the class to be public in order to access the methods
    // of it.

    private static final Logger LOG = LoggerFactory.getLogger(TopicManagerAPIBinding.class);

    private final TopicManager _topicManager;

    TopicManagerAPIBinding(TopicManager topicManager) {
        _topicManager = topicManager;
    }

    @XmlrpcMethodBinding
    public Object[] getBusStats(String callerId) {
        LOG.warn("call to stub getBusStats");
        return new Object[] {
            0,
            "OK",
            new Object[0] // TODO
        };
    }

    @XmlrpcMethodBinding
    public Object[] getBusInfo(String callerId) {
        LOG.warn("call to stub getBusInfo");
        return new Object[] {
            0,
            "OK",
            new Object[0] // TODO
        };
    }

    @XmlrpcMethodBinding
    public Object[] getMasterUri(String callerId) {
        LOG.warn("call to stub getMasterUri");
        return new Object[] {
            0,
            "OK",
            "master uri" // TODO
        };
    }

    @XmlrpcMethodBinding
    public Object[] shutdown(String callerId, String msg) {
        LOG.warn("call to shutdown");
        return new Object[] {
            -1,
            "denied",
            "ignore"
        };
    }

    @XmlrpcMethodBinding
    public Object[] getPid(String callerId) {
        LOG.warn("call to stub getPid");
        return new Object[] {
            0,
            "OK",
            123456789
        };
    }

    @XmlrpcMethodBinding
    public Object[] getSubscriptions(String callerId) {
        LOG.warn("call to stub getSubscriptions");
        return new Object[] {
            0,
            "OK",
            new Object[0]
        };
    }

    @XmlrpcMethodBinding
    public Object[] getPublications(String callerId) {
        LOG.warn("call to stub getPublications");
        return new Object[] {
            0,
            "OK",
            new Object[0]
        };
    }

    @XmlrpcMethodBinding
    public Object[] paramUpdate(String callerId, String key, Object value) {
        LOG.warn("call to stub paramUpdate");
        return new Object[] {
            0,
            "OK",
            "ignore"
        };
    }

    @XmlrpcMethodBinding
    public Object[] publisherUpdate(String callerId, String topic, Object[] publishers) {

        List<URI> pubUris = Arrays.stream(publishers)
            .map(pub -> (String)pub)
            .map(URI::create)
            .collect(Collectors.toList());


        LOG.info("publisherUpdate: "+callerId+", "+topic+", "+pubUris);
//        new ArrayList<>(publishers.length);
//        for (Object publisher : publishers) {
//            pubs.add((String)publisher);
//        }
        if (_topicManager.pubUpdate(topic, pubUris)) {
            return new Object[] { 1, "", 0 };
        } else {
            return new Object[] { 0, "error", 0 };
        }
    }

    @XmlrpcMethodBinding
    public Object[] requestTopic(String callerId, String topic, Object[] protocols) {
        LOG.info("requestTopic "+callerId+", "+topic);
        Object[] protocolParameters = _topicManager.requestTopic(topic, protocols);
        if (protocolParameters != null) {
            return new Object[] { 1, "", protocolParameters };
        } else {
            return new Object[] { 0, "error", 0 };
        }
    }
}
