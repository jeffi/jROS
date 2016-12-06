package edu.unc.cs.robotics.ros.xmlrpc;

import edu.unc.cs.robotics.ros.Service;

public interface XmlrpcServer extends Service {
    String getUri();
    void bind(Object obj);
    void unbind(Object obj);
}
