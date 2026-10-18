package com.smarttaxi.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable snapshot of the situation: taxi position, where the passenger waits (empty while the
 * passenger is inside the taxi) and the destination. Used as a HashMap key, so equals and hashCode matter.
 */
public final class State implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Position taxi;
    private final Position passenger; // null means the passenger is inside the taxi
    private final Position destination;

    public State(Position taxi, Optional<Position> passenger, Position destination) {
        this.taxi = Objects.requireNonNull(taxi, "taxi");
        this.passenger = Objects.requireNonNull(passenger, "passenger").orElse(null);
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    public Position getTaxi() {
        return taxi;
    }

    public Optional<Position> getPassenger() {
        return Optional.ofNullable(passenger);
    }

    public boolean isPassengerInTaxi() {
        return passenger == null;
    }

    public Position getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof State)) {
            return false;
        }
        State other = (State) o;
        return taxi.equals(other.taxi)
                && Objects.equals(passenger, other.passenger)
                && destination.equals(other.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taxi, passenger, destination);
    }

    @Override
    public String toString() {
        return "State[taxi=" + taxi + ", passenger=" + (passenger == null ? "in taxi" : passenger)
                + ", destination=" + destination + "]";
    }
}
