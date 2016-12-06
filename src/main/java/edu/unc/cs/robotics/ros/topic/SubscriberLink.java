package edu.unc.cs.robotics.ros.topic;


import edu.unc.cs.robotics.ros.msg.Message;

/**
 * Created by jeffi on 7/2/16.
 */
public interface SubscriberLink<M extends Message> {
    void enqueue(SerializedMessage<M> msg);
}
