package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 7/5/16.
 */
public interface MessageSerializer {

    void putSeq();
    void putInt(int v);
    void putByte(byte v);
    void putDouble(double v);
    void putString(String str);

    default void putStringArray(String[] array) {
        putInt(array.length);
        for (String str : array) {
            putString(str);
        }
    }

    default <M extends Message> void putMessageArray(M[] array) {
        putInt(array.length);
        for (M m : array) {
            m.serialize(this);
        }
    }

    default void putDoubleArray(double[] array) {
        putInt(array.length);
        for (double v : array) {
            putDouble(v);
        }
    }

    default void putDuration(long duration) {
        int sec = (int)(duration / 1_000_000_000L);
        int nsec = (int)(duration % 1_000_000_000L);
        putInt(sec);
        putInt(nsec);
    }

    default void putTime(long time) {
        int sec = (int)(time / 1_000_000_000L);
        int nsec = (int)(time % 1_000_000_000L);
        putInt(sec);
        putInt(nsec);
    }

}
