package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 3/15/16.
 */
public class Pose extends Message {
    public Point position;
    public Quaternion orientation;

    public Pose(MessageDeserializer buf) {
        this.position = new Point(buf);
        this.orientation = new Quaternion(buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        position.serialize(buf);
        orientation.serialize(buf);
    }

    @Override
    public String toString() {
        return "Pose{" +
            "position=" + position +
            ", orientation=" + orientation +
            '}';
    }
}
