package main;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import mapdata.MapData;
import mapdata.MapWay;
import mapdata.Point;
import routing.RouteResult;
import routing.RouteService;

import java.util.List;

public class MapView {
    private final Canvas canvas;
    private final MapData mapData;
    private final RouteService routeService;
    private final BorderPane root;

    private static final double MIN_SCALE = 0.2;
    private static final Color BACKGROUND_COLOR = Color.web("#0b0c10");
    private static final Color MAP_STROKE = Color.web("#b5d8ff"); // pastel light blue
    private static final Color DIJKSTRA_COLOR = Color.web("#3399ff");
    private static final Color ASTAR_COLOR = Color.web("#5ad35a");

    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double lastMouseX;
    private double lastMouseY;
    private boolean userPanned = false;

    private Point startPoint;
    private Point endPoint;
    private RouteResult dijkstraRoute;
    private RouteResult aStarRoute;

    public MapView(MapData mapData, RouteService routeService) {
        this.mapData = mapData;
        this.routeService = routeService;
        this.canvas = new Canvas(1200, 800);
        StackPane mapPane = new StackPane(canvas);
        mapPane.widthProperty().addListener((obs, oldV, newV) -> canvas.setWidth(newV.doubleValue()));
        mapPane.heightProperty().addListener((obs, oldV, newV) -> canvas.setHeight(newV.doubleValue()));

        this.root = new BorderPane(mapPane);
        root.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
        root.setFocusTraversable(true);
        root.setOnMouseEntered(e -> root.requestFocus());
        root.setOnKeyPressed(this::handleKeyPress);

        centerMap();
        draw();
        hookEvents();
    }

    public BorderPane getView() {
        return root;
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
                userPanned = true;
                draw();
            }
        });
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                Point mapPoint = screenToGeo(e.getX(), e.getY());
                if (startPoint == null) {
                    startPoint = mapPoint;
                } else if (endPoint == null) {
                    endPoint = mapPoint;
                } else {
                    startPoint = mapPoint;
                    endPoint = null;
                    dijkstraRoute = null;
                    aStarRoute = null;
                }
                computeRoute();
                draw();
            }
        });
        canvas.widthProperty().addListener((obs, oldV, newV) -> {
            if (!userPanned) centerMap();
            draw();
        });
        canvas.heightProperty().addListener((obs, oldV, newV) -> {
            if (!userPanned) centerMap();
            draw();
        });
    }

    private void computeRoute() {
        if (startPoint != null && endPoint != null) {
            try {
                dijkstraRoute = routeService.routeDijkstra(startPoint, endPoint);
                aStarRoute = routeService.routeAStar(startPoint, endPoint);
            } catch (Exception ex) {
                dijkstraRoute = null;
                aStarRoute = null;
            }
        }
    }

    private void handleKeyPress(KeyEvent e) {
        if (!e.isControlDown()) return;
        if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS || e.getCode() == KeyCode.ADD) {
            scale = Math.max(MIN_SCALE, scale * 1.1);
            draw();
            e.consume();
        } else if (e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT) {
            scale = Math.max(MIN_SCALE, scale * 0.9);
            draw();
            e.consume();
        }
    }

    private void handleScroll(ScrollEvent e) {
        double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
        scale = Math.max(MIN_SCALE, scale * delta);
        draw();
    }

    private void centerMap() {
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
        offsetX = (width - lonRange * s) / 2;
        offsetY = (latRange * s - height) / 2;
    }

    private Point screenToGeo(double x, double y) {
        double minLat = mapData.minLat();
        double minLon = mapData.minLon();
        double height = canvas.getHeight();
        double baseScale = Math.min(canvas.getWidth() / (mapData.maxLon() - minLon), height / (mapData.maxLat() - minLat));
        double s = baseScale * scale;
        double lon = (x - offsetX) / s + minLon;
        double lat = ((height - y) + offsetY) / s + minLat;
        return new Point(lat, lon);
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BACKGROUND_COLOR);
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

        gc.setStroke(MAP_STROKE);
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

        if (dijkstraRoute != null) {
            gc.setStroke(DIJKSTRA_COLOR);
            gc.setLineWidth(15); // thicker stroke for Dijkstra
            drawRoute(gc, dijkstraRoute.points(), s, minLon, minLat, height);
        }
        if (aStarRoute != null) {
            gc.setStroke(ASTAR_COLOR);
            gc.setLineWidth(5); // thinner than Dijkstra to compare
            drawRoute(gc, aStarRoute.points(), s, minLon, minLat, height);
        }

        if (startPoint != null) {
            drawMarker(gc, startPoint, Color.YELLOW, s, minLon, minLat, height);
        }
        if (endPoint != null) {
            drawMarker(gc, endPoint, Color.CYAN, s, minLon, minLat, height);
        }
    }

    private void drawRoute(GraphicsContext gc, List<Point> pts, double s, double minLon, double minLat, double height) {
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

    private void drawMarker(GraphicsContext gc, Point p, Color color, double s, double minLon, double minLat, double height) {
        double x = (p.lon() - minLon) * s + offsetX;
        double y = height - (p.lat() - minLat) * s + offsetY;
        gc.setFill(color);
        gc.fillOval(x - 4, y - 4, 8, 8);
    }
}
