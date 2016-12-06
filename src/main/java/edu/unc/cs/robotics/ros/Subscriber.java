package edu.unc.cs.robotics.ros;

import java.io.Closeable;

import edu.unc.cs.robotics.ros.msg.Message;

public interface Subscriber<M extends Message> extends Closeable {
    void close();
}
