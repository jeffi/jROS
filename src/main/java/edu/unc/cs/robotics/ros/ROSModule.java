package edu.unc.cs.robotics.ros;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.unc.cs.robotics.ros.msg.JointState;
import edu.unc.cs.robotics.ros.network.NetworkServer;
import edu.unc.cs.robotics.ros.topic.TopicManager;
import edu.unc.cs.robotics.ros.xmlrpc.JettyXmlrpcServer;
import edu.unc.cs.robotics.ros.xmlrpc.XmlrpcServer;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ROSModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ROSModule.class);

    private final String _name;
    private final String _host;
    private final URI _masterUri;
    private final HostNameMap _hostNameMap;
    private int _port;
    private Map<String, String> _remappings = new HashMap<>();
    private boolean _anonymousName;

    private int _scheduledThreadPoolSize = 1;

    private ROSModule(String name, String host, int port, URI masterUri, HostNameMap hostNameMap) {
        _name = name;
        _host = host;
        _port = port;
        _masterUri = masterUri;
        _hostNameMap = hostNameMap;
    }

    @Override
    protected void configure() {

        URI rosMasterUri = _masterUri;
        if (rosMasterUri == null) {
            String rosMasterUriEnv = System.getenv("ROS_MASTER_URI");
            if (rosMasterUriEnv != null) {
                try {
                    rosMasterUri = new URI(rosMasterUriEnv);
                } catch (URISyntaxException e) {
                    LOG.error("ROS_MASTER_URI is invalid", e);
                    throw new IllegalArgumentException(e);
                }
            }
            rosMasterUri = URI.create("http://localhost:11311/");
            LOG.warn("ROS_MASTER_URI not configured, using default: " + rosMasterUriEnv);
        }

        LOG.info("Using ROS master at "+rosMasterUri);

        Names names = new Names(_name, _remappings, _anonymousName);

        // ros::console::setFixedFilterToken("node", name);

        CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault();
        httpAsyncClient.start();

        bind(Names.class).toInstance(names);
        bind(HttpAsyncClient.class).toInstance(httpAsyncClient);
        bind(CloseableHttpAsyncClient.class).toInstance(httpAsyncClient);
        bind(XmlrpcServer.class).to(JettyXmlrpcServer.class);
        bind(URI.class).annotatedWith(ROSMaster.class).toInstance(rosMasterUri);
        bind(HostNameMap.class).toInstance(_hostNameMap);
        if (_host != null) {
            bind(HostBindingService.class).toInstance(() -> _host);
        } else {
            bind(HostBindingService.class).to(LookupHostBindingService.class);
        }

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();

        // The ScheduledExecutorService is used in this implementation to implement
        // non-blocking delays, e.g. for retries.
        bind(ScheduledExecutorService.class)
            .toInstance(Executors.newScheduledThreadPool(_scheduledThreadPoolSize, threadFactory));

        // The ExecutorService is used to run the listener methods
        bind(ExecutorService.class)
            .toInstance(Executors.newCachedThreadPool(threadFactory));
    }

    @Singleton
    public static class Services {
        private final CloseableHttpAsyncClient _closeableHttpAsyncClient;
        private final List<Service> _services;
        private final ScheduledExecutorService _scheduledExecutorService;
        private final ExecutorService _executorService;

        @Inject
        Services(
            CloseableHttpAsyncClient httpAsyncClient,
            HostBindingService hostBindingService,
            NetworkServer network,
            XmlrpcServer xmlRpc,
            TopicManager topicManager,
            ScheduledExecutorService scheduledExecutorService,
            ExecutorService executorService)
        {
            _closeableHttpAsyncClient = httpAsyncClient;

            _services = Arrays.asList(
                hostBindingService,
                network,
                topicManager,
                xmlRpc
            );

            _scheduledExecutorService = scheduledExecutorService;
            _executorService = executorService;
        }

        public void start() {
            _closeableHttpAsyncClient.start();

            if (_executorService.isShutdown()) {
                throw new IllegalStateException();
            }

            for (Service service : _services) {
                service.start();
            }
        }

        public void stop() {
            // stop in reverse order
            ListIterator<Service> it = _services.listIterator(_services.size());
            while (it.hasPrevious()) {
                it.previous().stop();
            }

            try {
                _closeableHttpAsyncClient.close();
            } catch (IOException e) {
                LOG.warn("IOException closing http client", e);
            }

            // Shut down the scheduledExecutorService and retrieve the list
            // of remaining items
            List<Runnable> runnables = _scheduledExecutorService.shutdownNow();

            // Submit all remaining scheduled items to be run now on the
            // executorService.
            for (Runnable runnable : runnables) {
                _executorService.submit(runnable);
            }

            // Also submit a task that waits for the scheduledExecutorService
            // to terminate.  This will task will wait for any currently
            // running schedules services to terminate.  By submitting it
            // we also make sure that the _executorService will only terminate
            // once the _scheduledExecutorService terminates.
            _executorService.submit(() -> {
                try {
                    _scheduledExecutorService.awaitTermination(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted waiting for scheduled executor service", e);
                }
            });

            // Then shut down the _executorService so that we can wait for
            // its termination
            _executorService.shutdown();

            try {
                _executorService.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                LOG.warn("interrupted waiting for executor service", e);
            }
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        final String _name;
        String _host;
        int _port;
        HostNameMap _hostNameMap = new HostNameMap();
        URI _master;

        private Builder(String name) {
            _name = name;
        }

        public Builder host(String host) {
            _host = host;
            return this;
        }

        public Builder hostMap(String from, String to) {
            _hostNameMap.addMapping(from, to);
            return this;
        }

        public Builder master(URI master) {
            _master = master;
            return this;
        }

        public Builder master(String master) {
            return master(URI.create(master));
        }

        public ROSModule build() {
            return new ROSModule(_name, _host, _port, _master, _hostNameMap);
        }
    }

    public static void main(String[] args) throws SocketException {
        URI rosMaster = URI.create("http://localhost:11311/"); // TODO: replace with actual

        Injector injector = Guice.createInjector(ROSModule.builder("mymodule")
            .hostMap("fetch", rosMaster.getHost())
            .master(rosMaster)
            .build());


//        Injector injector = Guice.createInjector(new ROSModule("robo", "localhost", 9988));
        Services services = injector.getInstance(Services.class);
        services.start();
        Runtime.getRuntime().addShutdownHook(new Thread(services::stop));

        if (true) return;

        ROSMasterClient rosMasterClient = injector.getInstance(ROSMasterClient.class);

        rosMasterClient.getPublishedTopics(
            "",
            (topics) -> topics.stream().sorted().forEach(System.out::println),
            ((faultCode, faultString) -> System.out.println("fault " + faultCode + ": " + faultString)));

        if (false) {
            NodeManager nodeManager = injector.getInstance(NodeManager.class);
            NodeHandle root = nodeManager.node(Name.create("/"));

            root.subscribe(
                JointState.META,
                "/joint_states",
                10,
                Executors.newSingleThreadExecutor(),
                System.out::println);
        }


//
//        NodeHandle nh = nodeManager.node(Name.create("ROS"));
//
//        ExecutorService service = Executors.newSingleThreadExecutor();
//
//        try (Subscriber<JointState> subscriber = nh.subscribe(JointState.META, "joints", 10, service, System.out::println)) {
//            service.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
