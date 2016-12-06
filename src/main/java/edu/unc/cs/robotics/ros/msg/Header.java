package edu.unc.cs.robotics.ros.msg;

import java.util.Objects;

import edu.unc.cs.robotics.ros.ROSTime;

public class Header extends Message {

    // uint32
    public int seq;

    // time
    public long stamp;

    public String frame;

    public Header(MessageDeserializer buf) {
        this.seq = buf.getInt();
        // System.out.println("SEQ: "+seq);
        this.stamp = buf.getTime();
        // System.out.println("STAMP: "+stamp_sec+"."+stamp_nsec);
        this.frame = buf.getString();
    }

    public Header(Header orig) {
        this.seq = orig.seq;
        this.stamp = orig.stamp;
        this.frame = orig.frame;
    }

    public Header(int seq, long stamp, String frame) {
        this.seq = seq;
        this.stamp = stamp;
        this.frame = Objects.requireNonNull(frame);
    }

    public Header(int seq, int stampSec, int stampNSec, String frame) {
        this(seq, ROSTime.convertFromSecNSec(stampSec, stampNSec), frame);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        buf.putSeq();
        buf.putTime(stamp);
        buf.putString(frame);
    }

    @Override
    public String toString() {
        return String.format("Header(#%d,%d.%09d,%s)",
            seq,
            stamp/1_000_000_000L,
            stamp%1_000_000_000L,
            frame);
    }
}
