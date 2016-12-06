package edu.unc.cs.robotics.ros.topic;


import java.io.Closeable;

import edu.unc.cs.robotics.ros.msg.Message;

public interface PublisherLink<M extends Message> extends Closeable {
    void close();
}
