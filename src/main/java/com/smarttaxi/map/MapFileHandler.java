package com.smarttaxi.map;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.smarttaxi.exception.InvalidMapException;
import com.smarttaxi.exception.StorageException;
import com.smarttaxi.model.Position;

/**
 * Reads and writes city maps as simple CSV text:
 * <pre>
 * SIZE,5
 * WALL,1,2
 * STOP,0,0
 * </pre>
 * Lines starting with # are comments. Rows and columns are zero-based.
 */
public final class MapFileHandler {

    private MapFileHandler() {
    }

    /** Converts a map to its text form. */
    public static String toText(CityMap map) {
        StringBuilder sb = new StringBuilder();
        sb.append("SIZE,").append(map.getSize()).append('\n');
        for (Position w : map.getWalls()) {
            sb.append("WALL,").append(w.getRow()).append(',').append(w.getCol()).append('\n');
        }
        for (Position s : map.getStops()) {
            sb.append("STOP,").append(s.getRow()).append(',').append(s.getCol()).append('\n');
        }
        return sb.toString();
    }

    /** Parses the text form and validates the result. */
    public static CityMap parse(String text) throws InvalidMapException {
        CityMap.Builder builder = null;
        Set<Position> walls = new HashSet<>();
        Set<Position> stops = new HashSet<>();
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int no = i + 1;
            String[] parts = line.split(",");
            String keyword = parts[0].trim().toUpperCase();
            try {
                switch (keyword) {
                    case "SIZE" -> {
                        if (builder != null || parts.length != 2) {
                            throw new InvalidMapException("Line " + no + ": SIZE must appear once as SIZE,n");
                        }
                        builder = CityMap.builder(Integer.parseInt(parts[1].trim()));
                    }
                    case "WALL", "STOP" -> {
                        if (builder == null) {
                            throw new InvalidMapException("Line " + no + ": SIZE must come first");
                        }
                        if (parts.length != 3) {
                            throw new InvalidMapException("Line " + no + ": expected " + keyword + ",row,col");
                        }
                        Position p = new Position(Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
                        if (keyword.equals("WALL")) {
                            builder.addWall(p);
                            walls.add(p);
                        } else {
                            builder.addStop(p);
                            stops.add(p);
                        }
                        if (walls.contains(p) && stops.contains(p)) {
                            throw new InvalidMapException("Line " + no + ": cell " + p + " is both a wall and a stop");
                        }
                    }
                    default -> throw new InvalidMapException("Line " + no + ": unknown keyword '" + parts[0] + "'");
                }
            } catch (NumberFormatException e) {
                throw new InvalidMapException("Line " + no + ": not a number", e);
            } catch (IllegalArgumentException e) {
                throw new InvalidMapException("Line " + no + ": " + e.getMessage(), e);
            }
        }
        if (builder == null) {
            throw new InvalidMapException("The map has no SIZE line");
        }
        CityMap map = builder.build();
        validate(map);
        return map;
    }

    /** A usable map has at least two stops and every stop can be reached from every other. */
    public static void validate(CityMap map) throws InvalidMapException {
        List<Position> stops = map.getStops();
        if (stops.size() < 2) {
            throw new InvalidMapException("A map needs at least 2 stops");
        }
        Set<Position> seen = new HashSet<>();
        Queue<Position> queue = new ArrayDeque<>();
        seen.add(stops.get(0));
        queue.add(stops.get(0));
        com.smarttaxi.model.Action[] moves = {com.smarttaxi.model.Action.SOUTH, com.smarttaxi.model.Action.NORTH,
            com.smarttaxi.model.Action.EAST, com.smarttaxi.model.Action.WEST};
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            for (com.smarttaxi.model.Action a : moves) {
                Position n = p.move(a);
                if (map.isInside(n) && !map.isWall(n) && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        for (Position s : stops) {
            if (!seen.contains(s)) {
                throw new InvalidMapException("Stop " + s + " cannot be reached from the other stops");
            }
        }
    }

    public static void save(CityMap map, Path file) throws StorageException {
        try {
            Files.writeString(file, toText(map), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not save map to " + file, e);
        }
    }

    public static CityMap load(Path file) throws InvalidMapException, StorageException {
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not read map from " + file, e);
        }
        return parse(text);
    }
}
