package com.smarttaxi.storage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.smarttaxi.config.LoggerUtil;
import com.smarttaxi.exception.StorageException;

/** Saves and loads a trained agent using Java object serialization. */
public final class AgentStorage {

    private static final Logger LOG = LoggerUtil.getLogger(AgentStorage.class);

    private AgentStorage() {
    }

    /** Writes the snapshot to the file, creating the folder if needed. */
    public static void save(AgentSnapshot snapshot, Path file) throws StorageException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(snapshot);
            }
            LOG.info("Agent saved to " + file);
        } catch (IOException e) {
            LOG.severe("Could not save agent: " + e);
            throw new StorageException("Could not save agent: " + e.getMessage());
        }
    }

    /** Reads a snapshot; rejects corrupt files and snapshots made for a different map size. */
    public static AgentSnapshot load(Path file, int expectedMapSize) throws StorageException {
        Object loaded;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            loaded = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOG.severe("Could not load agent: " + e);
            throw new StorageException("Could not load agent: " + e.getMessage());
        }
        if (!(loaded instanceof AgentSnapshot)) {
            throw new StorageException("File does not contain a saved agent");
        }
        AgentSnapshot snapshot = (AgentSnapshot) loaded;
        if (snapshot.getMapSize() != expectedMapSize) {
            throw new StorageException("Saved agent is for a " + snapshot.getMapSize() + "x" + snapshot.getMapSize() + " map but the current map is " + expectedMapSize + "x" + expectedMapSize);
        }
        LOG.info("Agent loaded from " + file);
        return snapshot;
    }
}
