package edu.unc.cs.robotics.ros.msg;

import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class ByteBufferDeserializer implements MessageDeserializer {
    private final ByteBuffer buf;

    public ByteBufferDeserializer(ByteBuffer buffer) {
        buf = buffer;
    }

    @Override
    public int getInt() {
        return buf.getInt();
    }

    @Override
    public byte getByte() {
        return buf.get();
    }

    @Override
    public double getDouble() {
        return buf.getDouble();
    }

    @Override
    public String getString() {
        int len = buf.getInt();
        StringBuilder str = new StringBuilder(len);
        for (int i = 0 ; i < len ; ++i) {
            str.append((char)(buf.get() & 0xff));
        }
        return str.toString();
    }

    @Override
    public String[] getStringArray() {
        final int len = buf.getInt();
        final String[] array = new String[len];
        for (int i = 0 ; i < len ; ++i) {
            array[i] = getString();
        }
        return array;
    }

    @Override
    public double[] getDoubleArray() {
        final int len = buf.getInt();
        final double[] array = new double[len];
        for (int i = 0 ; i < len ; ++i) {
            array[i] = buf.getDouble();
        }
        return array;
    }

    @Override
    public <T> T[] getMessageArray(
        IntFunction<T[]> arrayAlloc,
        Function<MessageDeserializer,T> itemFn)
    {
        final int len = buf.getInt();
        final T[] array = arrayAlloc.apply(len);
        for (int i = 0 ; i < len ; ++i) {
            array[i] = itemFn.apply(this);
        }
        return array;
    }
}
