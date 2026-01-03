package routing;

import java.util.List;

public record SearchAnimation(PathResult result, List<Long> visitedOrder, List<long[]> exploredEdges) {
}
