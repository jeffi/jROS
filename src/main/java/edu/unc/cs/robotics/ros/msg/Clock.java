package edu.unc.cs.robotics.ros.msg;

/**
 * Created by jeffi on 10/24/16.
 */
public class Clock extends Message {

    public static final String DATATYPE = "rosgraph_msgs/Clock";
    public static final String MD5SUM = "a9c97c1d230cfc112e270351a944ee47";
    public static final String DEFINITION =
        "# roslib/Clock is used for publishing simulated time in ROS. \n" +
        "# This message simply communicates the current time.\n" +
        "# For more information, see http://www.ros.org/wiki/Clock\n" +
        "time clock\n";

    public static final MetaMessage<Clock> META = new MetaMessageImpl<>(DATATYPE, MD5SUM, DEFINITION, Clock::new);

    public long clock;

    public Clock(MessageDeserializer buf) {
        this.clock = buf.getTime();
    }

    @Override
    public void serialize(MessageSerializer ser) {
        ser.putTime(clock);
    }

    @Override
    public String toString() {
        return "Clock{" + clock + '}';
    }
}
