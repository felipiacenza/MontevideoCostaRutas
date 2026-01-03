package routing;

import mapdata.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RouteService {
    private final Graph graph;
    private final Dijkstra dijkstra = new Dijkstra();
    private final AStar aStar = new AStar();

    public RouteService(Graph graph) {
        this.graph = graph;
    }

    public RouteResult route(Point start, Point end, RoutingAlgorithm algorithm) {
        long startId = nearestNodeId(start).orElseThrow(() -> new IllegalArgumentException("No start node"));
        long endId = nearestNodeId(end).orElseThrow(() -> new IllegalArgumentException("No end node"));

        PathResult pathResult = switch (algorithm) {
            case ASTAR -> aStar.shortestPath(graph, startId, endId);
            case DIJKSTRA -> dijkstra.shortestPath(graph, startId, endId);
        };

        List<Point> points = new ArrayList<>();
        for (Long nodeId : pathResult.pathNodeIds()) {
            points.add(graph.node(nodeId).point());
        }
        return new RouteResult(points, pathResult.costMeters());
    }

    private Optional<Long> nearestNodeId(Point target) {
        double best = Double.POSITIVE_INFINITY;
        Long bestId = null;
        for (Node n : graph.nodes()) {
            double d = Heuristics.haversineMeters(target, n.point());
            if (d < best) {
                best = d;
                bestId = n.id();
            }
        }
        return Optional.ofNullable(bestId);
    }
}
