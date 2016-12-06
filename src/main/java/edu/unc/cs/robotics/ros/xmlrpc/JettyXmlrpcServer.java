package edu.unc.cs.robotics.ros.xmlrpc;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import edu.unc.cs.robotics.ros.HostBindingService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JettyXmlrpcServer implements XmlrpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(JettyXmlrpcServer.class);

    private final XmlrpcBinding _xmlrpcBinding;
    private final XmlrpcServlet _servlet;
    private final HostBindingService _hostBindingService;
    private String _uri;

    private int _maxThreads;
    private int _minThreads;
    private int _idleTimeout;
    private Server _server;

    @Inject
    JettyXmlrpcServer(
        XmlrpcParser parser,
        HostBindingService hostBindingService)
    {
        _xmlrpcBinding = new XmlrpcBinding();
        _hostBindingService = hostBindingService;
        _servlet = new XmlrpcServlet(parser, _xmlrpcBinding);
    }

    @Override
    public String getUri() {
        if (_uri == null) {
            LOG.warn("call to getUri() when server is not running");
        }
        return _uri;
    }

    @Override
    public void bind(Object obj) {
        _xmlrpcBinding.bind(obj);
    }

    @Override
    public void unbind(Object obj) {
        _xmlrpcBinding.unbind(obj);
    }

    @Override
    public synchronized void start() {
        if (_maxThreads > 0) {
            _server = new Server(new QueuedThreadPool(_maxThreads, _minThreads, _idleTimeout));
        } else {
            _server = new Server();
        }

        final String host = _hostBindingService.host();

        ServerConnector connector = new ServerConnector(_server);
        connector.setIdleTimeout(TimeUnit.HOURS.toMillis(1));
        connector.setSoLingerTime(-1);
        connector.setHost(host);
//        connector.setPort(_port);

//        LOG.debug("binding to "+_host+":"+_port);

        _server.addConnector(connector);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(_servlet), "/*");

        _server.setHandler(handler);

        try {
            _server.start();
            _uri = "http://"+host+":"+connector.getLocalPort()+"/";
            LOG.info("started, local URI is {}", _uri);
        } catch (Exception ex) {
            LOG.error("server start failed", ex);
        }

    }

    @Override
    public synchronized void stop() {
        if (_server != null) {
            try {
                _server.stop();
                LOG.info("stopped");
            } catch (Exception ex) {
                LOG.error("stop failed", ex);
            } finally {
                _uri = null;
                _server = null;
            }
        }
    }

}
