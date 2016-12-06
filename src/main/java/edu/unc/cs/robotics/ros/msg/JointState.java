package edu.unc.cs.robotics.ros.msg;

import java.util.Arrays;

/**
 * Created by jeffi on 3/11/16.
 */
@MessageSpec(
    type = JointState.DATATYPE,
    md5sum = JointState.MD5SUM,
    definition = JointState.DEFINITION
)
public class JointState extends Message {

    public static final String DATATYPE = "sensor_msgs/JointState";
    public static final String MD5SUM = "3066dcd76a6cfaef579bd0f34173e9fd";
    public static final String DEFINITION = "" +
                                            "# This is a message that holds data to describe the state of a set of torque controlled joints. \n" +
                                            "#\n" +
                                            "# The state of each joint (revolute or prismatic) is defined by:\n" +
                                            "#  * the position of the joint (rad or m),\n" +
                                            "#  * the velocity of the joint (rad/s or m/s) and \n" +
                                            "#  * the effort that is applied in the joint (Nm or N).\n" +
                                            "#\n" +
                                            "# Each joint is uniquely identified by its name\n" +
                                            "# The header specifies the time at which the joint states were recorded. All the joint states\n" +
                                            "# in one message have to be recorded at the same time.\n" +
                                            "#\n" +
                                            "# This message consists of a multiple arrays, one for each part of the joint state. \n" +
                                            "# The goal is to make each of the fields optional. When e.g. your joints have no\n" +
                                            "# effort associated with them, you can leave the effort array empty. \n" +
                                            "#\n" +
                                            "# All arrays in this message should have the same size, or be empty.\n" +
                                            "# This is the only way to uniquely associate the joint name with the correct\n" +
                                            "# states.\n" +
                                            "\n" +
                                            "\n" +
                                            "Header header\n" +
                                            "\n" +
                                            "string[] name\n" +
                                            "float64[] position\n" +
                                            "float64[] velocity\n" +
                                            "float64[] effort\n" +
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
                                            "string frame_id\n";

    public static final MetaMessage<JointState> META = new MetaMessageImpl<>(DATATYPE, MD5SUM, DEFINITION, JointState::new);

    public Header header;

    public String[] name;
    public double[] position;
    public double[] velocity;
    public double[] effort;

    public JointState() {
    }

    public JointState(JointState orig) {
        this.header = new Header(orig.header);
        this.name = orig.name.clone();
        this.position = orig.position.clone();
        this.velocity = orig.velocity.clone();
        this.effort = orig.effort.clone();
    }

    public JointState(Header header, String[] name, double[] position, double[] velocity, double[] effort) {
        this.header = header;
        this.name = name;
        this.position = position;
        this.velocity = velocity;
        this.effort = effort;
    }

    public JointState(MessageDeserializer buf) {
        this.header = new Header(buf);
        this.name = deserializeStringArray(buf);
        this.position = deserializeDoubleArray(buf);
        this.velocity = deserializeDoubleArray(buf);
        this.effort = deserializeDoubleArray(buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        header.serialize(buf);
        serialize(buf, name);
        serialize(buf, position);
        serialize(buf, velocity);
        serialize(buf, effort);
    }


    @Override
    public String toString() {
        return "JointState{" +
               "header=" + header +
               ", name=" + Arrays.toString(name) +
               ", position=" + Arrays.toString(position) +
               ", velocity=" + Arrays.toString(velocity) +
               ", effort=" + Arrays.toString(effort) +
               '}';
    }
}
