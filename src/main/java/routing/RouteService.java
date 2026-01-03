package routing;

import mapdata.Point;

import java.util.Optional;

public class RouteService {
    private final Graph graph;
    private final Dijkstra dijkstra = new Dijkstra();
    private final AStar aStar = new AStar();

    public RouteService(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public SearchAnimation animateDijkstra(Point start, Point end) {
        return animateInternal(start, end, false);
    }

    public SearchAnimation animateAStar(Point start, Point end) {
        return animateInternal(start, end, true);
    }

    private SearchAnimation animateInternal(Point start, Point end, boolean useAStar) {
        long startId = nearestNodeId(start).orElseThrow(() -> new IllegalArgumentException("No start node"));
        long endId = nearestNodeId(end).orElseThrow(() -> new IllegalArgumentException("No end node"));

        return useAStar
                ? aStar.shortestPathAnimated(graph, startId, endId)
                : dijkstra.shortestPathAnimated(graph, startId, endId);
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
