package edu.unc.cs.robotics.ros.xmlrpc;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

import org.owasp.encoder.Encode;

public class XmlrpcSerializer {
    public String serializeFault(XmlrpcException ex) {
        int faultCode = ex.faultCode();
        String faultString = ex.getMessage();

        return ("<?xml version=\"1.0\"?>\n" +
            "<methodResponse>\n" +
            "<fault>\n" +
            "<value>\n" +
            "<struct>\n" +
            "<member>\n" +
            "<name>faultCode</name>\n" +
            "<value><int>") +
            faultCode +
            "</int></value>\n" +
            "</member>\n" +
            "<member>\n" +
            "<name>faultString</name>\n" +
            "<value><string>" +
            Encode.forXmlContent(faultString) +
            "</string></value>\n" +
            "</member>\n" +
            "</struct>\n" +
            "</value>\n" +
            "</fault>\n" +
            "</methodResponse>\n";
    }

    public String serializeResponse(Object result) {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\"?>\n" +
            "<methodResponse>\n" +
            "<params>\n" +
            "<param>\n" +
            "<value>");
        serializeValue(buf, result);
        buf.append("</value>\n" +
            "</param>\n" +
            "</params>\n" +
            "</methodResponse>\n");
        return buf.toString();
    }

    public String serializeCall(String methodName, Object... params) {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\"?>\n" +
            "<methodCall>\n" +
            "<methodName>");
        buf.append(Encode.forXmlContent(methodName));
        buf.append("</methodName>" +
            "<params>\n");
        for (Object param : params) {
            buf.append("<param>\n<value>");
            serializeValue(buf, param);
            buf.append("</value>\n</param>\n");
        }
        buf.append("</params>\n" +
            "</methodCall>\n");
        return buf.toString();
    }

    private void serializeValue(StringBuilder buf, Object value) {
        if (value instanceof Number) {
            if (value instanceof Integer || value instanceof Short || value instanceof Byte || value instanceof Long) {
                buf.append("<int>");
                buf.append(value);
                buf.append("</int>");
            } else if (value instanceof Double || value instanceof Float) {
                buf.append("<double>");
                buf.append(value);
                buf.append("</double>");
            } else {
                throw new IllegalArgumentException("unable to serialize type: " + value.getClass().getName());
            }
        } else if (value instanceof String) {
            buf.append("<string>");
            buf.append(Encode.forXmlContent((String)value));
            buf.append("</string>");
        } else if (value instanceof Boolean) {
            buf.append("<boolean>");
            buf.append(((Boolean)value) ? "1" : "0");
            buf.append("</boolean>");
        } else if (value instanceof Object[]) {
            buf.append("<array>\n<data>\n");
            for (Object v : (Object[])value) {
                buf.append("<value>");
                serializeValue(buf, v);
                buf.append("</value>");
            }
            buf.append("</data>\n</array>\n");
        } else if (value instanceof Collection) {
            buf.append("<array>\n<data>\n");
            for (Object v : (Collection)value) {
                buf.append("<value>");
                serializeValue(buf, v);
                buf.append("</value>");
            }
            buf.append("</data>\n</array>\n");
        } else if (value instanceof Map) {
            buf.append("<struct>\n");
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>)value;
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                buf.append("<member>\n<name>");
                buf.append(Encode.forXmlContent(entry.getKey()));
                buf.append("</name>\n<value>");
                serializeValue(buf, entry.getValue());
                buf.append("</value>\n</member>\n");
            }
            buf.append("</struct>\n");
        } else if (value instanceof Date) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime((Date)value);
            buf.append("<dateTime.iso8601>");
            buf.append(String.format(
                Locale.US, "%04d%02d%02dT%02d:%02d:%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DATE),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND)));
            buf.append("</dateTime.iso8601>");
        } else if (value instanceof byte[]) {
            throw new UnsupportedOperationException("base64 not supported yet, implement me!");
        } else {
            throw new IllegalArgumentException("unable to serialize: " + value);
        }
    }

}
