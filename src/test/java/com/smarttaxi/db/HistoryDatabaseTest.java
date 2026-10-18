package com.smarttaxi.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.smarttaxi.exception.DataAccessException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.service.EpisodeResult;
import com.smarttaxi.service.HistoryService;

class HistoryDatabaseTest {

    private static EpisodeResult episode(int no, double reward, boolean success) {
        return new EpisodeResult(no, reward, 3, 0.5, success, success ? 13.0 : 0.0, 1.5, success ? 11.5 : -1.5,
                new Position(0, 0), new Position(0, 4), new Position(4, 4),
                List.of(Action.EAST, Action.PICKUP, Action.DROPOFF));
    }

    private static TrainingRun run(double avgReward) {
        return new TrainingRun(0, LocalDateTime.of(2026, 10, 18, 12, 0), "Q-learning", 10, 0.1, 0.99, 1.0, 0.05,
                avgReward, 0.5, 10.0, "SIZE,5\nSTOP,0,0\nSTOP,4,4\n", "first");
    }

    private static Trip trip(String actions) {
        return new Trip(0, 0, 1, 5.0, 3, true, 13.0, 1.5, 11.5, "0,0", "0,4", "4,4", actions);
    }

    /** Runs the body with a fresh temporary database file. */
    private interface Body {
        void run(DatabaseManager db) throws Exception;
    }

    private static void withDatabase(Body body) throws Exception {
        Path file = Files.createTempFile("taxi-test", ".db");
        try {
            body.run(new DatabaseManager("jdbc:sqlite:" + file.toString().replace('\\', '/')));
        } finally {
            try {
                Files.deleteIfExists(file);
            } catch (Exception ignored) {
                // a locked temp file on Windows is not a test failure
            }
        }
    }

    @Test
    void runAndTripsAreSavedAndReadBack() throws Exception {
        withDatabase(db -> {
            TrainingRunDao runs = new TrainingRunDao(db);
            TripDao trips = new TripDao(db);
            int id = runs.saveRunWithTrips(run(5.0), List.of(trip("EAST,PICKUP,DROPOFF"), trip("SOUTH")));
            assertTrue(id > 0);
            assertEquals("first", runs.findById(id).orElseThrow().notes());
            assertEquals(2, trips.findByRunId(id).size());
            assertEquals(List.of(Action.EAST, Action.PICKUP, Action.DROPOFF), trips.findByRunId(id).get(0).actionList());
        });
    }

    @Test
    void failedSaveLeavesNothingBehind() throws Exception {
        withDatabase(db -> {
            TrainingRunDao runs = new TrainingRunDao(db);
            assertThrows(DataAccessException.class, () -> runs.saveRunWithTrips(run(5.0), List.of(trip(null))));
            assertEquals(0, runs.findAll().size());
            assertEquals(0, new TripDao(db).findAll().size());
        });
    }

    @Test
    void deleteRemovesTheRunAndItsTrips() throws Exception {
        withDatabase(db -> {
            TrainingRunDao runs = new TrainingRunDao(db);
            TripDao trips = new TripDao(db);
            int id = runs.saveRunWithTrips(run(5.0), List.of(trip("EAST")));
            runs.delete(id);
            assertTrue(runs.findById(id).isEmpty());
            assertEquals(0, trips.findByRunId(id).size());
        });
    }

    @Test
    void notesCanBeUpdated() throws Exception {
        withDatabase(db -> {
            TrainingRunDao runs = new TrainingRunDao(db);
            int id = runs.saveRunWithTrips(run(5.0), List.of());
            runs.updateNotes(id, "best so far");
            assertEquals("best so far", runs.findById(id).orElseThrow().notes());
        });
    }

    @Test
    void leaderboardIsSortedAndRejectsUnknownColumns() throws Exception {
        withDatabase(db -> {
            TrainingRunDao runs = new TrainingRunDao(db);
            runs.saveRunWithTrips(run(1.0), List.of());
            runs.saveRunWithTrips(run(9.0), List.of());
            runs.saveRunWithTrips(run(4.0), List.of());
            List<TrainingRun> top = runs.findTop(2, "avg_reward");
            assertEquals(2, top.size());
            assertEquals(9.0, top.get(0).avgReward(), 1e-9);
            assertEquals(4.0, top.get(1).avgReward(), 1e-9);
            assertThrows(IllegalArgumentException.class, () -> runs.findTop(2, "id; DROP TABLE trip"));
        });
    }

    @Test
    void historyServiceSummarisesAndSamplesEpisodes() throws Exception {
        withDatabase(db -> {
            HistoryService history = new HistoryService(new TrainingRunDao(db), new TripDao(db), 50);
            List<EpisodeResult> results = new ArrayList<>();
            for (int i = 1; i <= 120; i++) {
                results.add(episode(i, i <= 20 ? -100.0 : 10.0, i > 20));
            }
            int id = history.saveRun("Q-learning", results, 0.1, 0.99, 1.0, 0.05, CityMap.defaultMap(), "demo");
            TrainingRun saved = history.getRun(id).orElseThrow();
            assertEquals(120, saved.episodes());
            assertEquals(10.0, saved.avgReward(), 1e-9);
            assertEquals(1.0, saved.successRate(), 1e-9);
            // episodes 50 and 100 are sampled, plus the last 20 (101..120)
            assertEquals(22, history.getTrips(id).size());
            history.deleteRun(id);
            assertEquals(0, history.getRuns().size());
        });
    }
}
