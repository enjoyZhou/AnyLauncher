package com.tumuyan.fixedplay;

final class HomePressSequence {

    static Result next(boolean previousTriggered,
                       int previousCount,
                       long sequenceStartTime,
                       long currentTime,
                       long windowMillis,
                       int requiredPresses) {
        if (previousTriggered) {
            return new Result(previousCount, sequenceStartTime, true);
        }
        return next(previousCount, sequenceStartTime, currentTime, windowMillis, requiredPresses);
    }

    static Result next(int previousCount, long sequenceStartTime, long currentTime,
                       long windowMillis, int requiredPresses) {
        if (isTriggered(previousCount, requiredPresses)) {
            return new Result(previousCount, sequenceStartTime, true);
        }

        long elapsed = currentTime - sequenceStartTime;
        boolean continuesSequence = previousCount > 0
                && sequenceStartTime > 0
                && elapsed >= 0
                && elapsed <= windowMillis;
        int count = continuesSequence ? previousCount + 1 : 1;
        long startTime = continuesSequence ? sequenceStartTime : currentTime;
        boolean triggered = count >= requiredPresses;
        return new Result(count, startTime, triggered);
    }

    static Result reconcile(boolean previousTriggered,
                            int previousCount,
                            long sequenceStartTime,
                            long currentTime,
                            long windowMillis,
                            int requiredPresses) {
        if (previousTriggered) {
            return new Result(previousCount, sequenceStartTime, true);
        }
        return reconcile(previousCount, sequenceStartTime, currentTime, windowMillis, requiredPresses);
    }

    static Result reconcile(int previousCount, long sequenceStartTime, long currentTime,
                            long windowMillis, int requiredPresses) {
        if (isTriggered(previousCount, requiredPresses)) {
            return new Result(previousCount, sequenceStartTime, true);
        }
        if (previousCount <= 0 || sequenceStartTime <= 0) {
            return new Result(0, 0, false);
        }

        long elapsed = currentTime - sequenceStartTime;
        boolean withinWatchdogWindow = elapsed >= 0 && elapsed <= windowMillis;
        if (withinWatchdogWindow) {
            return new Result(previousCount, sequenceStartTime, false);
        }
        return new Result(0, 0, false);
    }

    static boolean isTriggered(int count, int requiredPresses) {
        return count >= requiredPresses;
    }

    static boolean shouldClearOnSettingsResume(int count, int requiredPresses) {
        return isTriggered(count, requiredPresses);
    }

    static boolean shouldClearOnSettingsResume(boolean triggered) {
        return triggered;
    }

    static final class Result {
        final int count;
        final long startTime;
        final boolean triggered;

        Result(int count, long startTime, boolean triggered) {
            this.count = count;
            this.startTime = startTime;
            this.triggered = triggered;
        }
    }
}
