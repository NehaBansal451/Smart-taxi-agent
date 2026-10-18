package com.smarttaxi.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import com.smarttaxi.exception.DataAccessException;

/** Opens SQLite connections and creates the tables on first use. */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private static final String FILE_PREFIX = "jdbc:sqlite:";

    private static final String CREATE_RUN = """
            CREATE TABLE IF NOT EXISTS training_run (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at    TEXT    NOT NULL,
                agent_type    TEXT    NOT NULL,
                episodes      INTEGER NOT NULL,
                alpha         REAL,
                gamma         REAL,
                epsilon_start REAL,
                epsilon_end   REAL,
                avg_reward    REAL    NOT NULL,
                success_rate  REAL    NOT NULL,
                total_profit  REAL    NOT NULL,
                map_data      TEXT    NOT NULL,
                notes         TEXT
            )""";

    private static final String CREATE_TRIP = """
            CREATE TABLE IF NOT EXISTS trip (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                run_id       INTEGER NOT NULL REFERENCES training_run(id) ON DELETE CASCADE,
                episode_no   INTEGER NOT NULL,
                total_reward REAL    NOT NULL,
                steps        INTEGER NOT NULL,
                success      INTEGER NOT NULL,
                fare         REAL,
                cost         REAL,
                profit       REAL,
                start_taxi   TEXT    NOT NULL,
                pickup       TEXT    NOT NULL,
                destination  TEXT    NOT NULL,
                actions      TEXT    NOT NULL
            )""";

    private final String url;

    /** url is a JDBC url such as jdbc:sqlite:data/taxi.db; the folder is created if it does not exist. */
    public DatabaseManager(String url) throws DataAccessException {
        this.url = url;
        createParentFolder();
        initSchema();
    }

    private void createParentFolder() throws DataAccessException {
        if (url.startsWith(FILE_PREFIX) && !url.contains(":memory:")) {
            Path parent = Path.of(url.substring(FILE_PREFIX.length())).toAbsolutePath().getParent();
            try {
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } catch (IOException e) {
                throw new DataAccessException("Could not create the database folder " + parent, e);
            }
        }
    }

    /** Every call returns a new connection; close it with try-with-resources. */
    public Connection getConnection() throws DataAccessException {
        try {
            Connection con = DriverManager.getConnection(url);
            try (Statement st = con.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return con;
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the database " + url, e);
        }
    }

    private void initSchema() throws DataAccessException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            st.execute(CREATE_RUN);
            st.execute(CREATE_TRIP);
            st.execute("CREATE INDEX IF NOT EXISTS idx_trip_run ON trip(run_id)");
            LOG.info("Database ready: " + url);
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the tables", e);
        }
    }
}
