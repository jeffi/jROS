package edu.unc.cs.robotics.ros.msg.tf2;


import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;
import edu.unc.cs.robotics.ros.msg.Quaternion;
import edu.unc.cs.robotics.ros.msg.Vector3;

/**
 * Created by jonathanlynn on 10/5/16.
 */
public class Transform extends Message {

    public static final String DATATYPE = "geometry_msgs/Transform";
    public static final String MD5SUM = "ac9eff44abf714214112b05d54a3cf9b";
    public static final String DEFINITION = "" +
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

    public Vector3 translation;
    public Quaternion rotation;

    public Transform(MessageDeserializer buf){
        this.translation = new Vector3(buf);
        this.rotation = new Quaternion(buf);
    }

    @Override
    public void serialize(MessageSerializer ser) {
        translation.serialize(ser);
        rotation.serialize(ser);
    }

    @Override
    public String toString() {
        return "Transform{" +
               "translation=" + translation +
               ", rotation=" + rotation +
               '}';
    }
}
