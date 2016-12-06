package edu.unc.cs.robotics.ros.msg.actionlib;


import java.util.function.Function;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;

public abstract class ActionGoal<G extends Message> extends Message {
    public Header header;
    public GoalID goalId;
    public G goal;

    public ActionGoal(
        Header header,
        GoalID goalId,
        G goal)
    {
        this.header = header;
        this.goalId = goalId;
        this.goal = goal;
    }

    protected ActionGoal(MessageDeserializer des, Function<MessageDeserializer, G> goalFn) {
        this.header = new Header(des);
        this.goalId = new GoalID(des);
        this.goal = goalFn.apply(des);
    }

    @Override
    public final void serialize(MessageSerializer ser) {
        header.serialize(ser);
        goalId.serialize(ser);
        goal.serialize(ser);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+
            "{header="+header+
            ", goalId="+goalId+
            ", goal="+goal+"}";
    }
}
