package com.smarttaxi.agent;

import com.smarttaxi.env.Environment;
import com.smarttaxi.env.StepResult;
import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

/** Measures how good an agent is: runs episodes with no learning and, for Q-learning, no exploration. */
public final class Evaluator {

    private Evaluator() {
    }

    public static EvaluationSummary evaluate(Environment env, Agent agent, int episodes)
            throws InvalidMoveException {
        QLearningAgent learner = agent instanceof QLearningAgent ? (QLearningAgent) agent : null;
        if (learner != null) {
            learner.setGreedyMode(true);
        }
        try {
            double totalReward = 0;
            long totalSteps = 0;
            int successes = 0;
            for (int i = 0; i < episodes; i++) {
                State state = env.reset();
                boolean done = false;
                while (!done) {
                    Action action = agent.chooseAction(state);
                    StepResult result = env.step(action);
                    totalReward += result.getReward();
                    state = result.getNextState();
                    done = result.isDone();
                    if (done && result.isSuccess()) {
                        successes++;
                    }
                }
                totalSteps += env.getStepCount();
            }
            return new EvaluationSummary(agent.getName(), episodes, totalReward / episodes,
                    (double) totalSteps / episodes, (double) successes / episodes);
        } finally {
            if (learner != null) {
                learner.setGreedyMode(false);
            }
        }
    }
}
