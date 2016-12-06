package edu.unc.cs.robotics.ros.msg.actionlib;

import java.util.Arrays;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;
import edu.unc.cs.robotics.ros.msg.MessageSpec;

@MessageSpec(
    type = "actionlib_msgs/GoalStatusArray",
    md5sum = "8b2b82f13216d0a8ea88bd3af735e619",
    definition = "# Stores the statuses for goals that are currently being tracked\n" +
        "# by an action server\n" +
        "Header header\n" +
        "GoalStatus[] status_list\n" +
        "\n" +
        "\n" +
        "================================================================================\n" +
        "MSG: std_msgs/Header\n" +
        "# Standard metadata for higher-level stamped data types.\n" +
        "# This is generally used to communicate timestamped data \n" +
        "# in a particular coordinate frame.\n" +
        "# \n" +
        "# sequence ID: consecutively increasing ID \n" +
        "uint32 seq\n" +
        "#Two-integer timestamp that is expressed as:\n" +
        "# * stamp.sec: seconds (stamp_secs) since epoch (in Python the variable is called 'secs')\n" +
        "# * stamp.nsec: nanoseconds since stamp_secs (in Python the variable is called 'nsecs')\n" +
        "# time-handling sugar is provided by the client library\n" +
        "time stamp\n" +
        "#Frame this data is associated with\n" +
        "# 0: no frame\n" +
        "# 1: global frame\n" +
        "string frame_id\n" +
        "\n" +
        "================================================================================\n" +
        "MSG: actionlib_msgs/GoalStatus\n" +
        "GoalID goal_id\n" +
        "uint8 status\n" +
        "uint8 PENDING         = 0   # The goal has yet to be processed by the action server\n" +
        "uint8 ACTIVE          = 1   # The goal is currently being processed by the action server\n" +
        "uint8 PREEMPTED       = 2   # The goal received a cancel request after it started executing\n" +
        "                            #   and has since completed its execution (Terminal State)\n" +
        "uint8 SUCCEEDED       = 3   # The goal was achieved successfully by the action server (Terminal State)\n" +
        "uint8 ABORTED         = 4   # The goal was aborted during execution by the action server due\n" +
        "                            #    to some failure (Terminal State)\n" +
        "uint8 REJECTED        = 5   # The goal was rejected by the action server without being processed,\n" +
        "                            #    because the goal was unattainable or invalid (Terminal State)\n" +
        "uint8 PREEMPTING      = 6   # The goal received a cancel request after it started executing\n" +
        "                            #    and has not yet completed execution\n" +
        "uint8 RECALLING       = 7   # The goal received a cancel request before it started executing,\n" +
        "                            #    but the action server has not yet confirmed that the goal is canceled\n" +
        "uint8 RECALLED        = 8   # The goal received a cancel request before it started executing\n" +
        "                            #    and was successfully cancelled (Terminal State)\n" +
        "uint8 LOST            = 9   # An action client can determine that a goal is LOST. This should not be\n" +
        "                            #    sent over the wire by an action server\n" +
        "\n" +
        "#Allow for the user to associate a string with GoalStatus for debugging\n" +
        "string text\n" +
        "\n" +
        "\n" +
        "================================================================================\n" +
        "MSG: actionlib_msgs/GoalID\n" +
        "# The stamp should store the time at which this goal was requested.\n" +
        "# It is used by an action server when it tries to preempt all\n" +
        "# goals that were requested before a certain time\n" +
        "time stamp\n" +
        "\n" +
        "# The id provides a way to associate feedback and\n" +
        "# result message with specific goal requests. The id\n" +
        "# specified must be unique.\n" +
        "string id\n" +
        "\n"
)
public class GoalStatusArray extends Message {

    public Header header;
    public GoalStatus[] statusList;

    public GoalStatusArray() {
    }

    public GoalStatusArray(MessageDeserializer buf) {
        header = new Header(buf);
        statusList = buf.getMessageArray(GoalStatus[]::new, GoalStatus::new);
    }

    @Override
    public void serialize(MessageSerializer ser) {
        header.serialize(ser);
        ser.putMessageArray(statusList);
    }

    @Override
    public String toString() {
        return "GoalStatusArray{" +
               "header=" + header +
               ", statusList=" + Arrays.toString(statusList) +
               '}';
    }
}
