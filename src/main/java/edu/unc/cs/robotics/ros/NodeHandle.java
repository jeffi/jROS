package edu.unc.cs.robotics.ros;

import java.util.concurrent.Executor;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.topic.PublicationListener;
import edu.unc.cs.robotics.ros.topic.SubscriptionListener;

public interface NodeHandle {
    Name name();

    <M extends Message> Publisher<M> advertise(
        MetaMessage<M> meta, String topic, int queueSize, boolean latch,
        Executor executor, PublicationListener<? super M> listener);

    default <M extends Message> Publisher<M> advertise(
        MetaMessage<M> meta, String topic, int queueSize)
    {
        return advertise(meta, topic, queueSize, false, null, null);
    }

    default <M extends Message> Publisher<M> advertise(
        Class<M> msgClass, String topic, int queueSize, boolean latch,
        Executor executor, PublicationListener<? super M> listener)
    {
        return advertise(
            MetaMessage.forClass(msgClass),
            topic, queueSize, latch,
            executor, listener);
    }

    <M extends Message> Subscriber<M> subscribe(
        MetaMessage<M> meta, String topic, int queueSize,
        Executor executor, SubscriptionListener<? super M> listener);

    default <M extends Message> Subscriber<M> subscribe(
        Class<M> msgClass, String topic, int queueSize,
        Executor executor, SubscriptionListener<? super M> listener)
    {
        return subscribe(
            MetaMessage.forClass(msgClass),
            topic, queueSize,
            executor, listener);
    }

}
