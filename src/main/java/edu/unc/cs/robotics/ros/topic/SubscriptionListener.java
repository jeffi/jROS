package edu.unc.cs.robotics.ros.topic;

/**
 * Created by jeffi on 7/2/16.
 */
public interface SubscriptionListener<M> {
    void message(M msg);


}
