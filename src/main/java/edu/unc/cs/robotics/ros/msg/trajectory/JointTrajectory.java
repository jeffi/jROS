package edu.unc.cs.robotics.ros.msg.trajectory;

import java.util.Arrays;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;

/**
 * Created by jeffi on 6/28/16.
 */
public class JointTrajectory extends Message {
    public static final String DATATYPE = "trajectory_msgs/JointTrajectory";
    public static final String MD5SUM = "65b4f94a94d1ed67169da35a02f33d3f";
    public static final String DEFINITION = "" +
                                            "Header header\n" +
                                            "string[] joint_names\n" +
                                            "JointTrajectoryPoint[] points\n" +
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
    public JointTrajectoryPoint[] points;

    public JointTrajectory(Header header, String[] jointNames, JointTrajectoryPoint[] points) {
        this.header = header;
        this.jointNames = jointNames;
        this.points = points;
    }

    public JointTrajectory(MessageDeserializer buf) {
        header = new Header(buf);
        jointNames = deserializeStringArray(buf);
        points = deserializeMessageArray(JointTrajectoryPoint[]::new, JointTrajectoryPoint::new, buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        header.serialize(buf);
        serialize(buf, jointNames);
        serialize(buf, points);
    }

    @Override
    public String toString() {
        return "JointTrajectory{" +
               "header=" + header +
               ", jointNames=" + Arrays.toString(jointNames) +
               ", points=" + Arrays.toString(points) +
               '}';
    }
}
