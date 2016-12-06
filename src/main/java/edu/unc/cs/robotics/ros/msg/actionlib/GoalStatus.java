package edu.unc.cs.robotics.ros.msg.actionlib;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;

/**
 * Created by jeffi on 6/28/16.
 */
public class GoalStatus extends Message {
    public static final String DATATYPE = "actionlib_msgs/GoalStatus";
    public static final String MD5SUM = "d388f9b87b3c471f784434d671988d4a";
    public static final String DESCRIPTION = "" +
                                             "GoalID goal_id\n" +
                                             "uint8 status\n" +
                                             "uint8 PENDING         = 0   # The goal has yet to be processed by the actionlib server\n" +
                                             "uint8 ACTIVE          = 1   # The goal is currently being processed by the actionlib server\n" +
                                             "uint8 PREEMPTED       = 2   # The goal received a cancel request after it started executing\n" +
                                             "                            #   and has since completed its execution (Terminal State)\n" +
                                             "uint8 SUCCEEDED       = 3   # The goal was achieved successfully by the actionlib server (Terminal State)\n" +
                                             "uint8 ABORTED         = 4   # The goal was aborted during execution by the actionlib server due\n" +
                                             "                            #    to some failure (Terminal State)\n" +
                                             "uint8 REJECTED        = 5   # The goal was rejected by the actionlib server without being processed,\n" +
                                             "                            #    because the goal was unattainable or invalid (Terminal State)\n" +
                                             "uint8 PREEMPTING      = 6   # The goal received a cancel request after it started executing\n" +
                                             "                            #    and has not yet completed execution\n" +
                                             "uint8 RECALLING       = 7   # The goal received a cancel request before it started executing,\n" +
                                             "                            #    but the actionlib server has not yet confirmed that the goal is canceled\n" +
                                             "uint8 RECALLED        = 8   # The goal received a cancel request before it started executing\n" +
                                             "                            #    and was successfully cancelled (Terminal State)\n" +
                                             "uint8 LOST            = 9   # An actionlib client can determine that a goal is LOST. This should not be\n" +
                                             "                            #    sent over the wire by an actionlib server\n" +
                                             "\n" +
                                             "#Allow for the user to associate a string with GoalStatus for debugging\n" +
                                             "string text\n" +
                                             "\n" +
                                             "\n" +
                                             "================================================================================\n" +
                                             "MSG: actionlib_msgs/GoalID\n" +
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

    public enum Status {
        PENDING(0),
        ACTIVE(1),
        PREEMPTED(2),
        PREEMPTING(3),
        SUCCEEDED(4),
        ABORTED(5),
        REJECTED(6),
        RECALLING(7),
        RECALLED(8),
        LOST(9),
        ;

        public final byte serialValue;

        Status(int serialValue) {
            this.serialValue = (byte)serialValue;
        }


        public static Status deserialize(MessageDeserializer buf) {
            final int value = buf.getByte() & 0xff;
            switch (value) {
            case 0: return PENDING;
            case 1: return ACTIVE;
            case 2: return PREEMPTED;
            case 3: return PREEMPTING;
            case 4: return SUCCEEDED;
            case 5: return ABORTED;
            case 6: return REJECTED;
            case 7: return RECALLING;
            case 8: return RECALLED;
            case 9: return LOST;
            default:
                throw new IllegalArgumentException(
                    String.format("invalid status (0x%x)", value));
            }
        }
    }

    public GoalID goalId;
    public Status status;
    public String text;

    public GoalStatus() {

    }

    public GoalStatus(MessageDeserializer buf) {
        goalId = new GoalID(buf);
        status = Status.deserialize(buf);
        text = buf.getString();
    }

    @Override
    public void serialize(MessageSerializer buf) {
        goalId.serialize(buf);
        buf.putByte(status.serialValue);
        buf.putString(text);
    }

    @Override
    public String toString() {
        return "GoalStatus{" +
            "goalId=" + goalId +
            ", status=" + status +
            ", text='" + text + '\'' +
            '}';
    }
}
