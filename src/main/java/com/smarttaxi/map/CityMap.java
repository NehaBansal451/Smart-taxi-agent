package com.smarttaxi.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.smarttaxi.model.Position;

/** An immutable city: a square grid with wall cells and taxi stops. Build one with {@link #builder(int)}. */
public final class CityMap implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 15;

    private final int size;
    private final Set<Position> walls;
    private final List<Position> stops;

    private CityMap(int size, Set<Position> walls, List<Position> stops) {
        this.size = size;
        this.walls = Collections.unmodifiableSet(new HashSet<>(walls));
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }

    public int getSize() {
        return size;
    }

    public boolean isInside(Position p) {
        return p.getRow() >= 0 && p.getRow() < size && p.getCol() >= 0 && p.getCol() < size;
    }

    public boolean isWall(Position p) {
        return walls.contains(p);
    }

    public Set<Position> getWalls() {
        return walls;
    }

    public List<Position> getStops() {
        return stops;
    }

    public Builder toBuilder() {
        Builder b = new Builder(size);
        b.walls.addAll(walls);
        b.stops.addAll(stops);
        return b;
    }

    public static Builder builder(int size) {
        return new Builder(size);
    }

    /** A ready-made 5x5 city with four corner stops and a wall in the middle column. */
    public static CityMap defaultMap() {
        return builder(5)
                .addWall(new Position(1, 2)).addWall(new Position(2, 2)).addWall(new Position(3, 2))
                .addStop(new Position(0, 0)).addStop(new Position(0, 4))
                .addStop(new Position(4, 0)).addStop(new Position(4, 4))
                .build();
    }

    /** Mutable helper used by the map editor and the map file reader. Throws IllegalArgumentException on bad input. */
    public static final class Builder {
        private final int size;
        private final Set<Position> walls = new HashSet<>();
        private final List<Position> stops = new ArrayList<>();

        private Builder(int size) {
            if (size < MIN_SIZE || size > MAX_SIZE) {
                throw new IllegalArgumentException("Map size must be between " + MIN_SIZE + " and " + MAX_SIZE);
            }
            this.size = size;
        }

        private void checkInside(Position p) {
            if (p.getRow() < 0 || p.getRow() >= size || p.getCol() < 0 || p.getCol() >= size) {
                throw new IllegalArgumentException("Position " + p + " is outside the map");
            }
        }

        public Builder addWall(Position p) {
            checkInside(p);
            stops.remove(p);
            walls.add(p);
            return this;
        }

        public Builder removeWall(Position p) {
            walls.remove(p);
            return this;
        }

        public Builder addStop(Position p) {
            checkInside(p);
            walls.remove(p);
            if (!stops.contains(p)) {
                stops.add(p);
            }
            return this;
        }

        public Builder removeStop(Position p) {
            stops.remove(p);
            return this;
        }

        public CityMap build() {
            return new CityMap(size, walls, stops);
        }
    }
}
