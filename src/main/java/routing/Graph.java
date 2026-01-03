package routing;

import java.util.*;

public class Graph {
    private final Map<Long, Node> nodes = new HashMap<>();
    private final Map<Long, List<Edge>> adjacency = new HashMap<>();

    public void addNode(Node node) {
        nodes.put(node.id(), node);
        adjacency.computeIfAbsent(node.id(), k -> new ArrayList<>());
    }

    public void addEdge(long fromId, long toId, double weightSeconds) {
        if (!nodes.containsKey(fromId) || !nodes.containsKey(toId)) {
            throw new IllegalArgumentException("Both nodes must exist before adding an edge");
        }
        adjacency.computeIfAbsent(fromId, k -> new ArrayList<>())
                 .add(new Edge(fromId, toId, weightSeconds));
    }

    public Collection<Node> nodes() {
        return nodes.values();
    }

    public Node node(long id) {
        return nodes.get(id);
    }

    public List<Edge> edgesFrom(long nodeId) {
        return adjacency.getOrDefault(nodeId, List.of());
    }
}
