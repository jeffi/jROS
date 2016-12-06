package edu.unc.cs.robotics.ros.actionlib;

import edu.unc.cs.robotics.ros.msg.Message;

public interface GoalFeedbackListener<
    Goal extends Message,
    Result extends Message,
    Feedback extends Message>
{
    void feedback(GoalHandle<Goal,Result> goalHandle, Feedback feedback);
}
