package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 3/15/16.
 */
public class Quaternion extends Message {
    public double x;
    public double y;
    public double z;
    public double w;

    public Quaternion(MessageDeserializer buf) {
        this.x = buf.getDouble();
        this.y = buf.getDouble();
        this.z = buf.getDouble();
        this.w = buf.getDouble();
    }

    @Override
    public void serialize(MessageSerializer buf) {
        buf.putDouble(x);
        buf.putDouble(y);
        buf.putDouble(z);
        buf.putDouble(w);
    }

    @Override
    public String toString() {
        return "Quaternion{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", w=" + w +
            '}';
    }
}
