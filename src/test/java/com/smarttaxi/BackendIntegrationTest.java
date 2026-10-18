package com.smarttaxi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Properties;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.smarttaxi.agent.EvaluationSummary;
import com.smarttaxi.agent.Evaluator;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.agent.RandomAgent;
import com.smarttaxi.config.AppConfig;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.fare.FareCalculator;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.service.TrainingService;
import com.smarttaxi.storage.AgentSnapshot;
import com.smarttaxi.storage.AgentStorage;

class BackendIntegrationTest {

    @Test
    void trainSaveLoadAndEvaluateBeatsRandom(@TempDir Path dir) throws Exception {
        CityMap map = CityMap.defaultMap();
        QLearningAgent agent = new QLearningAgent(new Random(6), 0.1, 0.99, 1.0, 0.05, 0.999);
        FareCalculator fare = new FareCalculator() {
            public double calculateFare(int distance, LocalTime time) { return 5 + 2 * distance; }
            public double calculateCost(int steps) { return 0.5 * steps; }
        };
        TrainingService service = new TrainingService(new GridWorld(map, new Random(5)), agent, fare, new AppConfig(new Properties()));
        service.train(6000, null, null);
        Path file = dir.resolve("agent.ser");
        AgentStorage.save(new AgentSnapshot(agent.getQTable(), agent.getAlpha(), agent.getGamma(), agent.getEpsilon(), map.getSize()), file);
        AgentSnapshot loaded = AgentStorage.load(file, map.getSize());
        QLearningAgent restored = new QLearningAgent(new Random(7), loaded.getQTable(), loaded.getAlpha(), loaded.getGamma(), loaded.getEpsilon(), 0.05, 0.999);
        EvaluationSummary learned = Evaluator.evaluate(new GridWorld(map, new Random(8)), restored, 200);
        EvaluationSummary random = Evaluator.evaluate(new GridWorld(map, new Random(8)), new RandomAgent(new Random(9)), 200);
        System.out.println("Q-learning: " + learned);
        System.out.println("Random:     " + random);
        assertTrue(learned.successRate() >= 0.9, "success rate was " + learned.successRate());
        assertTrue(learned.averageReward() > random.averageReward());
    }
}
