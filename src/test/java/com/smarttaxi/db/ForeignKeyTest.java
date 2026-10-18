package com.smarttaxi.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.smarttaxi.exception.DataAccessException;

/** Tests that SQLite enforces foreign keys on every connection (section 7.2 of the blueprint). */
class ForeignKeyTest {

    @TempDir
    Path tempDir;

    private DatabaseManager db;
    private TripDao tripDao;

    @BeforeEach
    void setUp() throws DataAccessException {
        String url = "jdbc:sqlite:" + tempDir.resolve("fk-test.db").toString().replace('\\', '/');
        db = new DatabaseManager(url);
        tripDao = new TripDao(db);
    }

    private int saveRunWithTwoTrips() throws DataAccessException {
        TrainingRun run = new TrainingRun(0, LocalDateTime.of(2026, 10, 18, 12, 0), "QLearning", 100,
                0.1, 0.99, 1.0, 0.05, 5.0, 0.9, 12.5, "SIZE,5", null);
        Trip first = new Trip(0, 0, 10, 9.0, 11, true, 25.0, 5.5, 19.5, "0,0", "0,4", "4,4", "SOUTH,EAST");
        Trip second = new Trip(0, 0, 20, 7.0, 13, true, 25.0, 6.5, 18.5, "1,1", "0,0", "4,0", "NORTH,WEST");
        return new TrainingRunDao(db).saveRunWithTrips(run, List.of(first, second));
    }

    @Test
    void foreignKeysAreSwitchedOnForEveryConnection() throws Exception {
        try (Connection con = db.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void deletingARunDirectlyCascadesToItsTrips() throws Exception {
        int runId = saveRunWithTwoTrips();
        assertEquals(2, tripDao.findByRunId(runId).size());

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM training_run WHERE id = ?")) {
            ps.setInt(1, runId);
            ps.executeUpdate();
        }

        assertTrue(tripDao.findByRunId(runId).isEmpty());
    }

    @Test
    void tripForAMissingRunIsRejected() {
        Trip orphan = new Trip(0, 9999, 10, 9.0, 11, true, 25.0, 5.5, 19.5, "0,0", "0,4", "4,4", "SOUTH");

        assertThrows(DataAccessException.class, () -> tripDao.insert(orphan));
    }
}