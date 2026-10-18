package com.smarttaxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import org.junit.jupiter.api.Test;

import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.config.AppConfig;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.fare.FareCalculator;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

class TrainingServiceTest {

    private static final FareCalculator FARE = new FareCalculator() {
        @Override
        public double calculateFare(int distance, LocalTime pickupTime) {
            return 5 + 2 * distance;
        }

        @Override
        public double calculateCost(int steps) {
            return 0.5 * steps;
        }
    };

    private TrainingService service() throws Exception {
        CityMap map = CityMap.defaultMap();
        GridWorld env = new GridWorld(map, new Random(3));
        QLearningAgent agent = new QLearningAgent(new Random(4), 0.1, 0.99, 1.0, 0.05, 0.999);
        return new TrainingService(env, agent, FARE, new AppConfig(new Properties()));
    }

    @Test
    void trainRunsRequestedEpisodesAndNotifiesListener() throws Exception {
        List<EpisodeResult> seen = new ArrayList<>();
        List<EpisodeResult> results = service().train(50, seen::add, null);
        assertEquals(50, results.size());
        assertEquals(50, seen.size());
        assertEquals(1, results.get(0).episodeNo());
        assertEquals(50, results.get(49).episodeNo());
    }

    @Test
    void cancelStopsTrainingEarly() throws Exception {
        List<EpisodeResult> seen = new ArrayList<>();
        List<EpisodeResult> results = service().train(1000, seen::add, () -> seen.size() >= 10);
        assertEquals(10, results.size());
    }

    @Test
    void episodeNumbersContinueAcrossCalls() throws Exception {
        TrainingService s = service();
        s.train(5, null, null);
        List<EpisodeResult> more = s.train(5, null, null);
        assertEquals(6, more.get(0).episodeNo());
        assertEquals(10, s.getEpisodeCounter());
    }

    @Test
    void profitIsFareMinusCostAndFareOnlyOnSuccess() throws Exception {
        TrainingService s = service();
        List<EpisodeResult> results = s.train(3000, null, null);
        boolean sawSuccess = false;
        for (EpisodeResult r : results) {
            assertEquals(r.fare() - r.cost(), r.profit(), 1e-9);
            assertEquals(0.5 * r.steps(), r.cost(), 1e-9);
            if (r.success()) {
                sawSuccess = true;
                int distance = Math.abs(r.pickup().getRow() - r.destination().getRow())
                        + Math.abs(r.pickup().getCol() - r.destination().getCol());
                assertEquals(5 + 2 * distance, r.fare(), 1e-9);
            } else {
                assertEquals(0.0, r.fare(), 1e-9);
            }
        }
        assertTrue(sawSuccess, "some episodes should succeed");
    }

    @Test
    void greedyRolloutDeliversAfterTraining() throws Exception {
        TrainingService s = service();
        s.train(8000, null, null);
        State start = new State(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4));
        List<Action> path = s.greedyRollout(start);
        assertTrue(path.contains(Action.PICKUP));
        assertEquals(Action.DROPOFF, path.get(path.size() - 1));
        assertTrue(path.size() < 200, "path length was " + path.size());
    }
}
