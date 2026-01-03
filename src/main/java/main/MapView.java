package main;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

import java.util.List;

public class MapView {
    private final Canvas canvas;
    private final MapData mapData;

    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double lastMouseX;
    private double lastMouseY;

    public MapView(MapData mapData) {
        this.mapData = mapData;
        this.canvas = new Canvas(1000, 1000);

        draw();
        hookEvents();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    private void hookEvents() {
        canvas.setOnScroll(this::handleScroll);
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                offsetX += e.getX() - lastMouseX;
                offsetY += e.getY() - lastMouseY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                draw();
            }
        });
        canvas.widthProperty().addListener((obs, oldV, newV) -> draw());
        canvas.heightProperty().addListener((obs, oldV, newV) -> draw());
    }

    private void handleScroll(ScrollEvent e) {
        double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
        scale *= delta;
        draw();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double minLat = mapData.minLat();
        double maxLat = mapData.maxLat();
        double minLon = mapData.minLon();
        double maxLon = mapData.maxLon();

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;

        double baseScale = Math.min(width / lonRange, height / latRange);
        double s = baseScale * scale;

        gc.setStroke(Color.LIGHTPINK);
        gc.setLineWidth(1.0);

        for (MapWay way : mapData.ways()) {
            List<Point> pts = way.geometry();
            for (int i = 0; i < pts.size() - 1; i++) {
                Point a = pts.get(i);
                Point b = pts.get(i + 1);
                double ax = (a.lon() - minLon) * s + offsetX;
                double ay = height - (a.lat() - minLat) * s + offsetY;
                double bx = (b.lon() - minLon) * s + offsetX;
                double by = height - (b.lat() - minLat) * s + offsetY;
                gc.strokeLine(ax, ay, bx, by);
            }
        }
    }
}

