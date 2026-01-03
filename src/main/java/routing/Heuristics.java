package routing;

import mapdata.Point;

public final class Heuristics {
    private static final double EARTH_RADIUS_M = 6_371_000; // meters

    private Heuristics() {}

    public static double haversineMeters(Point a, Point b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return EARTH_RADIUS_M * c;
    }
}
