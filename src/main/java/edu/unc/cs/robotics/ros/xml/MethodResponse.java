package edu.unc.cs.robotics.ros.xml;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by jeffi on 3/10/16.
 */
@XmlRootElement(name="methodResponse")
@XmlType(name="methodResponse")
public class MethodResponse {
    @XmlElementWrapper(name="params")
    @XmlElement(name="param")
    public List<Param> params = new ArrayList<>();

    public MethodResponse() {

    }

    public MethodResponse(Object... values) {
        params.add(new Param(new Value(new Array(values))));
    }

    public static MethodResponse parse(
        InputStream contentStream,
        int contentLength) throws IOException, JAXBException
    {
        final byte[] responseBytes = new byte[contentLength];
        new DataInputStream(contentStream).readFully(responseBytes);
        System.out.write(responseBytes);
        return (MethodResponse)MethodCall.JAXB_CONTEXT.createUnmarshaller()
            .unmarshal(new ByteArrayInputStream(responseBytes));
    }

    public Value get(int index) {
        return params.get(index).value;
    }


    @Override
    public String toString() {
        return "MethodResponse{" +
            "params=" + params +
            '}';
    }
}
