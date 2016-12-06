package edu.unc.cs.robotics.ros.msg.trajectory;

import java.util.Arrays;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageDeserializer;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;

/**
 * Created by jeffi on 6/28/16.
 */
public class JointTrajectoryPoint extends Message {
    public static final String DATATYPE = "trajectory_msgs/JointTrajectoryPoint";
    public static final String MD5SUM = "f3cd1e1c4d320c79d6985c904ae5dcd3";
    public static final String DEFINITION = "" +
                                            "# Each trajectory point specifies either positions[, velocities[, accelerations]]\n" +
                                            "# or positions[, effort] for the trajectory to be executed.\n" +
                                            "# All specified values are in the same order as the joint names in JointTrajectory.msg\n" +
                                            "\n" +
                                            "float64[] positions\n" +
                                            "float64[] velocities\n" +
                                            "float64[] accelerations\n" +
                                            "float64[] effort\n" +
                                            "duration time_from_start\n";

    public double[] positions;
    public double[] velocities;
    public double[] accelerations;
    public double[] effort;
    public long timeFromStart;

    public JointTrajectoryPoint() {

    }

    public JointTrajectoryPoint(
        double[] positions,
        double[] velocities,
        double[] accelerations,
        double[] effort,
        long timeFromStart)
    {
        this.positions = positions;
        this.velocities = velocities;
        this.accelerations = accelerations;
        this.effort = effort;
        this.timeFromStart = timeFromStart;
    }

    public JointTrajectoryPoint(MessageDeserializer buf) {
        positions = deserializeDoubleArray(buf);
        velocities = deserializeDoubleArray(buf);
        accelerations = deserializeDoubleArray(buf);
        effort = deserializeDoubleArray(buf);
        timeFromStart = deserializeDuration(buf);
    }

    @Override
    public void serialize(MessageSerializer buf) {
        serialize(buf, positions);
        serialize(buf, velocities);
        serialize(buf, accelerations);
        serialize(buf, effort);
        buf.putDuration(timeFromStart);
    }

    @Override
    public String toString() {
        return "JointTrajectoryPoint{" +
               "positions=" + Arrays.toString(positions) +
               ", velocities=" + Arrays.toString(velocities) +
               ", accelerations=" + Arrays.toString(accelerations) +
               ", effort=" + Arrays.toString(effort) +
               ", timeFromStart=" + timeFromStart +
               '}';
    }
}
