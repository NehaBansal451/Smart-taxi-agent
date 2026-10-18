package com.smarttaxi.util;

import com.smarttaxi.exception.InvalidConfigException;

/** Range checks for user and configuration input. Every method returns the value when it is valid. */
public final class Validator {

    public static final int MIN_GRID_SIZE = 3;
    public static final int MAX_GRID_SIZE = 15;
    public static final int MIN_EPISODES = 1;
    public static final int MAX_EPISODES = 100000;

    private Validator() {
    }

    /** Checks a value that must lie between 0 and 1 inclusive, such as alpha or gamma. */
    public static double requireUnitRange(String name, double value) throws InvalidConfigException {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new InvalidConfigException(name + " must be between 0 and 1 but was " + value);
        }
        return value;
    }

    /** Checks the city size (3 to 15). */
    public static int requireGridSize(int size) throws InvalidConfigException {
        if (size < MIN_GRID_SIZE || size > MAX_GRID_SIZE) {
            throw new InvalidConfigException("Grid size must be between " + MIN_GRID_SIZE
                    + " and " + MAX_GRID_SIZE + " but was " + size);
        }
        return size;
    }

    /** Checks the number of training episodes (1 to 100000). */
    public static int requireEpisodes(int episodes) throws InvalidConfigException {
        if (episodes < MIN_EPISODES || episodes > MAX_EPISODES) {
            throw new InvalidConfigException("Episodes must be between " + MIN_EPISODES
                    + " and " + MAX_EPISODES + " but was " + episodes);
        }
        return episodes;
    }

    /** Parses text typed into the episodes field and range-checks it. */
    public static int parseEpisodes(String text) throws InvalidConfigException {
        try {
            return requireEpisodes(Integer.parseInt(text == null ? "" : text.trim()));
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("Episodes must be a whole number but was '" + text + "'");
        }
    }
}
