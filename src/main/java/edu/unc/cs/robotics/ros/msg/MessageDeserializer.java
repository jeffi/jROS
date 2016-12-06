package edu.unc.cs.robotics.ros.msg;

import java.util.function.Function;
import java.util.function.IntFunction;

public interface MessageDeserializer {
    int getInt();

    byte getByte();

    double getDouble();

    String getString();

    String[] getStringArray();

    double[] getDoubleArray();

    <T> T[] getMessageArray(
        IntFunction<T[]> arrayAlloc,
        Function<MessageDeserializer,T> itemFn);

    default long getDuration() {
        int sec = getInt();
        int nsec = getInt();

        // make sure to properly handle seconds as a uint32
        // otherwise this will overflow in the year 2038
        return (sec & 0xffff_ffffL) * 1_000_000_000L + nsec;
    }

    default long getTime() {
        // there is a problem with ros::Time's representation
        // it stores seconds as a uint32, which will overflow
        // when seconds reaches 4,294,967,295 + 1
        // This will overflow in the year 2106 if properly handling unsigned
        // or in 2038 if it is treated as a signed integer anywhere.
        return getDuration();
    }
}