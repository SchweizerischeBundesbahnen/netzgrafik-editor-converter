package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Represents the GTFS route_type, indicating the mode of transport. The integer values and descriptions correspond to
 * the official GTFS Static specification.
 *
 * @see <a href="https://gtfs.org/schedule/reference/#routestxt">GTFS route_type Documentation</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum RouteType {

    TRAM(0),
    SUBWAY(1),
    RAIL(2),
    BUS(3),
    FERRY(4),
    CABLE_TRAM(5),
    AERIAL_LIFT(6),
    FUNICULAR(7),
    TROLLEYBUS(11),
    MONORAIL(12);

    private final int value;

    public static RouteType fromValue(int value) {
        return Arrays.stream(RouteType.values())
                .filter(e -> e.getValue() == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RouteType value: " + value));
    }
}