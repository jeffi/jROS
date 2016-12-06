package edu.unc.cs.robotics.ros.msg;

import java.nio.ByteBuffer;

/**
 * Created by jeffi on 7/5/16.
 */
public class ByteBufferSerializer implements MessageSerializer {
    private final int _seqNo;
    private final ByteBuffer buf;

    public ByteBufferSerializer(int seqNo, ByteBuffer buffer) {
        _seqNo = seqNo;
        buf = buffer;
    }

    @Override
    public void putSeq() {
        putInt(_seqNo);
    }

    @Override
    public void putInt(int v) {
        buf.putInt(v);
    }

    @Override
    public void putByte(byte v) {
        buf.put(v);
    }

    @Override
    public void putDouble(double v) {
        buf.putDouble(v);
    }

    @Override
    public void putString(String str) {
        final int len = str.length();
        buf.putInt(len);
        for (int i=0 ; i<len ; ++i) {
            buf.put((byte)str.charAt(i));
        }
    }

}
