package routing;

import java.util.*;

public class AStar {

    public SearchAnimation shortestPathAnimated(Graph graph, long sourceId, long targetId) {
        record NodeScore(long id, double fScore) {
        }

        Map<Long, Double> g = new HashMap<>();
        Map<Long, Double> f = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<NodeScore> open = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));
        List<Long> visitedOrder = new ArrayList<>();
        List<long[]> exploredEdges = new ArrayList<>();

        g.put(sourceId, 0.0);
        f.put(sourceId, heuristic(graph, sourceId, targetId));
        open.add(new NodeScore(sourceId, f.get(sourceId)));

        Set<Long> closed = new HashSet<>();

        while (!open.isEmpty()) {
            NodeScore current = open.poll();
            long u = current.id;

            if (closed.contains(u)) continue;
            closed.add(u);

            if (u == targetId) break;

            visitedOrder.add(u);

            for (Edge e : graph.edgesFrom(u)) {
                exploredEdges.add(new long[]{u, e.toId()});

                double tentativeG = g.get(u) + e.distanceMeters();
                if (tentativeG < g.getOrDefault(e.toId(), Double.POSITIVE_INFINITY)) {
                    g.put(e.toId(), tentativeG);
                    prev.put(e.toId(), u);

                    double newF = tentativeG + 1.3 * heuristic(graph, e.toId(), targetId);

                    f.put(e.toId(), newF);
                    open.add(new NodeScore(e.toId(), newF));
                }
            }
        }


        return getSearchAnimation(sourceId, targetId, g, prev, visitedOrder, exploredEdges);
    }

    static SearchAnimation getSearchAnimation(long sourceId, long targetId, Map<Long, Double> g, Map<Long, Long> prev, List<Long> visitedOrder, List<long[]> exploredEdges) {
        Double total = g.get(targetId);
        if (total == null || total.isInfinite()) {
            return new SearchAnimation(new PathResult(Double.POSITIVE_INFINITY, List.of()), visitedOrder, exploredEdges);
        }

        List<Long> path = new ArrayList<>();
        for (Long at = targetId; at != null; at = prev.get(at)) {
            path.add(at);
            if (at == sourceId) break;
        }
        Collections.reverse(path);
        return new SearchAnimation(new PathResult(total, path), visitedOrder, exploredEdges);
    }

    private double heuristic(Graph g, long fromId, long toId) {
        return Heuristics.haversineMeters(g.node(fromId).point(), g.node(toId).point());
    }
}
