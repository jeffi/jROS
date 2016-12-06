package edu.unc.cs.robotics.ros.msg.control;

import java.util.Arrays;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;
import edu.unc.cs.robotics.ros.msg.trajectory.JointTrajectoryPoint;

/**
 * Created by jeffi on 6/28/16.
 */
public class FollowJointTrajectoryFeedback extends Message {
    public static final String DATATYPE = "control_msgs/FollowJointTrajectoryFeedback";
    public static final String MD5SUM = "10817c60c2486ef6b33e97dcd87f4474";
    public static final String DESCRIPTION = "" +
                                             "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n" +
                                             "Header header\n" +
                                             "string[] joint_names\n" +
                                             "trajectory_msgs/JointTrajectoryPoint desired\n" +
                                             "trajectory_msgs/JointTrajectoryPoint actual\n" +
                                             "trajectory_msgs/JointTrajectoryPoint error\n" +
                                             "\n" +
                                             "\n" +
                                             "================================================================================\n" +
                                             "MSG: std_msgs/Header\n" +
                                             "# Standard metadata for higher-level stamped data types.\n" +
                                             "# This is generally used to communicate timestamped data \n" +
                                             "# in a particular coordinate frame.\n" +
                                             "# \n" +
                                             "# sequence ID: consecutively increasing ID \n" +
                                             "uint32 seq\n" +
                                             "#Two-integer timestamp that is expressed as:\n" +
                                             "# * stamp.sec: seconds (stamp_secs) since epoch (in Python the variable is called 'secs')\n" +
                                             "# * stamp.nsec: nanoseconds since stamp_secs (in Python the variable is called 'nsecs')\n" +
                                             "# time-handling sugar is provided by the client library\n" +
                                             "time stamp\n" +
                                             "#Frame this data is associated with\n" +
                                             "# 0: no frame\n" +
                                             "# 1: global frame\n" +
                                             "string frame_id\n" +
                                             "\n" +
                                             "================================================================================\n" +
                                             "MSG: trajectory_msgs/JointTrajectoryPoint\n" +
                                             "# Each trajectory point specifies either positions[, velocities[, accelerations]]\n" +
                                             "# or positions[, effort] for the trajectory to be executed.\n" +
                                             "# All specified values are in the same order as the joint names in JointTrajectory.msg\n" +
                                             "\n" +
                                             "float64[] positions\n" +
                                             "float64[] velocities\n" +
                                             "float64[] accelerations\n" +
                                             "float64[] effort\n" +
                                             "duration time_from_start\n";

    public Header header;
    public String[] jointNames;
    public JointTrajectoryPoint desired;
    public JointTrajectoryPoint actual;
    public JointTrajectoryPoint error;

    public FollowJointTrajectoryFeedback() {

    }

    public FollowJointTrajectoryFeedback(MessageDeserializer buf) {
        header = new Header(buf);
        jointNames = deserializeStringArray(buf);
        desired = new JointTrajectoryPoint(buf);
        actual = new JointTrajectoryPoint(buf);
        error = new JointTrajectoryPoint(buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        header.serialize(buf);
        serialize(buf, jointNames);
        desired.serialize(buf);
        actual.serialize(buf);
        error.serialize(buf);
    }

    @Override
    public String toString() {
        return "FollowJointTrajectoryFeedback{" +
               "header=" + header +
               ", jointNames=" + Arrays.toString(jointNames) +
               ", desired=" + desired +
               ", actual=" + actual +
               ", error=" + error +
               '}';
    }
}
