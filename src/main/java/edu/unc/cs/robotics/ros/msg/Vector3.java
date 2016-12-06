package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 3/15/16.
 */
public class Vector3 extends Message {
    public double x;
    public double y;
    public double z;

    public Vector3(MessageDeserializer buf) {
        this.x = buf.getDouble();
        this.y = buf.getDouble();
        this.z = buf.getDouble();
    }

    @Override
    public void serialize(MessageSerializer buf) {
        buf.putDouble(x);
        buf.putDouble(y);
        buf.putDouble(z);
    }

    @Override
    public String toString() {
        return "Vector3{" +
               "x=" + x +
               ", y=" + y +
               ", z=" + z +
               '}';
    }
}
