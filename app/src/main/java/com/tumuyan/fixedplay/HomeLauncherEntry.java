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
                                                 String configuredApp,
                                                 String launcherPackage) {
        return !pendingHomeInvocation
                && configuredApp != null
                && configuredApp.length() > 0
                && !launcherPackage.equals(configuredApp);
    }

    static HomePressSequence.Result next(boolean countAsPress,
                                         int previousCount,
                                         long sequenceStartTime,
                                         long currentTime,
                                         long windowMillis,
                                         int requiredPresses) {
        if (!countAsPress) {
            return new HomePressSequence.Result(0, 0, false);
        }
        return HomePressSequence.next(
                previousCount,
                sequenceStartTime,
                currentTime,
                windowMillis,
                requiredPresses);
    }
}
