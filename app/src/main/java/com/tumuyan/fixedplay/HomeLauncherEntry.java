package com.tumuyan.fixedplay;

final class HomeLauncherEntry {
    enum TriggerAction {
        OPEN_SETTINGS,
        LAUNCH_SECONDARY_APP
    }

    private static final String ACTION_MAIN = "android.intent.action.MAIN";

    static boolean isHomeIntent(String action, boolean hasHomeCategory) {
        return ACTION_MAIN.equals(action) && hasHomeCategory;
    }

    static TriggerAction actionForTrigger(boolean apply2nd, String app2nd) {
        if (!apply2nd || app2nd == null || app2nd.length() < 1) {
            return TriggerAction.OPEN_SETTINGS;
        }
        return TriggerAction.LAUNCH_SECONDARY_APP;
    }

    static boolean shouldRedirectToConfiguredApp(boolean pendingHomeInvocation,
                                                 boolean waitingForSettingsScreen,
                                                 String configuredApp,
                                                 String launcherPackage) {
        return !pendingHomeInvocation
                && !waitingForSettingsScreen
                && configuredApp != null
                && configuredApp.length() > 0
                && !launcherPackage.equals(configuredApp);
    }

    static boolean shouldRedirectToConfiguredApp(boolean pendingHomeInvocation,
                                                 String configuredApp,
                                                 String launcherPackage) {
        return shouldRedirectToConfiguredApp(
                pendingHomeInvocation,
                false,
                configuredApp,
                launcherPackage);
    }

    static HomePressSequence.Result next(boolean countAsPress,
                                         boolean previousTriggered,
                                         int previousCount,
                                         long sequenceStartTime,
                                         long currentTime,
                                         long windowMillis,
                                         int requiredPresses) {
        if (!countAsPress) {
            return HomePressSequence.reconcile(
                    previousTriggered,
                    previousCount,
                    sequenceStartTime,
                    currentTime,
                    windowMillis,
                    requiredPresses);
        }
        return HomePressSequence.next(
                previousTriggered,
                previousCount,
                sequenceStartTime,
                currentTime,
                windowMillis,
                requiredPresses);
    }

    static HomePressSequence.Result next(boolean countAsPress,
                                         int previousCount,
                                         long sequenceStartTime,
                                         long currentTime,
                                         long windowMillis,
                                         int requiredPresses) {
        return next(
                countAsPress,
                false,
                previousCount,
                sequenceStartTime,
                currentTime,
                windowMillis,
                requiredPresses);
    }
}
