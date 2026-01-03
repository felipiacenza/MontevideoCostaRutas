package routing;

import java.util.*;

public class AStar {
    public PathResult shortestPath(Graph graph, long sourceId, long targetId) {
        return shortestPathAnimated(graph, sourceId, targetId).result();
    }

    public SearchAnimation shortestPathAnimated(Graph graph, long sourceId, long targetId) {
        record NodeScore(long id, double fScore) {}

        Map<Long, Double> g = new HashMap<>();
        Map<Long, Double> f = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<NodeScore> open = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));
        List<Long> visitedOrder = new ArrayList<>();

        g.put(sourceId, 0.0);
        f.put(sourceId, heuristic(graph, sourceId, targetId));
        open.add(new NodeScore(sourceId, f.get(sourceId)));

        while (!open.isEmpty()) {
            NodeScore current = open.poll();
            long u = current.id;
            if (u == targetId) break;
            if (current.fScore > f.getOrDefault(u, Double.POSITIVE_INFINITY)) continue;

            visitedOrder.add(u);

            for (Edge e : graph.edgesFrom(u)) {
                double tentativeG = g.get(u) + e.distanceMeters();
                if (tentativeG < g.getOrDefault(e.toId(), Double.POSITIVE_INFINITY)) {
                    g.put(e.toId(), tentativeG);
                    prev.put(e.toId(), u);
                    double newF = tentativeG + heuristic(graph, e.toId(), targetId);
                    f.put(e.toId(), newF);
                    open.add(new NodeScore(e.toId(), newF));
                }
            }
        }

        Double total = g.get(targetId);
        if (total == null || total.isInfinite()) {
            return new SearchAnimation(new PathResult(Double.POSITIVE_INFINITY, List.of()), visitedOrder);
        }

        List<Long> path = new ArrayList<>();
        for (Long at = targetId; at != null; at = prev.get(at)) {
            path.add(at);
            if (at == sourceId) break;
        }
        Collections.reverse(path);
        return new SearchAnimation(new PathResult(total, path), visitedOrder);
    }

    private double heuristic(Graph g, long fromId, long toId) {
        return Heuristics.haversineMeters(g.node(fromId).point(), g.node(toId).point());
    }
}
