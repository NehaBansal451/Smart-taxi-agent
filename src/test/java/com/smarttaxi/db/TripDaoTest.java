package com.smarttaxi.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.smarttaxi.exception.DataAccessException;
import com.smarttaxi.model.Action;

/** Tests the CRUD methods of {@link TripDao} on a temporary database file. */
class TripDaoTest {

    @TempDir
    Path tempDir;

    private TripDao tripDao;
    private int runId;

    @BeforeEach
    void setUp() throws DataAccessException {
        String url = "jdbc:sqlite:" + tempDir.resolve("trip-test.db").toString().replace('\\', '/');
        DatabaseManager db = new DatabaseManager(url);
        tripDao = new TripDao(db);
        TrainingRun run = new TrainingRun(0, LocalDateTime.of(2026, 10, 18, 12, 0), "QLearning", 100,
                0.1, 0.99, 1.0, 0.05, 5.0, 0.9, 12.5, "SIZE,5", "test run");
        runId = new TrainingRunDao(db).saveRunWithTrips(run, List.of());
    }

    private Trip sampleTrip(int episodeNo) {
        return new Trip(0, runId, episodeNo, 9.0, 11, true, 25.0, 5.5, 19.5,
                "0,0", "0,4", "4,4", "SOUTH,EAST,PICKUP");
    }

    @Test
    void insertedTripCanBeReadBackById() throws DataAccessException {
        tripDao.insert(sampleTrip(10));

        Trip saved = tripDao.findByRunId(runId).get(0);

        assertTrue(saved.id() > 0);
        assertEquals(10, saved.episodeNo());
        assertEquals(19.5, saved.profit());
        assertEquals(saved, tripDao.findById(saved.id()).orElseThrow());
    }

    @Test
    void findByIdOfMissingTripIsEmpty() throws DataAccessException {
        assertTrue(tripDao.findById(9999).isEmpty());
    }

    @Test
    void tripsOfARunAreOrderedByEpisode() throws DataAccessException {
        tripDao.insert(sampleTrip(30));
        tripDao.insert(sampleTrip(10));
        tripDao.insert(sampleTrip(20));

        List<Trip> trips = tripDao.findByRunId(runId);

        assertEquals(List.of(10, 20, 30), trips.stream().map(Trip::episodeNo).toList());
    }

    @Test
    void updateChangesTheStoredTrip() throws DataAccessException {
        tripDao.insert(sampleTrip(10));
        Trip saved = tripDao.findByRunId(runId).get(0);

        Trip changed = new Trip(saved.id(), runId, 10, -3.0, 40, false, 0.0, 20.0, -20.0,
                "1,1", "0,0", "4,0", "NORTH");
        tripDao.update(changed);

        assertEquals(changed, tripDao.findById(saved.id()).orElseThrow());
    }

    @Test
    void deleteRemovesOnlyThatTrip() throws DataAccessException {
        tripDao.insert(sampleTrip(10));
        tripDao.insert(sampleTrip(20));
        Trip first = tripDao.findByRunId(runId).get(0);

        tripDao.delete(first.id());

        List<Trip> left = tripDao.findByRunId(runId);
        assertEquals(1, left.size());
        assertEquals(20, left.get(0).episodeNo());
    }

    @Test
    void storedPathAndPositionsCanBeParsedForReplay() throws DataAccessException {
        tripDao.insert(sampleTrip(10));
        Trip saved = tripDao.findByRunId(runId).get(0);

        assertEquals(List.of(Action.SOUTH, Action.EAST, Action.PICKUP), saved.actionList());
        assertEquals(0, Trip.parsePosition(saved.pickup()).getRow());
        assertEquals(4, Trip.parsePosition(saved.pickup()).getCol());
    }
}