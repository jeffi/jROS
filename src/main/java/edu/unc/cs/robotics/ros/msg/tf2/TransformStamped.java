package edu.unc.cs.robotics.ros.msg.tf2;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;

/**
 * Created by jonathanlynn on 10/5/16.
 */
public class TransformStamped extends Message {

    public static final String DATATYPE = "geometry_msgs/TransformStamped";
    public static final String MD5SUM = "b5764a33bfeb3588febc2682852579b0";
    public static final String DEFINITION = "# This expresses a transform from coordinate frame header.frame_id\n" +
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
                                            "================================================================================\n" +
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
                                            "================================================================================" +
                                            "MSG: geometry_msgs/Quaternion\n" +
                                            "# This represents an orientation in free space in quaternion form.\n" +
                                            "\n" +
                                            "float64 x\n" +
                                            "float64 y\n" +
                                            "float64 z\n" +
                                            "float64 w\n";

    public Header header;
    public String child_frame_id;
    public Transform transform;

    public TransformStamped(MessageDeserializer buf){
        this.header = new Header(buf);
        this.child_frame_id = buf.getString();
        this.transform = new Transform(buf);
    }

    @Override
    public void serialize(MessageSerializer ser) {
        header.serialize(ser);
        ser.putString(child_frame_id);
        transform.serialize(ser);
    }

    @Override
    public String toString() {
        return "TransformStamped{" +
               "header=" + header +
               ", child_frame_id='" + child_frame_id + '\'' +
               ", transform=" + transform +
               '}';
    }
}
