package com.smarttaxi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.smarttaxi.exception.InvalidConfigException;

class ConfigTest {

    @Test
    void missingKeysFallBackToDefaults() throws Exception {
        AppConfig config = new AppConfig(new Properties());
        assertEquals(0.1, config.getAlpha(), 1e-9);
        assertEquals(5000, config.getEpisodes());
        assertEquals(Optional.empty(), config.getSeed());
        assertFalse(config.isFareUsedInReward());
    }

    @Test
    void presentValuesOverrideDefaults() throws Exception {
        Properties p = new Properties();
        p.setProperty("q.alpha", "0.3");
        p.setProperty("training.seed", "42");
        AppConfig config = new AppConfig(p);
        assertEquals(0.3, config.getAlpha(), 1e-9);
        assertEquals(Optional.of(42L), config.getSeed());
    }

    @Test
    void outOfRangeValueThrows() {
        Properties p = new Properties();
        p.setProperty("q.alpha", "1.5");
        assertThrows(InvalidConfigException.class, () -> new AppConfig(p).getAlpha());
    }

    @Test
    void nonNumericValueThrows() {
        Properties p = new Properties();
        p.setProperty("training.episodes", "lots");
        assertThrows(InvalidConfigException.class, () -> new AppConfig(p).getEpisodes());
    }

    @Test
    void badBooleanThrows() {
        Properties p = new Properties();
        p.setProperty("fare.useInReward", "maybe");
        assertThrows(InvalidConfigException.class, () -> new AppConfig(p).isFareUsedInReward());
    }

    @Test
    void shippedConfigFileLoads() throws Exception {
        AppConfig config = AppConfig.load();
        assertEquals(200, config.getMaxSteps());
        assertEquals("jdbc:sqlite:data/taxi.db", config.getDbUrl());
    }

    @Test
    void loggerWritesToTheLogFile(@TempDir Path dir) throws Exception {
        Path logFile = dir.resolve("app.log");
        LoggerUtil.configure(logFile.toString(), "INFO");
        LoggerUtil.getLogger(ConfigTest.class).info("hello-from-test");
        LoggerUtil.reset();
        assertTrue(Files.readString(logFile).contains("hello-from-test"));
    }
}
