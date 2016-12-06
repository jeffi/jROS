package edu.unc.cs.robotics.ros;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

import edu.unc.cs.robotics.ros.xmlrpc.XmlrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ROSMasterClient {
    private static final Logger LOG = LoggerFactory.getLogger(ROSMasterClient.class);

    private static final int MAX_RETRIES = 3;

    private final XmlrpcClient _client;
    private final Names _names;
    private final URI _masterUri;

    @Inject
    ROSMasterClient(XmlrpcClient client, Names names, @ROSMaster URI uri) {
        _client = client;
        _names = names;
        _masterUri = uri;
    }

    public void getPublishedTopics(
        String subgraph,
        Consumer<List<ROSTopicInfo>> result,
        XmlrpcClient.FaultConsumer fault)
    {
        getPublishedTopics(subgraph, result, fault, 0);
    }

    private void getPublishedTopics(
        String subgraph,
        Consumer<List<ROSTopicInfo>> success,
        XmlrpcClient.FaultConsumer fault,
        int retry)
    {
        _client.prepare(_masterUri, "getPublishedTopics", _names.getName(), subgraph)
            .onError((ex) -> {
                LOG.warn("getPublishedTopics failed with exception", ex);
                if (retry < MAX_RETRIES) {
                    getPublishedTopics(subgraph, success, fault, retry+1);
                }
            })
            .onSuccess((result) -> {
                Object[] resultTuple = (Object[])result;
                // resultTuple[0] == 1
                // resultTuple[1] == "current topics"
                // resultTuple[2] == [ [topic1, type1]...[topicN, typeN] ]
                success.accept(Arrays.stream((Object[])resultTuple[2]).map((topic) -> {
                    Object[] pair = (Object[])topic;
                    return new ROSTopicInfo((String)pair[0], (String)pair[1]);
                }).collect(Collectors.toList()));
            })
            .onFault(fault)
            .invokeLater(retry*retry*5, TimeUnit.SECONDS);
    }

}
