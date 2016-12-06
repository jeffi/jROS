package edu.unc.cs.robotics.ros;

public interface HostBindingService extends Service {
    String host();

    @Override
    default void start() {}

    @Override
    default void stop() {}
}
