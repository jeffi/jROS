package edu.unc.cs.robotics.ros.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

/**
 * Created by jeffi on 3/10/16.
 */
@XmlType(name="methodCall", propOrder ={ "methodName", "params"})
@XmlRootElement(name="methodCall")
public class MethodCall {
    public static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(MethodCall.class, MethodResponse.class);
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }

    @XmlElement(required = true)
    public String methodName;

    @XmlElementWrapper(name="params")
    @XmlElement(name="param")
    public List<Param> params = new ArrayList<>();

    public MethodCall() {
    }

    public MethodCall(String name) {
        this.methodName = name;
    }

    public MethodCall addParam(String value) {
        params.add(new Param(new Value(value)));
        return this;
    }

    public MethodCall addParam(Object value) {
        params.add(new Param(new Value(value)));
        return this;
    }

    public MethodCall addParam(int value) {
        params.add(new Param(new Value(value)));
        return this;
    }

    public MethodCall addParam(String[] strings) {
        params.add(new Param(new Value(strings)));
        return this;
    }

    public void addParam(String[][] array) {
        params.add(new Param(new Value(array)));
    }

    public Object getParam(int index) {
        return params.get(index).value.toJavaObject();
    }

    public static MethodCall parse(InputStream in) throws IOException {
        try {
            return (MethodCall)JAXB_CONTEXT.createUnmarshaller().unmarshal(in);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    public byte[] toByteArray() {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JAXB_CONTEXT.createMarshaller().marshal(this, baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    public MethodResponse post(CloseableHttpAsyncClient httpClient, String uri) throws InterruptedException {
        try {
            final HttpPost post = new HttpPost(uri);
            post.setEntity(new ByteArrayEntity(toByteArray(), ContentType.TEXT_XML));
            final HttpResponse httpResponse = httpClient.execute(post, null).get();
            final HttpEntity responseEntity = httpResponse.getEntity();
            long contentLength = responseEntity.getContentLength();
            InputStream contentStream = responseEntity.getContent();
            return MethodResponse.parse(contentStream, (int)contentLength);
        } catch (JAXBException | IOException e) {
            throw new IllegalArgumentException(e);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

}
