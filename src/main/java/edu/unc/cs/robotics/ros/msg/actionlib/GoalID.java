package edu.unc.cs.robotics.ros.msg.actionlib;


import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;
import edu.unc.cs.robotics.ros.msg.MessageSpec;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.msg.MetaMessageImpl;

@MessageSpec(
    type = GoalID.DATATYPE,
    md5sum = GoalID.MD5SUM,
    definition = GoalID.DEFINITION
)
public class GoalID extends Message {

    public static final String DATATYPE = "actionlib_msgs/GoalID";
    public static final String MD5SUM = "302881f31927c1df708a2dbab0e80ee8";
    public static final String DEFINITION = "" +
                                            "# The stamp should store the time at which this goal was requested.\n" +
                                            "# It is used by an actionlib server when it tries to preempt all\n" +
                                            "# goals that were requested before a certain time\n" +
                                            "time stamp\n" +
                                            "\n" +
                                            "# The id provides a way to associate feedback and\n" +
                                            "# result message with specific goal requests. The id\n" +
                                            "# specified must be unique.\n" +
                                            "string id\n" +
                                            "\n";

    public static final MetaMessage<GoalID> META =
        new MetaMessageImpl<>(
            DATATYPE, MD5SUM, DEFINITION, GoalID::new);


    public long stamp;
    public String id;

    public GoalID() {

    }

    public GoalID(MessageDeserializer ser) {
        stamp = ser.getTime();
        id = ser.getString();
    }

    @Override
    public void serialize(MessageSerializer buf) {
        buf.putTime(stamp);
        buf.putString(id);
    }

    @Override
    public String toString() {
        return "GoalID{"+stamp+", "+id+"}";
    }
}
