package edu.unc.cs.robotics.ros;

/**
 * Created by jeffi on 3/11/16.
 */
public class Fault {
    public int faultCode;
    public String faultString;

    public Fault(int faultCode, String faultString) {
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    @Override
    public String toString() {
        return "Fault{" +
            "faultCode=" + faultCode +
            ", faultString='" + faultString + '\'' +
            '}';
    }
}
