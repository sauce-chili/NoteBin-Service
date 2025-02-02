package vstu.isd.notebin.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class UtilFunc {
    public static boolean localDateTimeAreEquals(
            LocalDateTime first,
            LocalDateTime second,
            ChronoUnit precisionUnit,
            long precision
    ) {
        if (precision < 0) {
            throw new IllegalArgumentException("Precision must be greater than 0");
        }

        if (first == second) {
            return true;
        }

        return Duration.between(first, second).get(precisionUnit) < precision;
    }

    public static boolean localDateTimeAreEquals(
            LocalDateTime first,
            LocalDateTime second
    ) {
        return localDateTimeAreEquals(first, second, ChronoUnit.SECONDS, 1);
    }

    public static boolean durationAreEquals(
            Duration first,
            Duration second,
            ChronoUnit precisionUnit,
            long precision
    ) {
        if (precision < 0) {
            throw new IllegalArgumentException("Precision must be greater than 0");
        }

        if (first == second) {
            return true;
        }

        return first.minus(second).get(precisionUnit) < precision;
    }

    public static boolean durationAreEquals(
            Duration first,
            Duration second
    ) {
        return durationAreEquals(first, second, ChronoUnit.SECONDS, 1);
    }
}
