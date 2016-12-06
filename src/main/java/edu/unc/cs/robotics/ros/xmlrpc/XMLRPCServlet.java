package edu.unc.cs.robotics.ros.xmlrpc;

import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class XmlrpcServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(XmlrpcServlet.class);

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final XmlrpcParser _parser;
    private final XmlrpcBinding _methodMap;
    private final XmlrpcSerializer _serializer = new XmlrpcSerializer();

    public XmlrpcServlet(XmlrpcParser parser, XmlrpcBinding methodMap) {
        _parser = parser;
        _methodMap = methodMap;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String contentType = req.getContentType();
        String charset = null;

        if (contentType == null) {
            LOG.info("missing content-type, expected text/xml");
        } else {
            int semi = contentType.indexOf(';');
            String parameters = null;
            if (semi != -1) {
                parameters = contentType.substring(semi + 1);
                contentType = contentType.substring(0, semi).trim();
            }

            if (!"text/xml".equals(contentType)) {
                LOG.warn("bad request: content type is "+contentType);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (parameters != null) {
                for (String param : parameters.split("\\s*;\\s*")) {
                    int eq = param.indexOf('=');
                    if (eq == -1) {
                        continue;
                    }
                    if ("charset".equalsIgnoreCase(param.substring(0, eq).trim())) {
                        charset = param.substring(eq+1).trim();
                    }
                }
            }
        }

        ServletInputStream in = req.getInputStream();
        InputSource inputSource = new InputSource(in);
        if (charset != null) {
            inputSource.setEncoding(charset);
        }

        MethodCall methodCall;

        try {
            methodCall = _parser.parseMethodCall(inputSource);
        } catch (ParserConfigurationException | SAXException ex) {
            LOG.warn("parse exception", ex);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String methodName = methodCall.getMethodName();
        XmlrpcMethod method = _methodMap.get(methodName);

        if (method == null) {
            LOG.warn("unknown method name: "+methodName);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Object result = method.invoke(methodCall.getParams());
            sendResponse(resp, _serializer.serializeResponse(result));
        } catch (XmlrpcException ex) {
            sendResponse(resp, _serializer.serializeFault(ex));
        }
    }

    private void sendResponse(HttpServletResponse resp, String xml) throws IOException {
        byte[] bytes = xml.getBytes(UTF8);
        resp.setStatus(200);
        resp.setContentType("text/xml; charset=UTF-8");
        resp.setContentLength(bytes.length);
        resp.getOutputStream().write(bytes);
    }

}
