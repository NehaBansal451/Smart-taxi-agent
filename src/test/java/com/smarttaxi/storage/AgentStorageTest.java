package com.smarttaxi.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.smarttaxi.agent.QTable;
import com.smarttaxi.exception.StorageException;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

class AgentStorageTest {

    private static State state(int row, int col) {
        return new State(new Position(row, col), Optional.of(new Position(0, 4)), new Position(4, 4));
    }

    private static AgentSnapshot snapshot(int mapSize) {
        QTable table = new QTable();
        table.setQ(state(0, 0), Action.SOUTH, 3.5);
        table.setQ(state(1, 0), Action.EAST, -2.0);
        return new AgentSnapshot(table, 0.1, 0.99, 0.25, mapSize);
    }

    @Test
    void saveThenLoadGivesEqualQTable(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("agent.ser");
        AgentStorage.save(snapshot(5), file);
        AgentSnapshot loaded = AgentStorage.load(file, 5);
        assertEquals(snapshot(5).getQTable(), loaded.getQTable());
        assertEquals(0.25, loaded.getEpsilon(), 1e-9);
        assertEquals(0.1, loaded.getAlpha(), 1e-9);
        assertEquals(0.99, loaded.getGamma(), 1e-9);
    }

    @Test
    void corruptFileThrowsStorageException(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bad.ser");
        Files.writeString(file, "this is not a saved agent");
        assertThrows(StorageException.class, () -> AgentStorage.load(file, 5));
    }

    @Test
    void missingFileThrowsStorageException(@TempDir Path dir) {
        assertThrows(StorageException.class, () -> AgentStorage.load(dir.resolve("nope.ser"), 5));
    }

    @Test
    void wrongMapSizeIsRejected(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("agent.ser");
        AgentStorage.save(snapshot(5), file);
        assertThrows(StorageException.class, () -> AgentStorage.load(file, 7));
    }
}
