package ch.sbb.pfi.netzgrafikeditor.converter.util.time;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

/**
 * The service day time starts at "noon minus 12 hours" and can extend into the next day. This approach allows night
 * services to be part of the same service day instead of being assigned to the following day, even if they depart after
 * midnight. For example, a Saturday night service would still belong to the service day of Saturday, even though it
 * departs on Sunday.
 * <p>
 * This class is an immutable value object. Its state cannot be modified after creation; manipulations will take effect
 * in a newly created value object.
 * <p>
 * Time definition according to <a href="https://gtfs.org/documentation/schedule/reference/#stop_timestxt">GTFS
 * Reference</a>:
 * <p>
 * <i>Time in the HH:MM:SS format (H:MM:SS is also accepted). The time is measured from "noon minus 12h" of the
 * service day (effectively midnight except for days on which daylight savings time changes occur). For times occurring
 * after midnight on the service day, enter the time as a value greater than 24:00:00 in HH:MM:SS. Example: 14:30:00 for
 * 2:30PM or 25:35:00 for 1:35AM on the next day.</i>
 */
@EqualsAndHashCode
public class ServiceDayTime implements Temporal, Comparable<ServiceDayTime>, Serializable {

    public static final ServiceDayTime MIN = new ServiceDayTime(0);
    public static final ServiceDayTime MAX = new ServiceDayTime(Integer.MAX_VALUE);
    public static final ServiceDayTime MIDNIGHT = MIN;
    static final int SECONDS_IN_MINUTE = 60;
    static final int MINUTES_IN_HOUR = 60;
    static final int HOURS_IN_DAY = 24;
    static final int SECONDS_IN_HOUR = SECONDS_IN_MINUTE * MINUTES_IN_HOUR;
    static final int SECONDS_IN_DAY = HOURS_IN_DAY * SECONDS_IN_HOUR;
    public static final ServiceDayTime NOON = MIDNIGHT.plus(12, ChronoUnit.HOURS);
    private final int totalSeconds;

