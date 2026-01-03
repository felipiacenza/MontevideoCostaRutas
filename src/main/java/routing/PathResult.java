package routing;

import java.util.List;

public record PathResult(double costMeters, List<Long> pathNodeIds) {
}

