package edu.unc.cs.robotics.ros;

import java.util.function.Consumer;
import javax.inject.Inject;

import edu.unc.cs.robotics.ros.xmlrpc.XmlrpcClient;


public class MasterClient {
    private final String _masterUri;
    private final XmlrpcClient _xmlrpcClient;

    @Inject
    MasterClient(String masterUri, XmlrpcClient xmlrpcClient) {
        _masterUri = masterUri;
        _xmlrpcClient = xmlrpcClient;
    }

    public void registerPublisher(String callerId, String topic, String topicType, String callerUri) {

        Runnable retry = () -> registerPublisher(callerId, topic, topicType, callerUri);

        _xmlrpcClient.prepare(_masterUri, "registerPublisher", callerId, topic, topicType, callerUri)
            .onSuccess(o -> {
                if (validateResponse(o)) {
                    // TODO: equivalent of: return false;
                }
            })
            .onFault(new XmlrpcClient.FaultConsumer() {
                @Override
                public void onFault(int faultCode, String faultString) {
                    // TODO: equivalent of: return false;
                }
            })
            .onError(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    // TODO: sleep, then...
                    retry.run();
                }
            })
            .invoke();
    }

    public void unregisterPublisher(String callerId, String topic, String callerUri) {
        // TODO: as with registerPublisher
        _xmlrpcClient.prepare(_masterUri, "unregisterPublisher", callerId, topic, callerUri)
            .invoke();
    }

    private boolean validateResponse(Object o) {
        if (!(o instanceof Object[])) {
            return false;
        }
        if (((Object[])o).length != 3) {
            return false;
        }

        return true;
    }

    public void registerSubscriber(String callerId, String topic, String dataType, String callerUri) {
        _xmlrpcClient.prepare(_masterUri, "registerSubscriber", callerId, topic, dataType, callerUri)
            .invoke();
    }
}
