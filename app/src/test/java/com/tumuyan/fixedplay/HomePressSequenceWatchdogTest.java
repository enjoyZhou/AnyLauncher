package com.tumuyan.fixedplay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomePressSequenceWatchdogTest {

    @Test
    public void keepsTriggeredSequenceLockedUntilItIsConsumed() {
        HomePressSequence.Result result = HomePressSequence.next(3, 1000, 5000, 800, 3);

        assertTrue(result.triggered);
        assertEquals(3, result.count);
        assertEquals(1000, result.startTime);
    }

    @Test
    public void settingsResumeOnlyClearsTriggeredSequence() {
        assertFalse(HomePressSequence.shouldClearOnSettingsResume(2, 3));
        assertTrue(HomePressSequence.shouldClearOnSettingsResume(3, 3));
        assertTrue(HomePressSequence.shouldClearOnSettingsResume(4, 3));
    }
}
