package com.smarttaxi.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import com.smarttaxi.exception.InvalidMapException;
import com.smarttaxi.exception.StorageException;

class MapFileHandlerTest {

    private static void assertSameMap(CityMap a, CityMap b) {
        assertEquals(a.getSize(), b.getSize());
        assertEquals(a.getWalls(), b.getWalls());
        assertEquals(a.getStops(), b.getStops());
    }

    @Test
    void textRoundTripKeepsTheMap() throws Exception {
        CityMap original = CityMap.defaultMap();
        assertSameMap(original, MapFileHandler.parse(MapFileHandler.toText(original)));
    }

    @Test
    void fileRoundTripKeepsTheMap() throws Exception {
        Path file = Files.createTempFile("taxi-map", ".csv");
        try {
            MapFileHandler.save(CityMap.defaultMap(), file);
            assertSameMap(CityMap.defaultMap(), MapFileHandler.load(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void commentsAndBlankLinesAreIgnored() throws Exception {
        CityMap m = MapFileHandler.parse("# my city\n\nSIZE,3\nSTOP,0,0\nSTOP,2,2\n");
        assertEquals(3, m.getSize());
        assertEquals(2, m.getStops().size());
    }

    @Test
    void badInputIsRejectedWithLineNumbers() {
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse("STOP,0,0\nSIZE,5\n"));
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse("SIZE,5\nTREE,1,1\n"));
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse("SIZE,5\nSTOP,9,9\nSTOP,0,0\n"));
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse("SIZE,two\n"));
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse("SIZE,2\n"));
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse(""));
        assertThrows(InvalidMapException.class, () -> MapFileHandler.parse("SIZE,5\nSTOP,0,0\n"));
    }

    @Test
    void wallAndStopOnTheSameCellIsRejected() {
        assertThrows(InvalidMapException.class,
                () -> MapFileHandler.parse("SIZE,5\nWALL,1,1\nSTOP,1,1\nSTOP,0,0\n"));
    }

    @Test
    void enclosedStopIsRejected() {
        String text = "SIZE,5\nSTOP,0,0\nSTOP,4,4\nWALL,3,4\nWALL,4,3\n";
        InvalidMapException e = assertThrows(InvalidMapException.class, () -> MapFileHandler.parse(text));
        assertTrue(e.getMessage().contains("reached"));
    }

    @Test
    void missingFileThrowsStorageException() {
        assertThrows(StorageException.class, () -> MapFileHandler.load(Path.of("no-such-folder", "nope.csv")));
    }
}
