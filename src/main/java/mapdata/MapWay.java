package mapdata;

import java.util.List;

public record MapWay(long id, List<Point> geometry) {
}
