package edu.unc.cs.robotics.ros;

/**
 * Created by jeffi on 3/11/16.
 */
public class ProtocolParam {
    public final String name;
    public final String host;
    public final int port;

    public ProtocolParam(String protocolName, String host, int port) {
        this.name = protocolName;
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return "ProtocolParam{" +
            "name='" + name + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            '}';
    }
}
