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

    public Graph build(MapData mapData) {
        Graph graph = new Graph();
        Map<PointKey, Long> pointToNodeId = new HashMap<>();
        AtomicLong nextId = new AtomicLong(1);

        for (MapWay way : mapData.ways()) {
            List<Point> pts = way.geometry();
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
                    double w = Heuristics.haversineMeters(prev, p);
                    graph.addEdge(prevId, nodeId, w);
                    graph.addEdge(nodeId, prevId, w); // bidirectional for now
                }
            }
        }
        return graph;
    }

    private record PointKey(long latKey, long lonKey) {
        PointKey(Point p) {
            this(Math.round(p.lat() / MERGE_EPS), Math.round(p.lon() / MERGE_EPS));
        }
    }
}
