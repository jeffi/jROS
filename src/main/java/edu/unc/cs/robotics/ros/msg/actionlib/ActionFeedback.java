package edu.unc.cs.robotics.ros.msg.actionlib;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;

public abstract class ActionFeedback<F extends Message> extends Message {
    public Header header;
    public GoalStatus status;
    public F feedback;

    @Override
    public String toString() {
        return getClass().getSimpleName()+"{header="+header+
            ", status="+status+
            ", feedback="+feedback+"}";
    }
}
