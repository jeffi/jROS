package edu.unc.cs.robotics.ros.msg.tf2;

import java.util.Arrays;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.msg.MetaMessageImpl;


/**
 * Created by jonathanlynn on 10/5/16.
 */
public class TFMessage extends Message {

    public static final String DATATYPE = "tf2_msgs/TFMessage";
    public static final String MD5SUM = "94810edda583a504dfda3829e70d7eec";
    public static final String DEFINITION =
            "geometry_msgs/TransformStamped[] transforms\n" +
            "\n" +
            "================================================================================\n" +
            "MSG: geometry_msgs/TransformStamped\n" +
            "# This expresses a transform from coordinate frame header.frame_id\n" +
            "# to the coordinate frame child_frame_id\n" +
            "#\n" +
            "# This message is mostly used by the \n" +
            "# <a href=\"http://www.ros.org/wiki/tf\">tf</a> package. \n" +
            "# See its documentation for more information.\n" +
            "\n" +
            "Header header\n" +
            "string child_frame_id # the frame id of the child frame\n" +
            "Transform transform\n" +
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
            "================================================================================" +
            "MSG: geometry_msgs/Transform\n" +
            "# This represents the transform between two coordinate frames in free space.\n" +
            "\n" +
            "Vector3 translation\n" +
            "Quaternion rotation\n" +
            "\n" +
            "================================================================================\n" +
            "MSG: geometry_msgs/Vector3\n" +
            "# This represents a vector in free space. \n" +
            "# It is only meant to represent a direction. Therefore, it does not\n" +
            "# make sense to apply a translation to it (e.g., when applying a \n" +
            "# generic rigid transformation to a Vector3, tf2 will only apply the\n" +
            "# rotation). If you want your data to be translatable too, use the\n" +
            "# geometry_msgs/Point message instead.\n" +
            "\n" +
            "float64 x\n" +
            "float64 y\n" +
            "float64 z\n" +
            "================================================================================\n" +
            "MSG: geometry_msgs/Quaternion\n" +
            "# This represents an orientation in free space in quaternion form.\n" +
            "\n" +
            "float64 x\n" +
            "float64 y\n" +
            "float64 z\n" +
            "float64 w\n";

    public static final MetaMessage<TFMessage> META = new MetaMessageImpl<>(DATATYPE, MD5SUM, DEFINITION, TFMessage::new);

    public TransformStamped[] transforms;

    public TFMessage(MessageDeserializer buf) {
        this.transforms = buf.getMessageArray(TransformStamped[]::new, TransformStamped::new);
    }

    @Override
    public void serialize(MessageSerializer ser) {
        ser.putMessageArray(transforms);
    }

    @Override
    public String toString() {
        return "TFMessage{" +
               "transforms=" + Arrays.toString(transforms) +
               '}';
    }
}
