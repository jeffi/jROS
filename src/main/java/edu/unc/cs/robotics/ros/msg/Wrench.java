package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 3/15/16.
 */
public class Wrench extends Message {
    public Vector3 force;
    public Vector3 torque;

    public Wrench(MessageDeserializer buf) {
        this.force = new Vector3(buf);
        this.torque = new Vector3(buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        force.serialize(buf);
        torque.serialize(buf);
    }
}
