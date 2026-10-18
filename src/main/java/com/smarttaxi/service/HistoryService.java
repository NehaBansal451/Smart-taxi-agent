package com.smarttaxi.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.smarttaxi.db.TrainingRun;
import com.smarttaxi.db.TrainingRunDao;
import com.smarttaxi.db.Trip;
import com.smarttaxi.db.TripDao;
import com.smarttaxi.exception.DataAccessException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.map.MapFileHandler;

/** Turns finished training results into stored history and reads it back for the screens. */
public class HistoryService {

    private static final int SUMMARY_WINDOW = 100;
    private static final int ALWAYS_KEEP_LAST = 20;

    private final TrainingRunDao runDao;
    private final TripDao tripDao;
    private final int sampleEvery;

    /** sampleEvery: every Nth episode is stored as a trip, plus the last 20 episodes. */
    public HistoryService(TrainingRunDao runDao, TripDao tripDao, int sampleEvery) {
        this.runDao = runDao;
        this.tripDao = tripDao;
        this.sampleEvery = Math.max(1, sampleEvery);
    }

    /** Saves a finished training session. Returns the id of the new run. */
    public int saveRun(String agentType, List<EpisodeResult> results, double alpha, double gamma,
                       double epsilonStart, double epsilonEnd, CityMap map, String notes)
            throws DataAccessException {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("There are no episodes to save");
        }
        int n = results.size();
        List<EpisodeResult> window = results.subList(Math.max(0, n - SUMMARY_WINDOW), n);
        double rewardSum = 0;
        int successes = 0;
        for (EpisodeResult r : window) {
            rewardSum += r.totalReward();
            if (r.success()) {
                successes++;
            }
        }
        double totalProfit = 0;
        for (EpisodeResult r : results) {
            totalProfit += r.profit();
        }
        TrainingRun run = new TrainingRun(0, LocalDateTime.now().withNano(0), agentType, n, alpha, gamma,
                epsilonStart, epsilonEnd, rewardSum / window.size(), (double) successes / window.size(),
                totalProfit, MapFileHandler.toText(map), notes);

        List<Trip> trips = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            EpisodeResult r = results.get(i);
            if (r.episodeNo() % sampleEvery == 0 || i >= n - ALWAYS_KEEP_LAST) {
                trips.add(Trip.fromEpisode(0, r));
            }
        }
        return runDao.saveRunWithTrips(run, trips);
    }

    public List<TrainingRun> getRuns() throws DataAccessException {
        return runDao.findAll();
    }

    /** column: avg_reward, success_rate or total_profit. */
    public List<TrainingRun> getLeaderboard(String column, int limit) throws DataAccessException {
        return runDao.findTop(limit, column);
    }

    public Optional<TrainingRun> getRun(int runId) throws DataAccessException {
        return runDao.findById(runId);
    }

    public List<Trip> getTrips(int runId) throws DataAccessException {
        return tripDao.findByRunId(runId);
    }

    public void updateNotes(int runId, String notes) throws DataAccessException {
        runDao.updateNotes(runId, notes);
    }

    public void deleteRun(int runId) throws DataAccessException {
        runDao.delete(runId);
    }
}
