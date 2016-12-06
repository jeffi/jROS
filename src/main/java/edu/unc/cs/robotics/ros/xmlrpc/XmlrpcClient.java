package edu.unc.cs.robotics.ros.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;

import edu.unc.cs.robotics.ros.HostNameMap;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.client.HttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class XmlrpcClient {
    private static final Logger LOG = LoggerFactory.getLogger(XmlrpcClient.class);

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ContentType CONTENT_TYPE = ContentType.TEXT_XML.withCharset(UTF8);

    private final HttpAsyncClient _client;
    private final HostNameMap _hostNameMap;
    private final ScheduledExecutorService _scheduledExecutorService;
    private final XmlrpcSerializer _serializer = new XmlrpcSerializer();
    private final XmlrpcParser _parser = new XmlrpcParser();

    public interface FaultConsumer {
        void onFault(int faultCode, String faultString);
    }

    public interface Dispatch {
        Dispatch onSuccess(Consumer<Object> callback);
        Dispatch onFault(FaultConsumer callback);
        Dispatch onError(Consumer<Throwable> callback);
        Dispatch onCancelled(Runnable callback);
        void invoke();
        void invokeLater(long delay, TimeUnit timeUnit);
    }

    @Inject
    public XmlrpcClient(HttpAsyncClient client, HostNameMap hostNameMap, ScheduledExecutorService scheduledExecutorService) {
        _client = client;
        _hostNameMap = hostNameMap;
        _scheduledExecutorService = scheduledExecutorService;
    }

    private static void defaultFaultConsumer(int faultCode, String faultString) {
        LOG.warn("invoke failed with fault "+faultCode+": "+faultString);
    }

    private static void defaultErrorConsumer(Throwable ex) {
        LOG.warn("invoke failed with error", ex);
    }

    private static void defaultCancelledConsumer() {
        LOG.warn("invoke cancelled");
    }

    public Dispatch prepare(String uri, String methodName, Object... params) {
        return prepare(URI.create(uri), methodName, params);
    }

    public Dispatch prepare(URI uri, String methodName, Object... params) {
        final HttpPost post = new HttpPost(_hostNameMap.remap(uri));
        String call = _serializer.serializeCall(methodName, params);
        LOG.debug("Preparing "+ methodName +": "+call);
        post.setEntity(new ByteArrayEntity(call.getBytes(UTF8), CONTENT_TYPE));

        return new Dispatch() {
            private Consumer<Object> _success;
            private FaultConsumer _fault = XmlrpcClient::defaultFaultConsumer;
            private Consumer<Throwable> _error = XmlrpcClient::defaultErrorConsumer;
            private Runnable _cancelled = XmlrpcClient::defaultCancelledConsumer;

            @Override
            public Dispatch onSuccess(Consumer<Object> callback) {
                _success = callback;
                return this;
            }

            @Override
            public Dispatch onFault(FaultConsumer callback) {
                _fault = callback;
                return this;
            }

            @Override
            public Dispatch onError(Consumer<Throwable> callback) {
                _error = callback;
                return this;
            }

            @Override
            public Dispatch onCancelled(Runnable callback) {
                _cancelled = callback;
                return this;
            }

            @Override
            public void invokeLater(long delay, TimeUnit timeUnit) {
                if (delay <= 0) {
                    invoke();
                } else {
                    _scheduledExecutorService.schedule(this::invoke, delay, timeUnit);
                }
            }

            @Override
            public void invoke() {
                if (_success == null) {
                    throw new IllegalStateException("onSuccess must be called before invoke");
                }

                _client.execute(post, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse httpResponse) {
                        try {
                            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                                _error.accept(new IOException("expected 200 OK, got " + httpResponse.getStatusLine()));
                                return;
                            }

                            HttpEntity entity = httpResponse.getEntity();
                            if (entity == null) {
                                _error.accept(new IOException("response does not contain an entity"));
                                return;
                            }

                            Header contentType = entity.getContentType();
                            if (contentType == null || contentType.getValue() == null) {
                                _error.accept(new IOException("response does not have a content-type header"));
                                return;
                            }

                            if (!contentType.getValue().matches("text/xml(?:;.*)?")) {
                                _error.accept(new IOException("response content-type is not text/xml"));
                                return;
                            }

                            MethodResponse methodResponse;

                            try {
                                long contentLength = entity.getContentLength();
                                byte[] data = new byte[(int)contentLength];
                                new DataInputStream(entity.getContent()).readFully(data);
                                LOG.debug("GOT response: " + new String(data, "US-ASCII"));
                                InputSource inputSource = new InputSource(new ByteArrayInputStream(data));
                                methodResponse = _parser.parseMethodResponse(inputSource);
                            } catch (Throwable ex) {
                                LOG.error("Exception processing result", ex);
                                _error.accept(ex);
                                return;
                            }

                            if (methodResponse.result != null) {
                                _success.accept(methodResponse.result);
                            } else {
                                _fault.onFault(methodResponse.faultCode, methodResponse.faultString);
                            }
                        } catch (Throwable ex) {
                            LOG.error("uncaught exception", ex);
                        }
                    }

                    @Override
                    public void failed(Exception ex) {
                        try {
                            _error.accept(ex);
                        } catch (Throwable bad) {
                            LOG.error("uncaught exception", bad);
                        }
                    }

                    @Override
                    public void cancelled() {
                        try {
                            _cancelled.run();
                        } catch (Throwable ex) {
                            LOG.error("uncaught exception", ex);
                        }
                    }
                });
            }
        };
    }

}
