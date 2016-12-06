package edu.unc.cs.robotics.ros.actionlib;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import edu.unc.cs.robotics.ros.Name;
import edu.unc.cs.robotics.ros.NodeHandle;
import edu.unc.cs.robotics.ros.Publisher;
import edu.unc.cs.robotics.ros.ROSTime;
import edu.unc.cs.robotics.ros.Subscriber;
import edu.unc.cs.robotics.ros.msg.Header;
import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.MetaMessage;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionFeedback;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionGoal;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionResult;
import edu.unc.cs.robotics.ros.msg.actionlib.GoalID;
import edu.unc.cs.robotics.ros.msg.actionlib.GoalStatusArray;
import edu.unc.cs.robotics.ros.msg.control.FollowJointTrajectoryActionFeedback;
import edu.unc.cs.robotics.ros.msg.control.FollowJointTrajectoryActionGoal;
import edu.unc.cs.robotics.ros.msg.control.FollowJointTrajectoryActionResult;
import edu.unc.cs.robotics.ros.msg.control.FollowJointTrajectoryFeedback;
import edu.unc.cs.robotics.ros.msg.control.FollowJointTrajectoryGoal;
import edu.unc.cs.robotics.ros.msg.control.FollowJointTrajectoryResult;
import edu.unc.cs.robotics.ros.topic.PublicationListener;
import edu.unc.cs.robotics.ros.topic.SubscriberLink;
import edu.unc.cs.robotics.ros.topic.SubscriptionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionClient<
    Goal extends Message,
    Result extends Message,
    Feedback extends Message>
    implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(ActionClient.class);

    private final NodeHandle _node;
    private final ExecutorService _executor;

    private final Subscriber<GoalStatusArray> _statusSubscriber;
    private final Subscriber<? extends ActionFeedback<Feedback>> _feedbackSubscriber;
    private final Subscriber<? extends ActionResult<Result>> _resultSubscriber;
    final GoalPublisher<? extends ActionGoal<Goal>> _goalPublisher;

    final Publisher<GoalID> _cancelPublisher;
    private final AtomicInteger _goalIdSequence = new AtomicInteger();

    final List<ClientGoalStateMachine<Goal, Result, Feedback>> _activeGoals = new ArrayList<>();

    public ActionClient(
        NodeHandle node,
        Class<? extends ActionGoal<Goal>> actionGoal,
        Class<? extends ActionResult<Result>> actionResult,
        Class<? extends ActionFeedback<Feedback>> actionFeedback)
    {
        _node = node;

        // TODO: would prefer to use a serial execution queue
        // on a thread pool
        _executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "ActionClient[" + node.name() + "]"));

        _statusSubscriber = _node.subscribe(GoalStatusArray.class, "status", 1, _executor, this::status);
        _feedbackSubscriber = _node.subscribe(actionFeedback, "feedback", 1, _executor, this::feedback);
        _resultSubscriber = _node.subscribe(actionResult, "result", 1, _executor, this::result);

        _goalPublisher = new GoalPublisher<>(actionGoal);

        _cancelPublisher = _node.advertise(GoalID.class, "cancel", 10, false, _executor, new PublicationListener<GoalID>() {
            @Override
            public void connect(SubscriberLink<? extends GoalID> link) {
                cancelConnect(link);
            }

            @Override
            public void disconnect(SubscriberLink<? extends GoalID> link) {
                cancelDisconnect(link);
            }
        });
    }


    public void close() {
        try {
            _statusSubscriber.close();
        } finally {
            try {
                _feedbackSubscriber.close();
            } finally {
                try {
                    _resultSubscriber.close();
                } finally {
                    try {
                        _goalPublisher.close();
                    } finally {
                        try {
                            _cancelPublisher.close();
                        } finally {
                            _executor.shutdown();
                            try {
                                if (!_executor.awaitTermination(1, TimeUnit.MINUTES)) {
                                    LOG.warn("executor did not terminate");
                                }
                            } catch (InterruptedException ex) {
                                LOG.warn("interrupted while waiting for executor to terminate", ex);
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: consider adding an executor as an argument
    public GoalHandle<Goal, Result> sendGoal(
        Goal goal,
        GoalFeedbackListener<Goal, Result, Feedback> feedbackListener,
        GoalTransitionListener<Goal, Result> transitionListener)
    {
        return _goalPublisher.send(goal, feedbackListener, transitionListener);
    }

    private GoalID generateId() {
        GoalID goalId = new GoalID();
        long t = ROSTime.stamp();
        goalId.stamp = t;
        t /= 1_000_000L;
        goalId.id = String.format("%s-%d-%d.%03d",
            _node.name(),
            _goalIdSequence.incrementAndGet(),
            t / 1000,
            t % 1000);
        return goalId;
    }

    public void cancelAllGoals() {
        GoalID cancel = new GoalID();
        cancel.stamp = 0;
        cancel.id = "";
        _cancelPublisher.publish(cancel);
    }

    private void goalConnect(SubscriberLink<? extends ActionGoal<Goal>> link) {
        LOG.debug("goal connect");
    }

    private void goalDisconnect(SubscriberLink<? extends ActionGoal<Goal>> link) {
        LOG.debug("goal disconnect");
    }

    private void cancelConnect(SubscriberLink<? extends GoalID> link) {
        LOG.debug("cancel connect");
    }

    private void cancelDisconnect(SubscriberLink<? extends GoalID> link) {
        LOG.debug("cancel disconnect");
    }


    private void status(GoalStatusArray statusArray) {
        LOG.debug("status: {}", statusArray);
        synchronized (_activeGoals) {
            for (ClientGoalStateMachine<Goal, Result, Feedback> activeGoal : _activeGoals) {
                activeGoal.updateStatus(statusArray);
            }
        }
    }

    private void feedback(ActionFeedback<Feedback> feedback) {
        LOG.debug("feedback: {}", feedback);
        synchronized (_activeGoals) {
            for (ClientGoalStateMachine<Goal, Result, Feedback> activeGoal : _activeGoals) {
                activeGoal.updateFeedback(feedback);
            }
        }
    }

    private void result(ActionResult<Result> result) {
        LOG.debug("result: {}", result);
        synchronized (_activeGoals) {
            for (ClientGoalStateMachine<Goal, Result, Feedback> activeGoal : _activeGoals) {
                activeGoal.updateResult(result);
            }
        }
    }


    public static <G extends Message, R extends Message, F extends Message> ActionClient<G,R,F> create(
        NodeHandle node,
        Class<? extends ActionGoal<G>> actionGoal,
        Class<? extends ActionResult<R>> actionResult,
        Class<? extends ActionFeedback<F>> actionFeedback)
    {
        return new ActionClient<>(node, actionGoal, actionResult, actionFeedback);
    }


    /**
     * This inner class is a thin wrapper for {@code Publisher<? extends ActionGoal<Goal>>}.
     * It allows for two things: it provides the implementation of PublicationListener, and
     * it also allows the generic type association between the actionGoal class and the
     * publication.  This is not possible with the wildcard types on the ActionClass.
     *
     * @param <T>
     */
    class GoalPublisher<T extends ActionGoal<Goal>> implements PublicationListener<T> {
        private final Class<T> _actionGoal;
        private final Publisher<T> _publisher;
        private final Constructor<T> _constructor;

        GoalPublisher(Class<T> actionGoal) {
            _actionGoal = actionGoal;
            _publisher = _node.advertise(actionGoal, "goal", 10, false, _executor, this);

            Class<Goal> goal = Reflection.getGoalClass(actionGoal);
            try {
                _constructor = _actionGoal.getConstructor(Header.class, GoalID.class, goal);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(
                    "Invalid ActionGoal implementation, missing constructor (Header, GoalId, Goal)", e);
            }
        }

        void resend(ActionGoal<Goal> actionGoal) {
            _publisher.publish(_actionGoal.cast(actionGoal));
        }

        ClientGoalStateMachine<Goal, Result, Feedback> send(
            Goal goal,
            GoalFeedbackListener<Goal, Result, Feedback> feedbackListener,
            GoalTransitionListener<Goal, Result> transitionListener) {
            try {
                T actionGoal = _constructor.newInstance(
                    new Header(0, ROSTime.stamp(), ""),
                    generateId(),
                    goal);

                LOG.debug("Sending goal "+actionGoal.goalId);

                ClientGoalStateMachine <Goal, Result, Feedback> handle = new ClientGoalStateMachine<>(
                    ActionClient.this, actionGoal, feedbackListener, transitionListener);

                synchronized (_activeGoals) {
                    _activeGoals.add(handle);
                }

                _publisher.publish(actionGoal);

                return handle;

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        @Override
        public void connect(SubscriberLink<? extends T> link) {
            goalConnect(link);
        }

        @Override
        public void disconnect(SubscriberLink<? extends T> link) {
            goalDisconnect(link);
        }

        void close() {
            _publisher.close();
        }
    }

    public static void main(String[] args) {
        NodeHandle nodeHandle = new NodeHandle() {
            @Override
            public Name name() {
                return null;
            }

            @Override
            public <M extends Message> Publisher<M> advertise(
                MetaMessage<M> meta,
                String topic,
                int queueSize,
                boolean latch,
                Executor executor,
                PublicationListener<? super M> listener) {
                return null;
            }

            @Override
            public <M extends Message> Subscriber<M> subscribe(
                MetaMessage<M> meta, String topic, int queueSize, Executor executor, SubscriptionListener<? super M> listener) {
                return null;
            }
        };

        String nodeName = "/arm_controller/follow_joint_trajectory";

        ActionClient<FollowJointTrajectoryGoal,FollowJointTrajectoryResult,FollowJointTrajectoryFeedback> actionClient =
            ActionClient.create(
                nodeHandle,
                FollowJointTrajectoryActionGoal.class,
                FollowJointTrajectoryActionResult.class,
                FollowJointTrajectoryActionFeedback.class);
    }
}
