package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 3/15/16.
 */
public class Twist extends Message {
    public Vector3 linear;
    public Vector3 angular;

    public Twist(MessageDeserializer buf) {
        this.linear = new Vector3(buf);
        this.angular = new Vector3(buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        linear.serialize(buf);
        angular.serialize(buf);
    }
}
