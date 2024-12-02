package ch.sbb.pfi.netzgrafikeditor.converter.util.time;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;

import static ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceDayTimeTest {

    private ServiceDayTime midnight;
    private ServiceDayTime noon;
    private ServiceDayTime endOfDay;
    private ServiceDayTime midnightNextDay;
    private ServiceDayTime noonNextDay;

    @BeforeEach
    void setUp() {
        midnight = new ServiceDayTime(0);
        noon = ServiceDayTime.of(12, 0, 0);
        endOfDay = ServiceDayTime.of(23, 59, 59);
        midnightNextDay = ServiceDayTime.of(24, 0, 0);
        noonNextDay = ServiceDayTime.of(36, 0, 0);
    }

    @Nested
    class Construction {
        @Test
        void shouldCreate_midnight() {
            assertEquals(0, midnight.getLong(ChronoField.SECOND_OF_DAY));
            assertEquals("00:00:00", midnight.toString());
        }

        @Test
        void shouldCreate_noon() {
            assertEquals(SECONDS_IN_DAY / 2, noon.getLong(ChronoField.SECOND_OF_DAY));
            assertEquals("12:00:00", noon.toString());
        }

        @Test
        void shouldCreate_endOfDay() {
            assertEquals(SECONDS_IN_DAY - 1, endOfDay.getLong(ChronoField.SECOND_OF_DAY));
            assertEquals("23:59:59", endOfDay.toString());
        }

        @Test
        void shouldCreate_midnightNextDay() {
            assertEquals(SECONDS_IN_DAY, midnightNextDay.getLong(ChronoField.SECOND_OF_DAY));
            assertEquals("24:00:00", midnightNextDay.toString());
        }

        @Test
        void shouldCreate_noonNextDay() {
            assertEquals(SECONDS_IN_DAY * 1.5, noonNextDay.getLong(ChronoField.SECOND_OF_DAY));
            assertEquals("36:00:00", noonNextDay.toString());
        }

        @Test
        void shouldThrow_negativeTotalSeconds() {
            assertThrows(IllegalArgumentException.class, () -> new ServiceDayTime(-1));
        }

        @Test
        void shouldThrow_invalidHours() {
            assertThrows(IllegalArgumentException.class, () -> ServiceDayTime.of(-1, 0, 0));
        }

        @Test
        void shouldThrow_invalidMinutes() {
            assertThrows(IllegalArgumentException.class, () -> ServiceDayTime.of(0, -1, 0));
            assertThrows(IllegalArgumentException.class, () -> ServiceDayTime.of(0, 60, 0));
        }

        @Test
        void shouldThrow_invalidSeconds() {
            assertThrows(IllegalArgumentException.class, () -> ServiceDayTime.of(0, 0, -1));
            assertThrows(IllegalArgumentException.class, () -> ServiceDayTime.of(0, 0, 60));
        }
    }

    @Nested
    class Field {
        @Test
        void shouldGet_thisDay() {
            assertEquals(0, midnight.get(ChronoField.HOUR_OF_DAY));
            assertEquals(0, midnight.get(ChronoField.MINUTE_OF_HOUR));
            assertEquals(0, midnight.get(ChronoField.SECOND_OF_MINUTE));

            assertEquals(12, noon.get(ChronoField.HOUR_OF_DAY));
            assertEquals(0, noon.get(ChronoField.MINUTE_OF_HOUR));
            assertEquals(0, noon.get(ChronoField.SECOND_OF_MINUTE));

            assertEquals(23, endOfDay.get(ChronoField.HOUR_OF_DAY));
            assertEquals(59, endOfDay.get(ChronoField.MINUTE_OF_HOUR));
            assertEquals(59, endOfDay.get(ChronoField.SECOND_OF_MINUTE));
        }

        @Test
        void shouldGet_nextDay() {
            assertEquals(24, midnightNextDay.get(ChronoField.HOUR_OF_DAY));
            assertEquals(0, midnightNextDay.get(ChronoField.MINUTE_OF_HOUR));
            assertEquals(0, midnightNextDay.get(ChronoField.SECOND_OF_MINUTE));

            assertEquals(36, noonNextDay.get(ChronoField.HOUR_OF_DAY));
            assertEquals(0, noonNextDay.get(ChronoField.MINUTE_OF_HOUR));
            assertEquals(0, noonNextDay.get(ChronoField.SECOND_OF_MINUTE));
        }

        @Nested
        class Range {

            @Test
            void shouldBeValidFor_hourOfDay() {
                assertEquals(ValueRange.of(0, Integer.MAX_VALUE / (SECONDS_IN_MINUTE * MINUTES_IN_HOUR)),
                        midnight.range(ChronoField.HOUR_OF_DAY));
            }

            @Test
            void shouldBeValidFor_minuteOfDay() {
                assertEquals(ValueRange.of(0, Integer.MAX_VALUE / SECONDS_IN_MINUTE),
                        midnight.range(ChronoField.MINUTE_OF_DAY));
            }

            @Test
            void shouldBeValidFor_secondOfDay() {
                assertEquals(ValueRange.of(0, Integer.MAX_VALUE), midnight.range(ChronoField.SECOND_OF_DAY));
            }

            @Test
            void shouldBeValidFor_minuteOfHour() {
                assertEquals(ValueRange.of(0, 59), midnight.range(ChronoField.MINUTE_OF_HOUR));
            }

            @Test
            void shouldBeValidFor_secondOfMinute() {
                assertEquals(ValueRange.of(0, 59), midnight.range(ChronoField.SECOND_OF_MINUTE));
            }
        }
    }

    @Nested
    class Comparison {
        @Test
        void shouldCompare_lessThan() {
            assertTrue(midnight.compareTo(noon) < 0);
            assertTrue(noon.compareTo(endOfDay) < 0);
            assertTrue(endOfDay.compareTo(midnightNextDay) < 0);
            assertTrue(midnightNextDay.compareTo(noonNextDay) < 0);

        }

        @Test
        void shouldCompare_greaterThan() {
            assertTrue(noonNextDay.compareTo(midnightNextDay) > 0);
            assertTrue(midnightNextDay.compareTo(endOfDay) > 0);
            assertTrue(endOfDay.compareTo(noon) > 0);
            assertTrue(noon.compareTo(midnight) > 0);
        }

        @Test
        void shouldCompare_equalTo() {
            ServiceDayTime anotherEndOfDay = ServiceDayTime.of(23, 59, 59);
            assertEquals(0, endOfDay.compareTo(anotherEndOfDay));

            ServiceDayTime anotherMidnightNextDay = ServiceDayTime.of(24, 0, 0);
            assertEquals(0, midnightNextDay.compareTo(anotherMidnightNextDay));

            ServiceDayTime anotherNoonNextDay = ServiceDayTime.of(36, 0, 0);
            assertEquals(0, noonNextDay.compareTo(anotherNoonNextDay));
        }
    }

    @Nested
    class Until {

        private ServiceDayTime serviceStart;
        private ServiceDayTime serviceEnd;

        @BeforeEach
        void setUp() {
            // setup case: Division with a remainder, which should be ignored
            serviceStart = ServiceDayTime.of(0, 0, 0);
            serviceEnd = ServiceDayTime.of(1, 1, 1);
        }

        @ParameterizedTest
        @CsvSource({"SECONDS, 1", "MINUTES, 60", "HOURS, 3600"})
        void shouldCalculateTimeDifference_forward(ChronoUnit unit, int secondsPerUnit) {
            assertTimeDifference(unit, secondsPerUnit, midnight, endOfDay);
            assertTimeDifference(unit, secondsPerUnit, midnight, midnightNextDay);
            assertTimeDifference(unit, secondsPerUnit, midnightNextDay, noonNextDay);
            assertTimeDifference(unit, secondsPerUnit, serviceStart, serviceEnd);
        }

        @ParameterizedTest
        @CsvSource({"SECONDS, 1", "MINUTES, 60", "HOURS, 3600"})
        void shouldCalculateTimeDifference_reverse(ChronoUnit unit, int secondsPerUnit) {
            assertTimeDifference(unit, secondsPerUnit, endOfDay, midnight);
            assertTimeDifference(unit, secondsPerUnit, midnightNextDay, midnight);
            assertTimeDifference(unit, secondsPerUnit, noonNextDay, midnightNextDay);
            assertTimeDifference(unit, secondsPerUnit, serviceEnd, serviceStart);
        }

        private void assertTimeDifference(ChronoUnit unit, int secondsPerUnit, ServiceDayTime start, ServiceDayTime end) {
            long expected = calculateExpectedDifference(start, end, secondsPerUnit);
            long actual = start.until(end, unit);
            assertEquals(expected, actual);
        }

        private long calculateExpectedDifference(ServiceDayTime start, ServiceDayTime end, int secondsPerUnit) {
            long differenceInSeconds = end.getLong(ChronoField.SECOND_OF_DAY) - start.getLong(
                    ChronoField.SECOND_OF_DAY);
            return differenceInSeconds / secondsPerUnit;
        }
    }

    @Nested
    class TimeManipulation {

        @Nested
        class Plus {
            @Test
            void shouldAdd_hours() {
                ServiceDayTime result = endOfDay.plus(1, ChronoUnit.HOURS);
                assertEquals(89999, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("24:59:59", result.toString());
            }

            @Test
            void shouldAdd_minutes() {
                ServiceDayTime result = endOfDay.plus(1, ChronoUnit.MINUTES);
                assertEquals(86459, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("24:00:59", result.toString());
            }

            @Test
            void shouldAdd_seconds() {
                ServiceDayTime result = endOfDay.plus(1, ChronoUnit.SECONDS);
                assertEquals(86400, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("24:00:00", result.toString());
            }
        }

        @Nested
        class Minus {
            @Test
            void shouldSubtract_hours() {
                ServiceDayTime result = midnightNextDay.minus(1, ChronoUnit.HOURS);
                assertEquals(82800, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("23:00:00", result.toString());
            }

            @Test
            void shouldSubtract_minutes() {
                ServiceDayTime result = midnightNextDay.minus(1, ChronoUnit.MINUTES);
                assertEquals(86340, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("23:59:00", result.toString());
            }

            @Test
            void shouldSubtract_seconds() {
                ServiceDayTime result = midnightNextDay.minus(1, ChronoUnit.SECONDS);
                assertEquals(86399, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("23:59:59", result.toString());
            }
        }

        @Nested
        class With {

            private ServiceDayTime time;

            @BeforeEach
            void setUp() {
                time = ServiceDayTime.of(12, 34, 56);
            }

            @Test
            void shouldChange_minuteOfHour() {
                Temporal result = time.with(ChronoField.MINUTE_OF_HOUR, 0);
                assertEquals(43256, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("12:00:56", result.toString());
            }

            @Test
            void shouldChange_secondOfMinute() {
                Temporal result = time.with(ChronoField.SECOND_OF_MINUTE, 0);
                assertEquals(45240, result.getLong(ChronoField.SECOND_OF_DAY));
                assertEquals("12:34:00", result.toString());
            }

            @Test
            void shouldThrowOnInvalid_minuteOfHour() {
                assertThrows(IllegalArgumentException.class, () -> time.with(ChronoField.MINUTE_OF_HOUR, -1));
                assertThrows(IllegalArgumentException.class, () -> time.with(ChronoField.MINUTE_OF_HOUR, 60));
            }

            @Test
            void shouldThrowOnInvalid_secondOfMinute() {
                assertThrows(IllegalArgumentException.class, () -> time.with(ChronoField.SECOND_OF_MINUTE, -1));
                assertThrows(IllegalArgumentException.class, () -> time.with(ChronoField.SECOND_OF_MINUTE, 60));
            }

            @Nested
            class ThisDay {

                @Test
                void shouldChange_hourOfDay() {
                    Temporal result = time.with(ChronoField.HOUR_OF_DAY, 0);
                    assertEquals(2096, result.getLong(ChronoField.SECOND_OF_DAY));
                    assertEquals("00:34:56", result.toString());
                }

                @Test
                void shouldChange_minuteOfDay() {
                    Temporal result = time.with(ChronoField.MINUTE_OF_DAY, 0);
                    assertEquals(56, result.getLong(ChronoField.SECOND_OF_DAY));
                    assertEquals("00:00:56", result.toString());
                }

                @Test
                void shouldChange_secondOfDay() {
                    Temporal result = time.with(ChronoField.SECOND_OF_DAY, 0);
                    assertEquals(0, result.getLong(ChronoField.SECOND_OF_DAY));
                    assertEquals("00:00:00", result.toString());
                }
            }

            @Nested
            class NextDay {

                @Test
                void shouldChange_hourOfDay() {
                    Temporal result = time.with(ChronoField.HOUR_OF_DAY, 25);
                    assertEquals(92096, result.getLong(ChronoField.SECOND_OF_DAY));
                    assertEquals("25:34:56", result.toString());
                }

                @Test
                void shouldChange_minuteOfDay() {
                    Temporal result = time.with(ChronoField.MINUTE_OF_DAY, 25 * MINUTES_IN_HOUR);
                    assertEquals(90056, result.getLong(ChronoField.SECOND_OF_DAY));
                    assertEquals("25:00:56", result.toString());
                }

                @Test
                void shouldChange_secondOfDay() {
                    Temporal result = time.with(ChronoField.SECOND_OF_DAY, 25 * SECONDS_IN_HOUR);
                    assertEquals(90000, result.getLong(ChronoField.SECOND_OF_DAY));
                    assertEquals("25:00:00", result.toString());
                }
            }
        }

        @Nested
        class IntegerOverflow {

            @Test
            void shouldThrow_plus() {
                assertThrows(ArithmeticException.class, () -> midnight.plus(Integer.MAX_VALUE, ChronoUnit.HOURS));
                assertThrows(ArithmeticException.class, () -> midnight.plus(Integer.MAX_VALUE, ChronoUnit.MINUTES));
                assertThrows(ArithmeticException.class,
                        () -> midnight.plus((long) Integer.MAX_VALUE + 1, ChronoUnit.SECONDS));
            }

            @Test
            void shouldThrow_minus() {
                assertThrows(ArithmeticException.class, () -> midnight.minus(Integer.MAX_VALUE, ChronoUnit.HOURS));
                assertThrows(ArithmeticException.class, () -> midnight.minus(Integer.MAX_VALUE, ChronoUnit.MINUTES));
                assertThrows(ArithmeticException.class,
                        () -> midnight.minus((long) Integer.MAX_VALUE + 2, ChronoUnit.SECONDS));
            }

            @Test
            void shouldThrow_with() {
                assertThrows(ArithmeticException.class,
                        () -> midnight.with(ChronoField.HOUR_OF_DAY, Integer.MAX_VALUE));
                assertThrows(ArithmeticException.class,
                        () -> midnight.with(ChronoField.MINUTE_OF_DAY, Integer.MAX_VALUE));
                assertThrows(ArithmeticException.class,
                        () -> midnight.with(ChronoField.SECOND_OF_DAY, (long) Integer.MAX_VALUE + 1));
            }
        }
    }
}
