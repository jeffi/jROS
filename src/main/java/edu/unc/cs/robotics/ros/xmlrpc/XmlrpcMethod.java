package edu.unc.cs.robotics.ros.xmlrpc;

public interface XmlrpcMethod {
    Object invoke(Object... params) throws XmlrpcException;
}
