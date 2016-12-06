package edu.unc.cs.robotics.ros.topic;

import edu.unc.cs.robotics.ros.msg.Message;

/**
 * Created by jeffi on 7/2/16.
 */
public interface PublicationListener<M extends Message> {
    void connect(SubscriberLink<? extends M> link);
    void disconnect(SubscriberLink<? extends M> link);
}
