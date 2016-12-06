package edu.unc.cs.robotics.ros;


import java.io.IOException;
import javax.inject.Singleton;

import edu.unc.cs.robotics.ros.xml.MethodCall;
import spark.Request;
import spark.Response;
import spark.Spark;

@Singleton
public class ROSMasterService {

    private Object registerService(Request request, Response response) throws Exception {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String service = (String)methodCall.getParam(1);
        String serviceApi = (String)methodCall.getParam(2);
        String callerApi = (String)methodCall.getParam(3);

        return null;
    }

    private Object unregisterService(Request request, Response response) throws Exception {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String service = (String)methodCall.getParam(1);
        String serviceApi = (String)methodCall.getParam(2);

        return null;
    }

    private Object registerSubscriber(Request request, Response response) throws Exception {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String topic = (String)methodCall.getParam(1);
        String topicType = (String)methodCall.getParam(2);
        String callerApi = (String)methodCall.getParam(3);

        return null;
    }

    private Object unregisterSubscriber(Request request, Response response) throws Exception {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String topic = (String)methodCall.getParam(1);
        String callerApi = (String)methodCall.getParam(2);

        return null;
    }

    private Object registerPublisher(Request request, Response response) throws Exception {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String topic = (String)methodCall.getParam(1);
        String topicType = (String)methodCall.getParam(2);
        String callerApi = (String)methodCall.getParam(3);

        return null;
    }

    private Object unregisterPublisher(Request request, Response response) throws Exception {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String topic = (String)methodCall.getParam(1);
        String callerApi = (String)methodCall.getParam(2);

        return null;
    }

    private Object lookupNode(Request request, Response response) throws IOException {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String node = (String)methodCall.getParam(1);

        return null;
    }

    private Object getPublishedTopics(Request request, Response response) throws IOException {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String subgraph = (String)methodCall.getParam(1);

        return null;
    }

    private Object getTopicTypes(Request request, Response response) throws IOException {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);

        return null;
    }

    private Object getSystemState(Request request, Response response) throws IOException {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);

        return null;
    }

    private Object getUri(Request request, Response response) throws IOException {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);

        return null;
    }

    private Object lookupService(Request request, Response response) throws IOException {
        MethodCall methodCall = MethodCall.parse(request.raw().getInputStream());
        String callerId = (String)methodCall.getParam(0);
        String service = (String)methodCall.getParam(1);

        return null;
    }

    public void setupRoutes() {
        Spark.post("/registerService", this::registerService);
        Spark.post("/unregisterService", this::unregisterService);
        Spark.post("/registerSubscriber", this::registerSubscriber);
        Spark.post("/unregisterSubscriber", this::unregisterSubscriber);
        Spark.post("/registerPublisher", this::registerPublisher);
        Spark.post("/unregisterPublisher", this::unregisterPublisher);
        Spark.post("/lookupNode", this::lookupNode);
        Spark.post("/getPublishedTopics", this::getPublishedTopics);
        Spark.post("/getTopicTypes", this::getTopicTypes);
        Spark.post("/getSystemState", this::getSystemState);
        Spark.post("/getUri", this::getUri);
        Spark.post("/lookupService", this::lookupService);
    }
}
