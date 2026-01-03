package routing;

import mapdata.Point;

import java.util.List;

public record RouteResult(List<Point> points, double distanceMeters) {
}

