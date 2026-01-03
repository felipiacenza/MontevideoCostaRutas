package montevideocostarutas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MapData load(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode elements = root.get("elements");
            if (elements == null || !elements.isArray()) {
                throw new IllegalArgumentException("JSON sin 'elements'");
            }

            List<MapWay> ways = new ArrayList<>();
            double minLat = Double.POSITIVE_INFINITY;
            double maxLat = Double.NEGATIVE_INFINITY;
            double minLon = Double.POSITIVE_INFINITY;
            double maxLon = Double.NEGATIVE_INFINITY;

            for (JsonNode element : elements) {
                if (!"way".equals(element.path("type").asText())) continue;
                long id = element.path("id").asLong();
                JsonNode geometry = element.get("geometry");
                if (geometry == null || !geometry.isArray()) continue;

                List<Point> points = new ArrayList<>();
                Iterator<JsonNode> iter = geometry.elements();
                while (iter.hasNext()) {
                    JsonNode node = iter.next();
                    double lat = node.path("lat").asDouble();
                    double lon = node.path("lon").asDouble();
                    points.add(new Point(lat, lon));
                    minLat = Math.min(minLat, lat);
                    maxLat = Math.max(maxLat, lat);
                    minLon = Math.min(minLon, lon);
                    maxLon = Math.max(maxLon, lon);
                }
                if (!points.isEmpty()) {
                    ways.add(new MapWay(id, points));
                }
            }

            if (ways.isEmpty()) {
                throw new IllegalStateException("No se encontraron 'ways' en el JSON");
            }

            return new MapData(ways, minLat, maxLat, minLon, maxLon);
        }
    }
}
