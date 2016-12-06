package edu.unc.cs.robotics.ros.actionlib;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import edu.unc.cs.robotics.ros.msg.Message;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionFeedback;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionGoal;
import edu.unc.cs.robotics.ros.msg.actionlib.ActionResult;
import edu.unc.cs.robotics.ros.msg.actionlib.GoalID;
import edu.unc.cs.robotics.ros.msg.actionlib.GoalStatus;
import edu.unc.cs.robotics.ros.msg.actionlib.GoalStatusArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.unc.cs.robotics.ros.actionlib.CommState.DONE;
import static edu.unc.cs.robotics.ros.actionlib.CommState.WAITING_FOR_GOAL_ACK;

public // TODO: make not public
class ClientGoalStateMachine<
    Goal extends Message,
    Result extends Message,
    Feedback extends Message>
    implements GoalHandle<Goal, Result>
{
    private static final Logger LOG = LoggerFactory.getLogger(ClientGoalStateMachine.class);

    private final ActionClient<Goal,Result,Feedback> _actionClient;
    private final ActionGoal<Goal> _actionGoal;
    private final GoalFeedbackListener<Goal, Result, Feedback> _feedbackListener;
    private final GoalTransitionListener<Goal, Result> _transitionListener;
    private boolean _active;
    private CommState _state = WAITING_FOR_GOAL_ACK;
    private GoalStatus.Status _latestGoalStatus;
    private Result _latestResult;

    ClientGoalStateMachine(
        ActionClient<Goal, Result, Feedback> actionClient,
        ActionGoal<Goal> actionGoal,
        GoalFeedbackListener<Goal, Result, Feedback> feedbackListener,
        GoalTransitionListener<Goal, Result> transitionListener)
    {
        _actionClient = actionClient;
        _actionGoal = actionGoal;
        _feedbackListener = feedbackListener;
        _transitionListener = transitionListener;
        _active = true;
    }

    @Override
    public Goal getGoal() {
        return _actionGoal.goal;
    }

    @Override
    public boolean isExpired() {
        return !_active;
    }

    @Override
    public CommState getCommState() {
        synchronized (_actionClient._activeGoals) {
            return _state;
        }
    }

    @Override
    public TerminalState getTerminalState() {
        if (!_active) {
            return TerminalState.LOST;
        }

        synchronized (_actionClient._activeGoals) {
            CommState commState = _state;
            if (commState != DONE) {
                LOG.warn("terminal state not available yet, in comm state {}", commState);
            }

            GoalStatus.Status goalStatus = _latestGoalStatus;
            switch (goalStatus) {
            case PENDING:
            case ACTIVE:
            case PREEMPTING:
            case RECALLING:
                LOG.warn("terminal state request while goal status is {}", goalStatus);
                return TerminalState.LOST;
            case PREEMPTED:
                return TerminalState.PREEMPTED;
            case SUCCEEDED:
                return TerminalState.SUCCEEDED;
            case ABORTED:
                return TerminalState.ABORTED;
            case REJECTED:
                return TerminalState.REJECTED;
            case RECALLED:
                return TerminalState.RECALLED;
            case LOST:
                return TerminalState.LOST;
            default:
                throw new AssertionError("unhandled goal status: " + goalStatus);
            }
        }
    }

    @Override
    public Result getResult() {
        if (!_active) {
            LOG.warn("trying to getResult on inactive GoalHandle");
        }

        synchronized (_actionClient._activeGoals) {
            return _latestResult;
        }
    }

    @Override
    public void resend() {
        if (!_active) {
            LOG.warn("trying to resend an inactive GoalHandle");
        }

        synchronized (_actionClient._activeGoals) {
            _actionClient._goalPublisher.resend(_actionGoal);
        }
    }

    @Override
    public void cancel() {
        if (!_active) {
            LOG.error("trying to cancel() inactive goal handle");
        }

        synchronized (_actionClient._activeGoals) {
            CommState commState = _state;
            switch (commState) {
            default:
                throw new AssertionError("unhandled state: " + commState);
            case WAITING_FOR_RESULT:
            case RECALLING:
            case PREEMPTING:
            case DONE:
                LOG.debug("Ignoring cancel request while in comm state {}", commState);
                return;
            case WAITING_FOR_GOAL_ACK:
            case PENDING:
            case ACTIVE:
            case WAITING_FOR_CANCEL_ACK:
                GoalID cancelMsg = new GoalID();
                cancelMsg.stamp = 0;
                cancelMsg.id = _actionGoal.goalId.id;

                _actionClient._cancelPublisher.publish(cancelMsg);

                transitionToState(CommState.WAITING_FOR_CANCEL_ACK);
            }
        }
    }

    @Override
    public void close() {
        if (_active) {
            synchronized (_actionClient._activeGoals) {
                _active = false;
                // TODO: make this O(n) -> O(1)
                // though not sure it will matter much, unless the caller
                // is creating a lot of goals.
                _actionClient._activeGoals.remove(this);
            }
        }
    }

    void updateFeedback(ActionFeedback<Feedback> actionFeedback) {
        if (!Objects.equals(_actionGoal.goalId.id, actionFeedback.status.goalId.id)) {
            return;
        }

        if (_feedbackListener != null) {
            _feedbackListener.feedback(this, actionFeedback.feedback);
        }
    }

    void updateResult(ActionResult<Result> actionResult) {
        if (!Objects.equals(_actionGoal.goalId.id, actionResult.status.goalId.id)) {
            return;
        }

        _latestGoalStatus = actionResult.status.status;
        _latestResult = actionResult.result;

        if (_state == DONE) {
            LOG.error("got result when already in DONE state");
            return;
        }

        updateStatus(actionResult.status);
        transitionToState(DONE);
    }

    void updateStatus(GoalStatusArray statusArray) {
        if (_state == DONE)
            return;

        Optional<GoalStatus> goalStatus = Arrays.stream(statusArray.statusList)
            .filter(s -> Objects.equals(s.goalId.id, _actionGoal.goalId.id)).findAny();

        if (!goalStatus.isPresent()) {
            if (_state != WAITING_FOR_GOAL_ACK &&
                _state != CommState.WAITING_FOR_RESULT &&
                _state != DONE) {
                processLost();
            }
            return;
        }

        updateStatus(goalStatus.get());
    }

    private void updateStatus(GoalStatus status) {
        _latestGoalStatus = status.status;

        switch (_state) {
        case WAITING_FOR_GOAL_ACK:
            switch (_latestGoalStatus) {
            case PENDING:
                transitionToState(CommState.PENDING);
                break;
            case ACTIVE:
                transitionToState(CommState.ACTIVE);
                break;
            case PREEMPTED:
                transitionToState(CommState.ACTIVE);
                transitionToState(CommState.PREEMPTING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case SUCCEEDED:
            case ABORTED:
            case REJECTED:
            case RECALLED:
                transitionToState(CommState.ACTIVE);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case PREEMPTING:
                transitionToState(CommState.ACTIVE);
                transitionToState(CommState.PREEMPTING);
                break;
            case RECALLING:
                transitionToState(CommState.PENDING);
                transitionToState(CommState.RECALLING);
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case PENDING:
            switch (_latestGoalStatus) {
            case PENDING:
                break;
            case ACTIVE:
                transitionToState(CommState.ACTIVE);
                break;
            case PREEMPTED:
                transitionToState(CommState.ACTIVE);
                transitionToState(CommState.PREEMPTING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case SUCCEEDED:
            case ABORTED:
                transitionToState(CommState.ACTIVE);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case REJECTED:
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case RECALLED:
                transitionToState(CommState.RECALLING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case PREEMPTING:
                transitionToState(CommState.ACTIVE);
                transitionToState(CommState.PREEMPTING);
                break;
            case RECALLING:
                transitionToState(CommState.RECALLING);
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case ACTIVE:
            switch (_latestGoalStatus) {
            case PENDING:
            case REJECTED:
            case RECALLING:
            case RECALLED:
                LOG.error("invalid transition from {} to {}", _state, _latestGoalStatus);
                break;
            case ACTIVE:
                break;
            case PREEMPTED:
                transitionToState(CommState.PREEMPTING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case SUCCEEDED:
            case ABORTED:
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case PREEMPTING:
                transitionToState(CommState.PREEMPTING);
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case WAITING_FOR_RESULT:
            switch (_latestGoalStatus) {
            case PENDING:
            case PREEMPTING:
            case RECALLING:
                LOG.error("invalid transition from {} to {}", _state, _latestGoalStatus);
                break;
            case ACTIVE:
            case PREEMPTED:
            case SUCCEEDED:
            case ABORTED:
            case REJECTED:
            case RECALLED:
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case WAITING_FOR_CANCEL_ACK:
            switch (_latestGoalStatus) {
            case PENDING:
                break;
            case ACTIVE:
                break;
            case SUCCEEDED:
            case ABORTED:
            case PREEMPTED:
                transitionToState(CommState.PREEMPTING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case RECALLED:
                transitionToState(CommState.RECALLING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case REJECTED:
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case PREEMPTING:
                transitionToState(CommState.PREEMPTING);
                break;
            case RECALLING:
                transitionToState(CommState.RECALLING);
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case RECALLING:
            switch (_latestGoalStatus) {
            case PENDING:
            case ACTIVE:
                LOG.error("invalid transition from {} to {}", _state, _latestGoalStatus);
                break;
            case SUCCEEDED:
            case ABORTED:
            case PREEMPTED:
                transitionToState(CommState.PREEMPTING);
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case RECALLED:
            case REJECTED:
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case PREEMPTING:
                transitionToState(CommState.PREEMPTING);
                break;
            case RECALLING:
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case PREEMPTING:
            switch (_latestGoalStatus) {
            case PENDING:
            case ACTIVE:
            case REJECTED:
            case RECALLING:
            case RECALLED:
                LOG.error("invalid transition from {} to {}", _state, _latestGoalStatus);
                break;
            case PREEMPTED:
            case SUCCEEDED:
            case ABORTED:
                transitionToState(CommState.WAITING_FOR_RESULT);
                break;
            case PREEMPTING:
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        case DONE:
            switch (_latestGoalStatus) {
            case PENDING:
            case ACTIVE:
            case RECALLING:
            case PREEMPTING:
                LOG.error("invalid transition from {} to {}", _state, _latestGoalStatus);
                break;
            case PREEMPTED:
            case SUCCEEDED:
            case ABORTED:
            case RECALLED:
            case REJECTED:
                break;
            default:
                LOG.error("bug: bad status: " + _latestGoalStatus);
                break;
            }
            break;
        default:
            LOG.error("in bad state: " + _state);
        }
    }

    private void processLost() {
        LOG.warn("goal lost");
        _latestGoalStatus = GoalStatus.Status.LOST;
        transitionToState(DONE);
    }

    private void transitionToState(CommState state) {
        LOG.debug("state transition {} => {}", _state, state);

        _state = state;

        if (_transitionListener != null) {
            _transitionListener.transition(this, state);
        }
    }
}
