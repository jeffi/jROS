package edu.unc.cs.robotics.ros.msg;

public class ByteCountSerializer implements MessageSerializer {
    private int _byteCount = 0;

    @Override
    public void putSeq() {
        putInt(0);
    }

    @Override
    public void putInt(int v) {
        _byteCount += 4;
    }

    @Override
    public void putByte(byte v) {
        _byteCount++;
    }

    @Override
    public void putDouble(double v) {
        _byteCount += 8;
    }

    @Override
    public void putString(String str) {
        _byteCount += 4 + str.length();
    }

    public int getByteCount() {
        return _byteCount;
    }
}
