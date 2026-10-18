package com.smarttaxi.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.smarttaxi.exception.DataAccessException;

/** Database access for the training_run table, including the transaction that saves a run with its trips. */
public class TrainingRunDao implements Dao<TrainingRun, Integer> {

    private static final String INSERT = "INSERT INTO training_run (started_at, agent_type, episodes, alpha, gamma, "
            + "epsilon_start, epsilon_end, avg_reward, success_rate, total_profit, map_data, notes) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final Set<String> SORT_COLUMNS = Set.of("avg_reward", "success_rate", "total_profit");

    private final DatabaseManager db;

    public TrainingRunDao(DatabaseManager db) {
        this.db = db;
    }

    private static void bind(PreparedStatement ps, TrainingRun r) throws SQLException {
        ps.setString(1, r.startedAt().toString());
        ps.setString(2, r.agentType());
        ps.setInt(3, r.episodes());
        ps.setDouble(4, r.alpha());
        ps.setDouble(5, r.gamma());
        ps.setDouble(6, r.epsilonStart());
        ps.setDouble(7, r.epsilonEnd());
        ps.setDouble(8, r.avgReward());
        ps.setDouble(9, r.successRate());
        ps.setDouble(10, r.totalProfit());
        ps.setString(11, r.mapData());
        ps.setString(12, r.notes());
    }

    private static TrainingRun map(ResultSet rs) throws SQLException {
        return new TrainingRun(rs.getInt("id"), LocalDateTime.parse(rs.getString("started_at")),
                rs.getString("agent_type"), rs.getInt("episodes"), rs.getDouble("alpha"), rs.getDouble("gamma"),
                rs.getDouble("epsilon_start"), rs.getDouble("epsilon_end"), rs.getDouble("avg_reward"),
                rs.getDouble("success_rate"), rs.getDouble("total_profit"), rs.getString("map_data"),
                rs.getString("notes"));
    }

    @Override
    public void insert(TrainingRun run) throws DataAccessException {
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(INSERT)) {
            bind(ps, run);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert the training run", e);
        }
    }

    /**
     * Saves a run and all its trips in ONE transaction: either everything is stored or nothing is.
     * Returns the id given to the new run.
     */
    public int saveRunWithTrips(TrainingRun run, List<Trip> trips) throws DataAccessException {
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try {
                int runId;
                try (PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, run);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No id was generated for the training run");
                        }
                        runId = keys.getInt(1);
                    }
                }
                try (PreparedStatement tp = con.prepareStatement(TripDao.INSERT)) {
                    for (Trip t : trips) {
                        TripDao.bind(tp, t, runId);
                        tp.addBatch();
                    }
                    tp.executeBatch();
                }
                con.commit();
                return runId;
            } catch (SQLException e) {
                con.rollback();
                throw new DataAccessException("Could not save the run; nothing was stored", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error while saving the run", e);
        }
    }

    @Override
    public Optional<TrainingRun> findById(Integer id) throws DataAccessException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM training_run WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read run " + id, e);
        }
    }

    @Override
    public List<TrainingRun> findAll() throws DataAccessException {
        return list("SELECT * FROM training_run ORDER BY id DESC");
    }

    /** The best runs first. orderByColumn must be avg_reward, success_rate or total_profit. */
    public List<TrainingRun> findTop(int limit, String orderByColumn) throws DataAccessException {
        if (!SORT_COLUMNS.contains(orderByColumn)) {
            throw new IllegalArgumentException("Cannot sort by " + orderByColumn);
        }
        return list("SELECT * FROM training_run ORDER BY " + orderByColumn + " DESC LIMIT " + Math.max(1, limit));
    }

    private List<TrainingRun> list(String sql) throws DataAccessException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<TrainingRun> runs = new ArrayList<>();
            while (rs.next()) {
                runs.add(map(rs));
            }
            return runs;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the training runs", e);
        }
    }

    @Override
    public void update(TrainingRun r) throws DataAccessException {
        String sql = "UPDATE training_run SET started_at=?, agent_type=?, episodes=?, alpha=?, gamma=?, "
                + "epsilon_start=?, epsilon_end=?, avg_reward=?, success_rate=?, total_profit=?, map_data=?, "
                + "notes=? WHERE id=?";
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, r);
            ps.setInt(13, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update run " + r.id(), e);
        }
    }

    public void updateNotes(int runId, String notes) throws DataAccessException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE training_run SET notes = ? WHERE id = ?")) {
            ps.setString(1, notes);
            ps.setInt(2, runId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the notes of run " + runId, e);
        }
    }

    /** Deletes a run and its trips in one transaction. */
    @Override
    public void delete(Integer id) throws DataAccessException {
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement trips = con.prepareStatement("DELETE FROM trip WHERE run_id = ?");
                 PreparedStatement run = con.prepareStatement("DELETE FROM training_run WHERE id = ?")) {
                trips.setInt(1, id);
                trips.executeUpdate();
                run.setInt(1, id);
                run.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw new DataAccessException("Could not delete run " + id, e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error while deleting run " + id, e);
        }
    }
}
