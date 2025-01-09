package ch.sbb.pfi.netzgrafikeditor.converter.util.spatial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for calculating the great-circle distance (Haversine distance) between two points on the Earth's
 * surface specified by their latitude and longitude. This implementation assumes a spherical Earth and does not account
 * for ellipsoidal effects, which may introduce small inaccuracies for certain calculations.
 * <p>
 * Source, modified from: <a
 * href="https://github.com/eugenp/tutorials/blob/a97478b1af6da0bb2bbf0fdbef53867fbc317e0b/algorithms-modules/algorithms-miscellaneous-1/src/main/java/com/baeldung/algorithms/latlondistance/HaversineDistance.java">com.baeldung.algorithms.latlondistance</a>
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public final class HaversineDistance {

    private static final int EARTH_RADIUS_METERS = 6_371_000;

    public static double calculate(Coordinate from, Coordinate to) {
        return calculate(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
    }

    public static double calculate(double fromLat, double fromLon, double toLat, double toLon) {

        double dLat = Math.toRadians((toLat - fromLat));
        double dLong = Math.toRadians((toLon - fromLon));

        fromLat = Math.toRadians(fromLat);
        toLat = Math.toRadians(toLat);

        double a = haversine(dLat) + Math.cos(fromLat) * Math.cos(toLat) * haversine(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    private static double haversine(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }
}