package edu.unc.cs.robotics.ros;

import org.owasp.encoder.Encode;

/**
 * Builds an XMLRPC method call XML string.  Instances of this class may only
 * be used once.  Attempts to reuse will cause a NullPointerException.
 */
public class MethodCallBuilder {
    private StringBuilder _buf = new StringBuilder();

    public MethodCallBuilder(String methodName) {
        _buf.append("<?xml version='1.0'?>\n"+
            "<methodCall>\n"+
            "<methodName>")
            .append(Encode.forXmlContent(methodName))
            .append("</methodName>\n")
            .append("<params>");
    }

    public MethodCallBuilder addParam(String param) {
        _buf.append("<param><value>");
        appendString(param);
        _buf.append("</value></param>\n");
        return this;
    }

    public MethodCallBuilder addParam(int value) {
        _buf.append("<param><value>");
        appendInt(value);
        _buf.append("</value></param>\n");
        return this;
    }

    public MethodCallBuilder addParam(boolean value) {
        _buf.append("<param><value>");
        appendBoolean(value);
        _buf.append("</value></param>\n");
        return this;
    }

    public MethodCallBuilder addParam(Object... array) {
        _buf.append("<param><value>");
        appendArray(array);
        _buf.append("</value></param>\n");
        return this;
    }

    private void appendValue(Object value) {
        _buf.append("<value>");
        if (value instanceof String) {
            appendString((String)value);
        } else if (value instanceof Number) {
            if ((value instanceof Double) || (value instanceof Float)) {
                appendDouble(((Number)value).doubleValue());
            } else if ((value instanceof Integer) || (value instanceof Long) || (value instanceof Short) || (value instanceof Byte)) {
                appendInt(((Number)value).intValue());
            } else {
                throw new IllegalArgumentException();
            }
        } else if (value instanceof Boolean) {
            appendBoolean((Boolean)value);
        } else if (value instanceof Object[]) {
            appendArray((Object[])value);
        } else {
            throw new IllegalArgumentException();
        }
        _buf.append("</value>");
    }

    private void appendArray(Object[] array) {
        _buf.append("<array><data>");
        for (Object value : array) {
            appendValue(value);
        }
        _buf.append("</data></array>");
    }

    private void appendString(String value) {
        _buf.append("<string>").append(Encode.forXmlContent(value)).append("</string>");
    }

    private void appendDouble(double value) {
        _buf.append("<double>").append(value).append("</double>");
    }

    private void appendInt(int value) {
        _buf.append("<int>").append(value).append("</int>");
    }

    private void appendBoolean(boolean value) {
        _buf.append("<boolean>").append(value ? '1':'0').append("</boolean>");
    }

    public String build() {
        String xml = _buf.append("</params>\n</methodCall>")
                .toString();
        _buf = null;
        return xml;
    }
}