    public ServiceDayTime(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Seconds cannot be negative");
        }
        this.totalSeconds = seconds;
    }

    public static ServiceDayTime of(int hours, int minutes, int seconds) {
        if (hours < 0) {
            throw new IllegalArgumentException("Hours cannot be negative");
        }
        if (minutes < 0 || minutes >= MINUTES_IN_HOUR) {
            throw new IllegalArgumentException("Minutes must be between 0 and 59 inclusive");
        }
        if (seconds < 0 || seconds >= SECONDS_IN_MINUTE) {
            throw new IllegalArgumentException("Seconds must be between 0 and 59 inclusive");
        }

        // cast to long to avoid potential int overflow
        long total = (long) hours * SECONDS_IN_HOUR + (long) minutes * SECONDS_IN_MINUTE + seconds;

        return new ServiceDayTime(Math.toIntExact(total));
    }

    public static ServiceDayTime from(Temporal temporal) {
        return new ServiceDayTime(temporal.get(ChronoField.SECOND_OF_DAY));
    }

    @Override
    public int compareTo(ServiceDayTime other) {
        return Integer.compare(this.totalSeconds, other.totalSeconds);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit chronoUnit) {
            return switch (chronoUnit) {
                case SECONDS, MINUTES, HOURS, DAYS -> true;
                default -> false;
            };
        }

        return false;
    }

    @Override
    public ServiceDayTime plus(TemporalAmount amount) {
        return (ServiceDayTime) amount.addTo(this);
    }

    @Override
    public ServiceDayTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit == null) {
            throw new UnsupportedTemporalTypeException("Unit cannot be null");
        }
        if (!(unit instanceof ChronoUnit chronoUnit)) {
            throw new UnsupportedTemporalTypeException("Unit not supported: " + unit);
        }

        long addedSeconds = switch (chronoUnit) {
            case SECONDS -> amountToAdd;
            case MINUTES -> amountToAdd * SECONDS_IN_MINUTE;
            case HOURS -> amountToAdd * SECONDS_IN_HOUR;
            case DAYS -> amountToAdd * SECONDS_IN_DAY;
            default -> throw new UnsupportedTemporalTypeException("Unit not supported: " + unit);
        };

        return new ServiceDayTime(Math.toIntExact(totalSeconds + addedSeconds));
    }

    @Override
    public ServiceDayTime minus(TemporalAmount amount) {
        return (ServiceDayTime) amount.subtractFrom(this);
    }

    @Override
    public ServiceDayTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        long secondsDiff = endExclusive.get(ChronoField.SECOND_OF_DAY) - this.totalSeconds;

        if (unit == null) {
            throw new UnsupportedTemporalTypeException("Unit cannot be null");
        }
        if (!(unit instanceof ChronoUnit chronoUnit)) {
            throw new UnsupportedTemporalTypeException("Unit not supported: " + unit);
        }

        return switch (chronoUnit) {
            case SECONDS -> secondsDiff;
            case MINUTES -> secondsDiff / SECONDS_IN_MINUTE;
            case HOURS -> secondsDiff / SECONDS_IN_HOUR;
            case DAYS -> secondsDiff / SECONDS_IN_DAY;
            default -> throw new UnsupportedTemporalTypeException("Unit not supported: " + unit);
        };
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (field == null) {
            return false;
        }
        if (!(field instanceof ChronoField chronoField)) {
            return false; // If the field is not a ChronoField, return false
        }

        return switch (chronoField) {
            case HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE, MINUTE_OF_DAY, SECOND_OF_DAY -> true;
            default -> false;
        };
    }

    @Override
    public ServiceDayTime with(TemporalField field, long newValue) {
        int hours = totalSeconds / SECONDS_IN_HOUR;
        int minutes = (totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE;
        int seconds = totalSeconds % SECONDS_IN_MINUTE;

        int safeValue = Math.toIntExact(newValue);

        if (field == null) {
            throw new UnsupportedTemporalTypeException("Field cannot be null");
        }
        if (!(field instanceof ChronoField chronoField)) {
            throw new UnsupportedTemporalTypeException("Field not supported: " + field);
        }

        return switch (chronoField) {
            case HOUR_OF_DAY -> ServiceDayTime.of(safeValue, minutes, seconds);
            case MINUTE_OF_HOUR -> ServiceDayTime.of(hours, safeValue, seconds);
            case SECOND_OF_MINUTE -> ServiceDayTime.of(hours, minutes, safeValue);
            case MINUTE_OF_DAY -> new ServiceDayTime(Math.toIntExact(newValue * SECONDS_IN_MINUTE + seconds));
            case SECOND_OF_DAY -> new ServiceDayTime(safeValue);
            default -> throw new UnsupportedTemporalTypeException("Field not supported: " + field);
        };
    }

    @Override
    public int get(TemporalField field) {
        return (int) getLong(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field == null) {
            throw new UnsupportedTemporalTypeException("Field cannot be null");
        }
        if (!(field instanceof ChronoField chronoField)) {
            throw new UnsupportedTemporalTypeException("Field not supported: " + field);
        }

        return switch (chronoField) {
            case HOUR_OF_DAY -> totalSeconds / SECONDS_IN_HOUR;
            case MINUTE_OF_HOUR -> (totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE;
            case SECOND_OF_MINUTE -> totalSeconds % SECONDS_IN_MINUTE;
            case MINUTE_OF_DAY -> totalSeconds / SECONDS_IN_MINUTE;
            case SECOND_OF_DAY -> totalSeconds;
            default -> throw new UnsupportedTemporalTypeException("Field not supported: " + field);
        };
    }


    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d", getLong(ChronoField.HOUR_OF_DAY), getLong(ChronoField.MINUTE_OF_HOUR),
                getLong(ChronoField.SECOND_OF_MINUTE));
    }

    public int toSecondOfDay() {
        return totalSeconds;
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field == ChronoField.HOUR_OF_DAY) {
            return ValueRange.of(0, MAX.getLong(ChronoField.HOUR_OF_DAY));
        } else if (field == ChronoField.MINUTE_OF_DAY) {
            return ValueRange.of(0, MAX.getLong(ChronoField.MINUTE_OF_DAY));
        } else if (field == ChronoField.SECOND_OF_DAY) {
            return ValueRange.of(0, MAX.getLong(ChronoField.SECOND_OF_DAY));
        }

        // delegate to a default implementation for other fields
        return Temporal.super.range(field);
    }
}
