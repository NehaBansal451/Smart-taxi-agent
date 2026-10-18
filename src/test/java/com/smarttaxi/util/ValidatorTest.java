package com.smarttaxi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.smarttaxi.exception.InvalidConfigException;

class ValidatorTest {

    @Test
    void unitRangeAcceptsBoundariesAndRejectsOutside() throws Exception {
        assertEquals(0.0, Validator.requireUnitRange("alpha", 0.0));
        assertEquals(1.0, Validator.requireUnitRange("alpha", 1.0));
        assertThrows(InvalidConfigException.class, () -> Validator.requireUnitRange("alpha", -0.001));
        assertThrows(InvalidConfigException.class, () -> Validator.requireUnitRange("alpha", 1.001));
        assertThrows(InvalidConfigException.class, () -> Validator.requireUnitRange("alpha", Double.NaN));
    }

    @Test
    void gridSizeBoundaries() throws Exception {
        assertEquals(3, Validator.requireGridSize(3));
        assertEquals(15, Validator.requireGridSize(15));
        assertThrows(InvalidConfigException.class, () -> Validator.requireGridSize(2));
        assertThrows(InvalidConfigException.class, () -> Validator.requireGridSize(16));
    }

    @Test
    void episodeBoundaries() throws Exception {
        assertEquals(1, Validator.requireEpisodes(1));
        assertEquals(100000, Validator.requireEpisodes(100000));
        assertThrows(InvalidConfigException.class, () -> Validator.requireEpisodes(0));
        assertThrows(InvalidConfigException.class, () -> Validator.requireEpisodes(100001));
    }

    @Test
    void parseEpisodesHandlesTextInput() throws Exception {
        assertEquals(500, Validator.parseEpisodes(" 500 "));
        assertThrows(InvalidConfigException.class, () -> Validator.parseEpisodes("abc"));
        assertThrows(InvalidConfigException.class, () -> Validator.parseEpisodes(""));
        assertThrows(InvalidConfigException.class, () -> Validator.parseEpisodes(null));
        assertThrows(InvalidConfigException.class, () -> Validator.parseEpisodes("0"));
    }
}
