package com.tumuyan.fixedplay;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomePressSequenceTest {

    @Test
    public void triggersOnThirdPressWithinWindow() {
        HomePressSequence.Result first = HomePressSequence.next(0, 0, 1000, 800, 3);
        HomePressSequence.Result second = HomePressSequence.next(
                first.count, first.startTime, 1400, 800, 3);
        HomePressSequence.Result third = HomePressSequence.next(
                second.count, second.startTime, 1800, 800, 3);

        assertFalse(first.triggered);
        assertFalse(second.triggered);
        assertTrue(third.triggered);
        assertEquals(3, third.count);
        assertEquals(1000, third.startTime);
    }

    @Test
    public void resetsWhenTotalWindowIsExceeded() {
        HomePressSequence.Result result = HomePressSequence.next(2, 1000, 1801, 800, 3);

        assertFalse(result.triggered);
        assertEquals(1, result.count);
        assertEquals(1801, result.startTime);
    }

    @Test
    public void resetsWhenTotalWindowExceedsFiveHundredMilliseconds() {
        HomePressSequence.Result result = HomePressSequence.next(2, 1000, 1601, 500, 3);

        assertFalse(result.triggered);
        assertEquals(1, result.count);
        assertEquals(1601, result.startTime);
    }

    @Test
    public void mainActivityUsesFiveHundredMillisecondUserPressWindow() throws Exception {
        Field field = MainActivity.class.getDeclaredField("SECONDARY_LAUNCHER_USER_PRESS_WINDOW_MS");
        field.setAccessible(true);

        assertEquals(500L, field.getLong(null));
    }

    @Test
    public void mainActivityUsesFifteenHundredMillisecondModernSystemTolerance() throws Exception {
        Field field = MainActivity.class.getDeclaredField("SECONDARY_LAUNCHER_SYSTEM_TOLERANCE_MS");
        field.setAccessible(true);

        assertEquals(1500L, field.getLong(null));
    }

    @Test
    public void resetsAfterDeviceRebootOrInvalidClockState() {
        HomePressSequence.Result result = HomePressSequence.next(2, 5000, 1000, 800, 3);

        assertFalse(result.triggered);
        assertEquals(1, result.count);
        assertEquals(1000, result.startTime);
    }

    @Test
    public void acceptsSystemSchedulingToleranceOnSlowDevices() {
        HomePressSequence.Result result = HomePressSequence.next(2, 1000, 2050, 1050, 3);

        assertTrue(result.triggered);
    }

    @Test
    public void startsNewSequenceAfterTriggerWasConsumed() {
        HomePressSequence.Result result = HomePressSequence.next(3, 1000, 5000, 1050, 3);

        assertFalse(result.triggered);
        assertEquals(1, result.count);
        assertEquals(5000, result.startTime);
    }
}
