package montevideocostarutas;

import java.util.List;

public record MapData(List<MapWay> ways, double minLat, double maxLat, double minLon, double maxLon) {
}

