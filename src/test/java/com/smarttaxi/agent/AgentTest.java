package com.smarttaxi.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

import com.smarttaxi.env.GridWorld;
import com.smarttaxi.env.StepResult;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

class AgentTest {

    private static State state(int taxiRow, int taxiCol) {
        return new State(new Position(taxiRow, taxiCol), Optional.of(new Position(0, 4)), new Position(4, 4));
    }

    @Test
    void updateFollowsTheQLearningFormula() {
        QLearningAgent agent = new QLearningAgent(new Random(1), 0.5, 0.9, 1.0, 0.05, 0.999);
        State s = state(0, 0);
        State next = state(1, 0);
        agent.getQTable().setQ(next, Action.SOUTH, 10.0);
        agent.learn(s, Action.SOUTH, -1.0, next, false);
        assertEquals(4.0, agent.getQTable().getQ(s, Action.SOUTH), 1e-9);
        agent.learn(s, Action.EAST, 20.0, next, true);
        assertEquals(10.0, agent.getQTable().getQ(s, Action.EAST), 1e-9);
    }

    @Test
    void epsilonDecaysToItsFloor() {
        QLearningAgent agent = new QLearningAgent(new Random(1), 0.1, 0.9, 1.0, 0.2, 0.5);
        agent.endEpisode();
        assertEquals(0.5, agent.getEpsilon(), 1e-9);
        agent.endEpisode();
        assertEquals(0.25, agent.getEpsilon(), 1e-9);
        agent.endEpisode();
        assertEquals(0.2, agent.getEpsilon(), 1e-9);
        agent.endEpisode();
        assertEquals(0.2, agent.getEpsilon(), 1e-9);
    }

    @Test
    void greedyModeAlwaysPicksTheBestAction() {
        QLearningAgent agent = new QLearningAgent(new Random(1), 0.1, 0.9, 1.0, 0.05, 0.999);
        State s = state(0, 0);
        agent.getQTable().setQ(s, Action.EAST, 5.0);
        agent.setGreedyMode(true);
        for (int i = 0; i < 50; i++) {
            assertEquals(Action.EAST, agent.chooseAction(s));
        }
    }

    @Test
    void randomAgentDoesNotExplore() {
        assertEquals(0.0, new RandomAgent(new Random(1)).getEpsilon(), 1e-9);
    }

    @Test
    void trainedAgentSolvesTheDefaultMapAndBeatsRandom() throws Exception {
        CityMap map = CityMap.defaultMap();
        GridWorld env = new GridWorld(map, new Random(11));
        QLearningAgent agent = new QLearningAgent(new Random(12), 0.1, 0.99, 1.0, 0.05, 0.999);
        for (int ep = 0; ep < 6000; ep++) {
            State s = env.reset();
            boolean done = false;
            while (!done) {
                Action a = agent.chooseAction(s);
                StepResult r = env.step(a);
                agent.learn(s, a, r.getReward(), r.getNextState(), r.isDone());
                s = r.getNextState();
                done = r.isDone();
            }
            agent.endEpisode();
        }
        EvaluationSummary learned = Evaluator.evaluate(new GridWorld(map, new Random(21)), agent, 200);
        EvaluationSummary random = Evaluator.evaluate(new GridWorld(map, new Random(21)), new RandomAgent(new Random(22)), 200);
        assertTrue(learned.successRate() >= 0.9, "success rate was " + learned.successRate());
        assertTrue(learned.averageReward() > random.averageReward(), "learned must beat random");
    }
}
