package com.smarttaxi.model;

import java.io.Serializable;

/** An immutable cell position on the city grid (zero-based row and column). */
public final class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /**
     * Returns the neighbouring position after a movement action. PICKUP and DROPOFF do not move,
     * so they return this position. No bounds check is made here; the environment does that.
     */
    public Position move(Action action) {
        return switch (action) {
            case SOUTH -> new Position(row + 1, col);
            case NORTH -> new Position(row - 1, col);
            case EAST -> new Position(row, col + 1);
            case WEST -> new Position(row, col - 1);
            default -> this;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return row + "," + col;
    }
}
