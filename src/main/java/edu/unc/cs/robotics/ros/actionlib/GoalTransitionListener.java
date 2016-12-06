package edu.unc.cs.robotics.ros.actionlib;

import edu.unc.cs.robotics.ros.msg.Message;

/**
 * Created by jeffi on 7/7/16.
 */
public interface GoalTransitionListener<Goal extends Message, Result extends Message> {
    void transition(GoalHandle<Goal,Result> goalHandle, CommState state);
}
