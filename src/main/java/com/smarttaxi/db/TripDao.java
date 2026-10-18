package com.smarttaxi.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.smarttaxi.exception.DataAccessException;

/** Database access for the trip table. */
public class TripDao implements Dao<Trip, Integer> {

    static final String INSERT = "INSERT INTO trip (run_id, episode_no, total_reward, steps, success, fare, cost, "
            + "profit, start_taxi, pickup, destination, actions) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String SELECT = "SELECT * FROM trip";

    private final DatabaseManager db;

    public TripDao(DatabaseManager db) {
        this.db = db;
    }

    /** Fills the insert statement; shared with the transaction in TrainingRunDao. */
    static void bind(PreparedStatement ps, Trip t, int runId) throws SQLException {
        ps.setInt(1, runId);
        ps.setInt(2, t.episodeNo());
        ps.setDouble(3, t.totalReward());
        ps.setInt(4, t.steps());
        ps.setInt(5, t.success() ? 1 : 0);
        ps.setDouble(6, t.fare());
        ps.setDouble(7, t.cost());
        ps.setDouble(8, t.profit());
        ps.setString(9, t.startTaxi());
        ps.setString(10, t.pickup());
        ps.setString(11, t.destination());
        ps.setString(12, t.actions());
    }

    private static Trip map(ResultSet rs) throws SQLException {
        return new Trip(rs.getInt("id"), rs.getInt("run_id"), rs.getInt("episode_no"),
                rs.getDouble("total_reward"), rs.getInt("steps"), rs.getInt("success") == 1,
                rs.getDouble("fare"), rs.getDouble("cost"), rs.getDouble("profit"),
                rs.getString("start_taxi"), rs.getString("pickup"), rs.getString("destination"),
                rs.getString("actions"));
    }

    @Override
    public void insert(Trip trip) throws DataAccessException {
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(INSERT)) {
            bind(ps, trip, trip.runId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert the trip", e);
        }
    }

    @Override
    public Optional<Trip> findById(Integer id) throws DataAccessException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read trip " + id, e);
        }
    }

    @Override
    public List<Trip> findAll() throws DataAccessException {
        return query(SELECT + " ORDER BY id", null);
    }

    public List<Trip> findByRunId(int runId) throws DataAccessException {
        return query(SELECT + " WHERE run_id = ? ORDER BY episode_no", runId);
    }

    private List<Trip> query(String sql, Integer param) throws DataAccessException {
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            List<Trip> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read trips", e);
        }
    }

    @Override
    public void update(Trip t) throws DataAccessException {
        String sql = "UPDATE trip SET run_id=?, episode_no=?, total_reward=?, steps=?, success=?, fare=?, cost=?, "
                + "profit=?, start_taxi=?, pickup=?, destination=?, actions=? WHERE id=?";
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, t, t.runId());
            ps.setInt(13, t.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update trip " + t.id(), e);
        }
    }

    @Override
    public void delete(Integer id) throws DataAccessException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM trip WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete trip " + id, e);
        }
    }
}
