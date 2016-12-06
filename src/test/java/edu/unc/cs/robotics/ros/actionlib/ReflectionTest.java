package edu.unc.cs.robotics.ros.actionlib;

import java.lang.reflect.ParameterizedType;

import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MessageSerializer;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionGoal;
import edu.unc.cs.robotics.ros.msg.actionlib.GoalID;
import junit.framework.TestCase;

public class ReflectionTest extends TestCase {
    static class MyGoal extends Message {
        @Override
        public void serialize(MessageSerializer ser) {
            throw new UnsupportedOperationException();
        }
    }

    static class MyActionGoal extends ActionGoal<MyGoal> {
        public MyActionGoal(Header header, GoalID goalId, MyGoal goal) {
            super(header, goalId, goal);
        }
    }

    public void testFindGenericSuperclass() throws Exception {
        ParameterizedType pType = Reflection.findGenericSuperclass(MyActionGoal.class, ActionGoal.class);
        assertEquals(ActionGoal.class.getName(), pType.getRawType().getTypeName());
    }

    public void testGetGoalClass() throws Exception {
        Class<MyGoal> actual = Reflection.getGoalClass(MyActionGoal.class);

        assertEquals(MyGoal.class.getName(), actual.getTypeName());

    }
}