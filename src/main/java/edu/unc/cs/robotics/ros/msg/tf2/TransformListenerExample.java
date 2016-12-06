package edu.unc.cs.robotics.ros.msg.tf2;

import java.net.URI;
import java.util.concurrent.Executors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.unc.cs.robotics.ros.Name;
import edu.unc.cs.robotics.ros.NodeHandle;
import edu.unc.cs.robotics.ros.NodeManager;
import edu.unc.cs.robotics.ros.ROSModule;
import edu.unc.cs.robotics.ros.Subscriber;


/**
 * Created by jeffi on 10/5/16.
 */
public class TransformListenerExample {
    public static void main(String[] args) {
        new TransformListenerExample().run();
    }

    void run() {
        URI fetchMaster = URI.create("http://fetch:11311/");

        Injector injector = Guice.createInjector(
            ROSModule.builder("tflistener_example")
                .master(fetchMaster)
                .hostMap("fetch", fetchMaster.getHost())
                .build());

        ROSModule.Services services = injector.getInstance(ROSModule.Services.class);
        services.start();
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                services::stop,
                services.getClass().getName() + "::stop"));

        NodeManager nodeManager = injector.getInstance(NodeManager.class);
        NodeHandle root = nodeManager.node(Name.create("/"));

        try (Subscriber<TFMessage> jointStateSub = root.subscribe(
            TFMessage.META,
            "tf",
            100,
            Executors.newSingleThreadExecutor(),
            this::jointStateCallback))
        {
            Thread.sleep(999999999);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void jointStateCallback(TFMessage tfMsg) {
        for (TransformStamped tf : tfMsg.transforms) {
            if ("landmark_green".equals(tf.child_frame_id) ||
                "landmark_blue".equals(tf.child_frame_id))
            {
                System.out.println(tf.child_frame_id + ": " + tf.transform);
            }
        }

    }
}
