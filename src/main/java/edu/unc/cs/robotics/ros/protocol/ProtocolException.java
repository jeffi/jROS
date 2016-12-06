package edu.unc.cs.robotics.ros.protocol;

/**
 * Created by jeffi on 3/11/16.
 */
public class ProtocolException extends Exception {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
