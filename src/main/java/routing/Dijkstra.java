package routing;

import java.util.*;

public class Dijkstra {

    public SearchAnimation shortestPathAnimated(Graph graph, long sourceId, long targetId) {
        record NodeDist(long id, double dist) {}

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<NodeDist> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeDist::dist));
        List<Long> visitedOrder = new ArrayList<>();
        List<long[]> exploredEdges = new ArrayList<>();

        dist.put(sourceId, 0.0);
        pq.add(new NodeDist(sourceId, 0.0));

        while (!pq.isEmpty()) {
            NodeDist current = pq.poll();
            if (current.id == targetId) break;
            if (current.dist > dist.getOrDefault(current.id, Double.POSITIVE_INFINITY)) continue;

            visitedOrder.add(current.id);

            for (Edge e : graph.edgesFrom(current.id)) {
                exploredEdges.add(new long[] { current.id, e.toId() });
                double alt = current.dist + e.timeSeconds();
                if (alt < dist.getOrDefault(e.toId(), Double.POSITIVE_INFINITY)) {
                    dist.put(e.toId(), alt);
                    prev.put(e.toId(), current.id);
                    pq.add(new NodeDist(e.toId(), alt));
                }
            }
        }

        Double total = dist.get(targetId);
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
}
