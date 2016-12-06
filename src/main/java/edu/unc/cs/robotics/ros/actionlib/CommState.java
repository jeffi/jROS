package edu.unc.cs.robotics.ros.actionlib;

/**
 * Created by jeffi on 7/6/16.
 */
public enum CommState {
    WAITING_FOR_GOAL_ACK,
    PENDING,
    ACTIVE,
    WAITING_FOR_RESULT,
    WAITING_FOR_CANCEL_ACK,
    RECALLING,
    PREEMPTING,
    DONE,
    ;
}
