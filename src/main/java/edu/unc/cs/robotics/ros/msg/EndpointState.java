package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 3/15/16.
 */
public class EndpointState extends Message {
    public static final String DATATYPE = "baxter_core_msgs/EndpointState";
    public static final String MD5SUM = "44bea01d596ff699fa1447bec34167ac";
    public static final String DEFINITION = "" +
                                            "Header header\\n\\\n" +
                                            "geometry_msgs/Pose   pose\n" +
                                            "geometry_msgs/Twist  twist\n" +
                                            "geometry_msgs/Wrench wrench\n" +
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
                                            "MSG: geometry_msgs/Pose\n" +
                                            "# A representation of pose in free space, composed of postion and orientation. \n" +
                                            "Point position\n" +
                                            "Quaternion orientation\n" +
                                            "\n" +
                                            "================================================================================\n" +
                                            "MSG: geometry_msgs/Point\n" +
                                            "# This contains the position of a point in free space\n" +
                                            "float64 x\n" +
                                            "float64 y\n" +
                                            "float64 z\n" +
                                            "\n" +
                                            "================================================================================\n" +
                                            "MSG: geometry_msgs/Quaternion\n" +
                                            "# This represents an orientation in free space in quaternion form.\n" +
                                            "\n" +
                                            "float64 x\n" +
                                            "float64 y\n" +
                                            "float64 z\n" +
                                            "float64 w\n" +
                                            "\n" +
                                            "================================================================================\n" +
                                            "MSG: geometry_msgs/Twist\n" +
                                            "# This expresses velocity in free space broken into its linear and angular parts.\n" +
                                            "Vector3  linear\n" +
                                            "Vector3  angular\n" +
                                            "\n" +
                                            "================================================================================\n" +
                                            "MSG: geometry_msgs/Vector3\n" +
                                            "# This represents a vector in free space. \n" +
                                            "\n" +
                                            "float64 x\n" +
                                            "float64 y\n" +
                                            "float64 z\n" +
                                            "================================================================================\n" +
                                            "MSG: geometry_msgs/Wrench\n" +
                                            "# This represents force in free space, separated into\n" +
                                            "# its linear and angular parts.\n" +
                                            "Vector3  force\n" +
                                            "Vector3  torque\n" +
                                            "";

    public Header header;

    public Pose pose;
    public Twist twist;
    public Wrench wrench;

    public EndpointState(MessageDeserializer buf) {
        this.header = new Header(buf);
        this.pose = new Pose(buf);
        this.twist = new Twist(buf);
        this.wrench = new Wrench(buf);
    }

    @Override
    public void serialize(MessageSerializer ser) {
        header.serialize(ser);
        pose.serialize(ser);
        twist.serialize(ser);
        wrench.serialize(ser);
    }
}
