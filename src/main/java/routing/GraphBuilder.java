package routing;

import mapdata.MapData;
import mapdata.MapWay;
import mapdata.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class GraphBuilder {
    private static final double MERGE_EPS = 1e-6; // approx 0.11 m
    private static final double KMH_TO_MPS = 1000.0 / 3600.0;

    private static final double DEFAULT_PRIMARY = 60;
    private static final double DEFAULT_SECONDARY = 50;
    private static final double DEFAULT_TERTIARY = 40;
    private static final double DEFAULT_RESIDENTIAL = 30;
    private static final double DEFAULT_FALLBACK = 30;

    public Graph build(MapData mapData) {
        Graph graph = new Graph();
        Map<PointKey, Long> pointToNodeId = new HashMap<>();
        AtomicLong nextId = new AtomicLong(1);

        for (MapWay way : mapData.ways()) {
            List<Point> pts = way.geometry();
            double speedKmh = resolveSpeedKmh(way);
            double speedMps = speedKmh * KMH_TO_MPS;
            boolean oneway = isOneWay(way);
            for (int i = 0; i < pts.size(); i++) {
                Point p = pts.get(i);
                long nodeId = pointToNodeId.computeIfAbsent(new PointKey(p), k -> {
                    long id = nextId.getAndIncrement();
                    graph.addNode(new Node(id, p));
                    return id;
                });
                if (i > 0) {
                    Point prev = pts.get(i - 1);
                    long prevId = pointToNodeId.get(new PointKey(prev));
                    double distance = Heuristics.haversineMeters(prev, p);
                    double travelSeconds = distance / speedMps;
                    graph.addEdge(prevId, nodeId, travelSeconds, distance);
                    if (!oneway) {
                        graph.addEdge(nodeId, prevId, travelSeconds, distance);
                    }
                }
            }
        }
        return graph;
    }

    private double resolveSpeedKmh(MapWay way) {
        if (way.maxSpeedKmh() != null) {
            return way.maxSpeedKmh();
        }
        String h = way.highway();
        if (h == null) return DEFAULT_FALLBACK;
        return switch (h) {
            case "primary" -> DEFAULT_PRIMARY;
            case "secondary" -> DEFAULT_SECONDARY;
            case "tertiary" -> DEFAULT_TERTIARY;
            case "residential" -> DEFAULT_RESIDENTIAL;
            default -> DEFAULT_FALLBACK;
        };
    }

    private boolean isOneWay(MapWay way) {
        String val = way.oneway();
        if (val == null) return false;
        val = val.trim().toLowerCase();
        return switch (val) {
            case "yes", "true", "1" -> true;
            case "-1" -> true; // treat as oneway but reverse not handled here
            default -> false;
        };
    }

    private record PointKey(long latKey, long lonKey) {
        PointKey(Point p) {
            this(Math.round(p.lat() / MERGE_EPS), Math.round(p.lon() / MERGE_EPS));
        }
    }
}
