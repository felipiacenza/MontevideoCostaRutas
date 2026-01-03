package routing;

import java.util.List;

public record PathResult(double cost, List<Long> pathNodeIds) {
}
