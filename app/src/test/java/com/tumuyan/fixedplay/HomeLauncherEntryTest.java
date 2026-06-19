package com.tumuyan.fixedplay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HomeLauncherEntryTest {

    @Test
    public void ignoresNonHomeLaunchAndClearsPersistedSequence() {
        HomePressSequence.Result result = HomeLauncherEntry.next(
                false, 2, 1000, 1200, 1050, 3);

        assertFalse(result.triggered);
        assertEquals(0, result.count);
        assertEquals(0, result.startTime);
    }

    @Test
    public void countsColdStartThatCameFromHomeIntent() {
        assertEquals(true, HomeLauncherEntry.isHomeIntent("android.intent.action.MAIN", true));
    }

    @Test
    public void ignoresColdStartWithoutHomeCategory() {
        assertEquals(false, HomeLauncherEntry.isHomeIntent("android.intent.action.MAIN", false));
    }

    @Test
    public void opensSettingsWhenTripleHomeTriggeredWithoutSecondaryLauncherEnabled() {
        assertEquals(HomeLauncherEntry.TriggerAction.OPEN_SETTINGS,
                HomeLauncherEntry.actionForTrigger(false, "com.uncube.launcher3"));
    }

    @Test
    public void launchesSecondaryAppWhenTripleHomeTriggeredWithSecondaryLauncherEnabled() {
        assertEquals(HomeLauncherEntry.TriggerAction.LAUNCH_SECONDARY_APP,
                HomeLauncherEntry.actionForTrigger(true, "com.uncube.launcher3"));
    }
}
