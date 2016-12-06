package edu.unc.cs.robotics.ros.xmlrpc;

import java.io.StringReader;
import java.util.Arrays;

import junit.framework.TestCase;
import org.xml.sax.InputSource;

/**
 * Created by jeffi on 7/5/16.
 */
public class XmlrpcParserTest extends TestCase {
    public void testParseResponse() throws Exception {
        String input = "<?xml version='1.0'?>\n" +
                       "<methodResponse>\n" +
                       "<params>\n" +
                       "<param>\n" +
                       "<value><array><data>\n" +
                       "<value><int>1</int></value>\n" +
                       "<value><string>current topics</string></value>\n" +
                       "<value><array><data>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/torso_controller/follow_joint_trajectory/cancel</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalID</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/follow_joint_trajectory/result</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_registered/hw_registered/image_rect_raw</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/led_action/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/led_action/result</string></value>\n" +
                       "<value><string>robot_calibration_msgs/GripperLedCommandActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/robot_state</string></value>\n" +
                       "<value><string>fetch_driver_msgs/RobotState</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_rectify_depth/parameter_updates</string></value>\n" +
                       "<value><string>dynamic_reconfigure/Config</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/query_controller_states/result</string></value>\n" +
                       "<value><string>robot_controllers_msgs/QueryControllerStatesActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/odom_combined</string></value>\n" +
                       "<value><string>nav_msgs/Odometry</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth/points</string></value>\n" +
                       "<value><string>sensor_msgs/PointCloud2</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/gripper_action/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/base_scan</string></value>\n" +
                       "<value><string>sensor_msgs/LaserScan</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/rgb/image_rect_color</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth/image</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth/image_rect_raw</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/graft/state</string></value>\n" +
                       "<value><string>graft/GraftState</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/teleop/cmd_vel</string></value>\n" +
                       "<value><string>geometry_msgs/Twist</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/follow_joint_trajectory/feedback</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/query_controller_states/goal</string></value>\n" +
                       "<value><string>robot_controllers_msgs/QueryControllerStatesActionGoal</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/battery_state</string></value>\n" +
                       "<value><string>power_msgs/BatteryState</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/point_head/result</string></value>\n" +
                       "<value><string>control_msgs/PointHeadActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/arm_controller/follow_joint_trajectory/feedback</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/imu</string></value>\n" +
                       "<value><string>sensor_msgs/Imu</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_registered/points</string></value>\n" +
                       "<value><string>sensor_msgs/PointCloud2</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/gripper_action/goal</string></value>\n" +
                       "<value><string>control_msgs/GripperCommandActionGoal</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_registered_rectify_depth/parameter_descriptions</string></value>\n" +
                       "<value><string>dynamic_reconfigure/ConfigDescription</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/arm_with_torso_controller/follow_joint_trajectory/result</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/arm_controller/follow_joint_trajectory/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/torso_controller/follow_joint_trajectory/result</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/gripper_action/result</string></value>\n" +
                       "<value><string>control_msgs/GripperCommandActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/sick_tim551_2050001/parameter_descriptions</string></value>\n" +
                       "<value><string>dynamic_reconfigure/ConfigDescription</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/tf</string></value>\n" +
                       "<value><string>tf2_msgs/TFMessage</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_registered/hw_registered/image_rect</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/torso_controller/follow_joint_trajectory/goal</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionGoal</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/base_controller/command</string></value>\n" +
                       "<value><string>geometry_msgs/Twist</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/base_scan_raw</string></value>\n" +
                       "<value><string>sensor_msgs/LaserScan</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/query_controller_states/feedback</string></value>\n" +
                       "<value><string>robot_controllers_msgs/QueryControllerStatesActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/point_head/feedback</string></value>\n" +
                       "<value><string>control_msgs/PointHeadActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_rectify_depth/parameter_descriptions</string></value>\n" +
                       "<value><string>dynamic_reconfigure/ConfigDescription</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/follow_joint_trajectory/goal</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionGoal</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/tf_static</string></value>\n" +
                       "<value><string>tf2_msgs/TFMessage</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/diagnostics</string></value>\n" +
                       "<value><string>diagnostic_msgs/DiagnosticArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/sick_tim551_2050001/parameter_updates</string></value>\n" +
                       "<value><string>dynamic_reconfigure/Config</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_registered_rectify_depth/parameter_updates</string></value>\n" +
                       "<value><string>dynamic_reconfigure/Config</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/joint_states</string></value>\n" +
                       "<value><string>sensor_msgs/JointState</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/rosout</string></value>\n" +
                       "<value><string>rosgraph_msgs/Log</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/query_controller_states/cancel</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalID</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/sound_play/feedback</string></value>\n" +
                       "<value><string>sound_play/SoundRequestActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/gripper_action/cancel</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalID</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth/image_rect</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/query_controller_states/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/joy</string></value>\n" +
                       "<value><string>sensor_msgs/Joy</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/rectify_color/parameter_updates</string></value>\n" +
                       "<value><string>dynamic_reconfigure/Config</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/sound_play/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/torso_controller/follow_joint_trajectory/feedback</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/torso_controller/follow_joint_trajectory/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/gripper_action/feedback</string></value>\n" +
                       "<value><string>control_msgs/GripperCommandActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/rosout_agg</string></value>\n" +
                       "<value><string>rosgraph_msgs/Log</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/odom</string></value>\n" +
                       "<value><string>nav_msgs/Odometry</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/follow_joint_trajectory/cancel</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalID</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/cmd_vel_mux/selected</string></value>\n" +
                       "<value><string>std_msgs/String</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/depth_registered/image</string></value>\n" +
                       "<value><string>sensor_msgs/Image</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_camera/rectify_color/parameter_descriptions</string></value>\n" +
                       "<value><string>dynamic_reconfigure/ConfigDescription</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/sound_play/result</string></value>\n" +
                       "<value><string>sound_play/SoundRequestActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/gripper_controller/led_action/feedback</string></value>\n" +
                       "<value><string>robot_calibration_msgs/GripperLedCommandActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/point_head/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/arm_with_torso_controller/follow_joint_trajectory/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/head_controller/follow_joint_trajectory/status</string></value>\n" +
                       "<value><string>actionlib_msgs/GoalStatusArray</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/arm_controller/follow_joint_trajectory/result</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionResult</string></value>\n" +
                       "</data></array></value>\n" +
                       "<value><array><data>\n" +
                       "<value><string>/arm_with_torso_controller/follow_joint_trajectory/feedback</string></value>\n" +
                       "<value><string>control_msgs/FollowJointTrajectoryActionFeedback</string></value>\n" +
                       "</data></array></value>\n" +
                       "</data></array></value>\n" +
                       "</data></array></value>\n" +
                       "</param>\n" +
                       "</params>\n" +
                       "</methodResponse>";

        XmlrpcParser parser = new XmlrpcParser();
        InputSource inputSource = new InputSource(new StringReader(input));

        MethodResponse response = parser.parseMethodResponse(inputSource);

        assertNotNull(response.result);
        assertNull(response.faultString);

        Object result = response.result;
        assertEquals(Object[].class, result.getClass());
        Object[] array = (Object[])response.result;
        assertEquals(3, array.length);
        assertEquals(new Integer(1), array[0]);
        assertEquals("current topics", array[1]);
        assertEquals(Object[].class, array[2].getClass());

        Object[] topics = (Object[])array[2];

        assertEquals(70, topics.length);
        Object[] topic = (Object[])topics[0];

        assertEquals(2, topic.length);
        assertEquals("/torso_controller/follow_joint_trajectory/cancel", topic[0]);
        assertEquals("actionlib_msgs/GoalID", topic[1]);
    }

    public void testRequestTopicResult() throws Exception {
        String input = "<?xml version=\"1.0\"?>\n" +
                       "<methodResponse><params><param>\n" +
                       "\t<value><array><data><value><i4>1</i4></value><value></value><value><array><data><value>TCPROS</value><value>fetch19</value><value><i4>51225</i4></value></data></array></value></data></array></value>\n" +
                       "</param></params></methodResponse>\n";

        InputSource inputSource = new InputSource(new StringReader(input));
        XmlrpcParser parser = new XmlrpcParser();

        MethodResponse response = parser.parseMethodResponse(inputSource);
        assertNotNull(response.result);
        assertNull(response.faultString);
        assertEquals(Object[].class, response.result.getClass());

        Object[] array = (Object[])response.result;
        assertEquals(3, array.length);
        assertEquals(1, array[0]);
        assertEquals("", array[1]);
        assertEquals(Object[].class, array[2].getClass());

        Object[] protoList = (Object[])array[2];
        assertEquals(3, protoList.length);
        System.out.println(Arrays.asList(protoList));
        assertEquals("TCPROS", protoList[0]);
        assertEquals("fetch19", protoList[1]);
        assertEquals(51225, protoList[2]);
    }
}