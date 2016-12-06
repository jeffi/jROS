package edu.unc.cs.robotics.ros.msg;

import java.util.function.Function;
import java.util.function.IntFunction;

public abstract class Message {
    public abstract void serialize(MessageSerializer ser);

    protected static String[] deserializeStringArray(MessageDeserializer buf) {
        return buf.getStringArray();
    }

    protected static double[] deserializeDoubleArray(MessageDeserializer buf) {
        return buf.getDoubleArray();
    }

    protected static void serialize(MessageSerializer buf, String[] name) {
        buf.putStringArray(name);
    }

    protected static void serialize(MessageSerializer buf, double[] array) {
        buf.putDoubleArray(array);
    }

    protected static <M extends Message> void serialize(MessageSerializer buf, M[] array) {
        buf.putMessageArray(array);
    }

    protected static <M extends Message> M[] deserializeMessageArray(
        IntFunction<M[]> arrayAlloc, Function<MessageDeserializer, M> itemFn,
        MessageDeserializer buf) {
        return buf.getMessageArray(arrayAlloc, itemFn);
    }

    protected static long deserializeDuration(MessageDeserializer buf) {
        return buf.getDuration();
    }
}