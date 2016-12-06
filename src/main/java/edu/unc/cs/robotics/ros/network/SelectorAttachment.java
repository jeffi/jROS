package edu.unc.cs.robotics.ros.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;

interface SelectorAttachment {
    default void readable(SelectionKey key) throws IOException {

    }

    default void writable(SelectionKey key) throws IOException {

    }

    default void connectable(SelectionKey key) throws IOException {

    }

    default void acceptable(SelectionKey key) throws IOException {

    }
}
