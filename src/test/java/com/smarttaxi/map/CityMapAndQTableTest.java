package com.smarttaxi.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import com.smarttaxi.agent.QTable;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

class CityMapAndQTableTest {

    @Test
    void defaultMapHasExpectedContents() {
        CityMap m = CityMap.defaultMap();
        assertEquals(5, m.getSize());
        assertEquals(4, m.getStops().size());
        assertEquals(3, m.getWalls().size());
        assertTrue(m.isWall(new Position(2, 2)));
        assertFalse(m.isInside(new Position(5, 0)));
    }

    @Test
    void toBuilderDoesNotChangeTheOriginal() {
        CityMap m = CityMap.defaultMap();
        CityMap changed = m.toBuilder().addStop(new Position(2, 2)).build();
        assertFalse(changed.isWall(new Position(2, 2)));
        assertEquals(5, changed.getStops().size());
        assertEquals(4, m.getStops().size());
    }

    @Test
    void badSizeAndOutsidePositionAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CityMap.builder(2));
        assertThrows(IllegalArgumentException.class, () -> CityMap.builder(5).addWall(new Position(5, 5)));
    }

    @Test
    void qTableStoresAndFindsBestAction() {
        QTable q = new QTable();
        State s = new State(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4));
        assertEquals(0.0, q.getQ(s, Action.EAST));
        q.setQ(s, Action.EAST, 2.5);
        assertEquals(Action.EAST, q.getBestAction(s));
        assertEquals(2.5, q.getMaxValue(s));
        assertEquals(0.0, q.getQ(s, Action.WEST));
        assertEquals(1, q.size());
    }
}
