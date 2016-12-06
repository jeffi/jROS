package edu.unc.cs.robotics.ros;

import java.net.URI;
import java.util.concurrent.Executors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.unc.cs.robotics.ros.msg.Clock;

/**
 * Created by jeffi on 10/24/16.
 */
public class ClockSync {

    public static void main(String[] args) throws InterruptedException {
        final URI fetchMaster = URI.create("http://fetch:11311/");
        final Injector injector = Guice.createInjector(
            ROSModule.builder("clocksync")
                .master(fetchMaster)
                .hostMap("fetch", fetchMaster.getHost())
                .build());

        ROSModule.Services services = injector.getInstance(ROSModule.Services.class);
        services.start();
        Thread shutdownHook = new Thread(
            services::stop,
            services.getClass().getName() + "::stop");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            NodeManager nodeManager = injector.getInstance(NodeManager.class);
            NodeHandle root = nodeManager.node(Name.create("/"));

            try (Subscriber<Clock> clockSubscriber = root.subscribe(
                Clock.META,
                "clock",
                10,
                Executors.newSingleThreadExecutor(),
                ClockSync::clockCallback))
            {
                System.out.println("WAITING");
                Thread.sleep(10000);
                System.out.println("DONE");
            } finally {
                System.out.println("FINALLY");
            }

        } finally {
            services.stop();
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    private static void clockCallback(Clock clock) {
        System.out.println("Clock: " + clock);
    }
}
