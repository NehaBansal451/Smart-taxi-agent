package com.smarttaxi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import com.smarttaxi.exception.InvalidConfigException;

/**
 * Typed access to config.properties. Missing keys fall back to documented defaults;
 * present but invalid values throw InvalidConfigException.
 */
public class AppConfig {

    private static final String RESOURCE = "/config.properties";

    private final Properties props;

    /** Creates a config from the given values; keys not present use the defaults. */
    public AppConfig(Properties overrides) {
        props = new Properties(defaultProperties());
        for (String key : overrides.stringPropertyNames()) {
            props.setProperty(key, overrides.getProperty(key));
        }
    }

    /** Loads src/main/resources/config.properties from the classpath (defaults if it is absent). */
    public static AppConfig load() throws InvalidConfigException {
        Properties loaded = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                loaded.load(in);
            }
        } catch (IOException e) {
            throw new InvalidConfigException("Could not read config.properties: " + e.getMessage());
        }
        return new AppConfig(loaded);
    }

    private static Properties defaultProperties() {
        Properties d = new Properties();
        d.setProperty("map.file", "data/maps/default.csv");
        d.setProperty("q.alpha", "0.1");
        d.setProperty("q.gamma", "0.99");
        d.setProperty("q.epsilon.start", "1.0");
        d.setProperty("q.epsilon.min", "0.05");
        d.setProperty("q.epsilon.decay", "0.999");
        d.setProperty("training.episodes", "5000");
        d.setProperty("training.maxStepsPerEpisode", "200");
        d.setProperty("training.seed", "");
        d.setProperty("reward.step", "-1");
        d.setProperty("reward.illegal", "-10");
        d.setProperty("reward.dropoff", "20");
        d.setProperty("fare.base", "5");
        d.setProperty("fare.perCell", "2");
        d.setProperty("fare.costPerStep", "0.5");
        d.setProperty("fare.peak.multiplier", "1.5");
        d.setProperty("fare.useInReward", "false");
        d.setProperty("history.sampleEvery", "50");
        d.setProperty("ui.refreshEveryEpisodes", "20");
        d.setProperty("db.url", "jdbc:sqlite:data/taxi.db");
        d.setProperty("agent.file", "data/agent.ser");
        d.setProperty("log.file", "logs/app.log");
        d.setProperty("log.level", "INFO");
        return d;
    }

    // ---- generic typed getters ----

    public String getString(String key) {
        String value = props.getProperty(key);
        return value == null ? "" : value.trim();
    }

    public int getInt(String key, int min, int max) throws InvalidConfigException {
        String text = getString(key);
        try {
            int value = Integer.parseInt(text);
            if (value < min || value > max) {
                throw new InvalidConfigException(key + " must be between " + min + " and " + max + " but was " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(key + " must be a whole number but was '" + text + "'");
        }
    }

    public double getDouble(String key, double min, double max) throws InvalidConfigException {
        String text = getString(key);
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || value < min || value > max) {
                throw new InvalidConfigException(key + " must be between " + min + " and " + max + " but was " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(key + " must be a number but was '" + text + "'");
        }
    }

    public boolean getBoolean(String key) throws InvalidConfigException {
        String text = getString(key);
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new InvalidConfigException(key + " must be true or false but was '" + text + "'");
    }

    /** The training seed, or empty when none is configured. */
    public Optional<Long> getSeed() throws InvalidConfigException {
        String text = getString("training.seed");
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(text));
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("training.seed must be a whole number or empty but was '" + text + "'");
        }
    }

    // ---- named getters ----

    public String getMapFile() { return getString("map.file"); }
    public String getDbUrl() { return getString("db.url"); }
    public String getAgentFile() { return getString("agent.file"); }
    public String getLogFile() { return getString("log.file"); }
    public String getLogLevel() { return getString("log.level"); }

    public double getAlpha() throws InvalidConfigException { return getDouble("q.alpha", 0.0, 1.0); }
    public double getGamma() throws InvalidConfigException { return getDouble("q.gamma", 0.0, 1.0); }
    public double getEpsilonStart() throws InvalidConfigException { return getDouble("q.epsilon.start", 0.0, 1.0); }
    public double getEpsilonMin() throws InvalidConfigException { return getDouble("q.epsilon.min", 0.0, 1.0); }
    public double getEpsilonDecay() throws InvalidConfigException { return getDouble("q.epsilon.decay", 0.0, 1.0); }

    public int getEpisodes() throws InvalidConfigException { return getInt("training.episodes", 1, 100000); }
    public int getMaxSteps() throws InvalidConfigException { return getInt("training.maxStepsPerEpisode", 1, 100000); }

    public double getStepReward() throws InvalidConfigException { return getDouble("reward.step", -1000, 1000); }
    public double getIllegalReward() throws InvalidConfigException { return getDouble("reward.illegal", -1000, 1000); }
    public double getDropoffReward() throws InvalidConfigException { return getDouble("reward.dropoff", -1000, 1000); }

    public double getFareBase() throws InvalidConfigException { return getDouble("fare.base", 0, 10000); }
    public double getFarePerCell() throws InvalidConfigException { return getDouble("fare.perCell", 0, 10000); }
    public double getCostPerStep() throws InvalidConfigException { return getDouble("fare.costPerStep", 0, 10000); }
    public double getPeakMultiplier() throws InvalidConfigException { return getDouble("fare.peak.multiplier", 1, 10); }
    public boolean isFareUsedInReward() throws InvalidConfigException { return getBoolean("fare.useInReward"); }

    public int getHistorySampleEvery() throws InvalidConfigException { return getInt("history.sampleEvery", 1, 100000); }
    public int getRefreshEveryEpisodes() throws InvalidConfigException { return getInt("ui.refreshEveryEpisodes", 1, 10000); }
}
