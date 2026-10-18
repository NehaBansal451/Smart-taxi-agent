package com.smarttaxi.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import com.smarttaxi.agent.Agent;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.config.AppConfig;
import com.smarttaxi.config.LoggerUtil;
import com.smarttaxi.env.Environment;
import com.smarttaxi.env.StepResult;
import com.smarttaxi.exception.InvalidConfigException;
import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.fare.FareCalculator;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

/** Runs training episodes; the bridge between the core and the UI and database. */
public class TrainingService {

    private static final Logger LOG = LoggerUtil.getLogger(TrainingService.class);
    private static final int LOG_EVERY = 500;
    private static final double PROFIT_REWARD_SCALE = 0.1;

    private final Environment env;
    private final Agent agent;
    private final FareCalculator fare;
    private final boolean fareInReward;
    private final Random random;
    private int episodeCounter;

    public TrainingService(Environment env, Agent agent, FareCalculator fare, AppConfig config)
            throws InvalidConfigException {
        this.env = env;
        this.agent = agent;
        this.fare = fare;
        this.fareInReward = config.isFareUsedInReward();
        this.random = config.getSeed().map(Random::new).orElseGet(Random::new);
    }

    /** Runs one episode with learning switched on and returns what happened. */
    public EpisodeResult runEpisode(int episodeNo) throws InvalidMoveException {
        State state = env.reset();
        Position startTaxi = state.getTaxi();
        Position pickup = state.getPassenger().orElse(null);
        Position destination = state.getDestination();
        LocalTime pickupTime = LocalTime.of(random.nextInt(24), random.nextInt(60));
        double epsilon = agent.getEpsilon();

        List<Action> actions = new ArrayList<>();
        double totalReward = 0;
        boolean success = false;
        double tripFare = 0;
        double cost = 0;
        double profit = 0;

        boolean done = false;
        while (!done) {
            Action action = agent.chooseAction(state);
            StepResult result = env.step(action);
            double reward = result.getReward();
            done = result.isDone();
            if (done) {
                success = result.isSuccess();
                if (success && pickup != null) {
                    tripFare = fare.calculateFare(manhattan(pickup, destination), pickupTime);
                }
                cost = fare.calculateCost(env.getStepCount());
                profit = tripFare - cost;
                if (fareInReward) {
                    reward += profit * PROFIT_REWARD_SCALE;
                }
            }
            agent.learn(state, action, reward, result.getNextState(), done);
            actions.add(action);
            totalReward += reward;
            state = result.getNextState();
        }
        agent.endEpisode();
        return new EpisodeResult(episodeNo, totalReward, env.getStepCount(), epsilon, success,
                tripFare, cost, profit, startTaxi, pickup, destination, actions);
    }

    /**
     * Trains for the given number of episodes. The listener (may be null) is called after each one;
     * cancelled (may be null) is polled between episodes. Episode numbers continue across calls.
     */
    public List<EpisodeResult> train(int episodes, EpisodeListener listener, BooleanSupplier cancelled)
            throws InvalidMoveException {
        LOG.info("Training started: " + episodes + " episodes, agent " + agent.getName());
        List<EpisodeResult> results = new ArrayList<>();
        for (int i = 0; i < episodes; i++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                LOG.info("Training cancelled after " + i + " episodes");
                break;
            }
            episodeCounter++;
            EpisodeResult result = runEpisode(episodeCounter);
            results.add(result);
            if (listener != null) {
                listener.onEpisode(result);
            }
            if (episodeCounter % LOG_EVERY == 0) {
                LOG.info("Episode " + episodeCounter + ": reward " + result.totalReward()
                        + ", epsilon " + result.epsilon());
            }
        }
        LOG.info("Training finished: " + results.size() + " episodes run");
        return results;
    }

    /** Plays one trip from the given state with no learning and no exploration. */
    public List<Action> greedyRollout(State start) throws InvalidMoveException {
        QLearningAgent learner = agent instanceof QLearningAgent ? (QLearningAgent) agent : null;
        if (learner != null) {
            learner.setGreedyMode(true);
        }
        try {
            List<Action> actions = new ArrayList<>();
            State state = env.reset(start);
            boolean done = false;
            while (!done) {
                Action action = agent.chooseAction(state);
                StepResult result = env.step(action);
                actions.add(action);
                state = result.getNextState();
                done = result.isDone();
            }
            return actions;
        } finally {
            if (learner != null) {
                learner.setGreedyMode(false);
            }
        }
    }

    /** Number of episodes trained so far (used to continue numbering after Pause/Resume). */
    public int getEpisodeCounter() {
        return episodeCounter;
    }

    private static int manhattan(Position a, Position b) {
        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getCol() - b.getCol());
    }
}
