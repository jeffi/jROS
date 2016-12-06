package edu.unc.cs.robotics.ros.actionlib;

import java.io.Closeable;

import edu.unc.cs.robotics.ros.msg.Message;

public interface GoalHandle<Goal extends Message, Result extends Message> extends Closeable {
    Goal getGoal();
    boolean isExpired();
    CommState getCommState();
    TerminalState getTerminalState();
    Result getResult();

    void resend();
    void cancel();

    void close(); // equivalent to roscpp's reset() or going out of scope
}
