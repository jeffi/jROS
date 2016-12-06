package edu.unc.cs.robotics.ros.msg.control;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;

/**
 * Created by jeffi on 6/28/16.
 */
public class JointTolerance extends Message {
    public static final String DATATYPE = "control_msgs/JointTolerance";
    public static final String MD5SUM = "f544fe9c16cf04547e135dd6063ff5be";
    public static final String DESCRIPTION = "" +
                                             "# The tolerances specify the amount the position, velocity, and\n" +
                                             "# accelerations can vary from the setpoints.  For example, in the case\n" +
                                             "# of trajectory control, when the actual position varies beyond\n" +
                                             "# (desired position + position tolerance), the trajectory goal may\n" +
                                             "# abort.\n" +
                                             "# \n" +
                                             "# There are two special values for tolerances:\n" +
                                             "#  * 0 - The tolerance is unspecified and will remain at whatever the default is\n" +
                                             "#  * -1 - The tolerance is \\\"erased\\\".  If there was a default, the joint will be\n" +
                                             "#         allowed to move without restriction.\n" +
                                             "\n" +
                                             "string name\n" +
                                             "float64 position  # in radians or meters (for a revolute or prismatic joint, respectively)\n" +
                                             "float64 velocity  # in rad/sec or m/sec\n" +
                                             "float64 acceleration  # in rad/sec^2 or m/sec^2\n";

    public String name;
    public double position;
    public double velocity;
    public double acceleration;

    public JointTolerance() {

    }

    public JointTolerance(MessageDeserializer buf) {
        name = buf.getString();
        position = buf.getDouble();
        velocity = buf.getDouble();
        acceleration = buf.getDouble();
    }

    @Override
    public void serialize(MessageSerializer buf) {
        buf.putString(name);
        buf.putDouble(position);
        buf.putDouble(velocity);
        buf.putDouble(acceleration);
    }

    @Override
    public String toString() {
        return "JointTolerance{" +
            "name='" + name + '\'' +
            ", position=" + position +
            ", velocity=" + velocity +
            ", acceleration=" + acceleration +
            '}';
    }
}
