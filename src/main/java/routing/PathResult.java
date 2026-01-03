package routing;

import java.util.List;

public record PathResult(double costSeconds, List<Long> pathNodeIds) {
}
