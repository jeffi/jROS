package edu.unc.cs.robotics.ros.xmlrpc;

/**
 * Created by jeffi on 7/1/16.
 */
public class XmlrpcException extends Exception {
    private final int _code;

    public XmlrpcException(int code, String msg) {
        super(msg);
        _code = code;
    }

    public XmlrpcException(int code, Throwable cause) {
        super(cause);
        _code = code;
    }

    public XmlrpcException(int code, String msg, Throwable cause) {
        super(msg, cause);
        _code = code;
    }

    public int faultCode() {
        return _code;
    }
}
