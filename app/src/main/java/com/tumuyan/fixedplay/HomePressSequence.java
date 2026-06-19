package com.tumuyan.fixedplay;

final class HomePressSequence {

    static Result next(int previousCount, long sequenceStartTime, long currentTime,
                       long windowMillis, int requiredPresses) {
        if (previousCount >= requiredPresses) {
            previousCount = 0;
            sequenceStartTime = 0;
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
