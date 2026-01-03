package main;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import mapdata.MapData;
import mapdata.MapWay;
import mapdata.Point;
import routing.RouteResult;
import routing.RouteService;
import routing.SearchAnimation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapView {
    private final Canvas canvasDijkstra;
    private final Canvas canvasAStar;
    private final Label infoDijkstra;
    private final Label infoAStar;
    private final MapData mapData;
    private final RouteService routeService;
    private final BorderPane root;
    private Stats dijkstraStats = Stats.empty();
    private Stats aStarStats = Stats.empty();

    private static final double MIN_SCALE = 0.2;
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final Color MAP_STROKE = Color.web("#bababa");
    private static final Color DIJKSTRA_COLOR = Color.web("#185be0");
    private static final Color DIJKSTRA_PATH_COLOR = Color.web("#74a1fc");
    private static final Color ASTAR_COLOR = Color.web("#e09d18");
    private static final Color ASTAR_PATH_COLOR = Color.web("#fccf74");

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
    private SearchAnimation dijkstraAnim;
    private SearchAnimation aStarAnim;

    private Deque<Long> dijkstraSteps = new ArrayDeque<>();
    private Deque<Long> aStarSteps = new ArrayDeque<>();
    private List<long[]> dijkstraEdges = new ArrayList<>();
    private List<long[]> aStarEdges = new ArrayList<>();
    private final Set<Long> visitedDijkstra = new HashSet<>();
    private final Set<Long> visitedAStar = new HashSet<>();
    private AnimationTimer animTimer;
    private static final int STEPS_PER_FRAME = 30;
    private boolean dijkstraDone = false;
    private boolean aStarDone = false;

    public MapView(MapData mapData, RouteService routeService) {
        this.mapData = mapData;
        this.routeService = routeService;
        this.canvasDijkstra = new Canvas(600, 800);
        this.canvasAStar = new Canvas(600, 800);
        this.infoDijkstra = buildInfoLabel("Dijkstra: pendiente");
        this.infoAStar = buildInfoLabel("A*: pendiente");

        StackPane leftPane = new StackPane(canvasDijkstra);
        StackPane rightPane = new StackPane(canvasAStar);
        leftPane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
        rightPane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
        canvasDijkstra.widthProperty().bind(leftPane.widthProperty());
        canvasDijkstra.heightProperty().bind(leftPane.heightProperty());
        canvasAStar.widthProperty().bind(rightPane.widthProperty());
        canvasAStar.heightProperty().bind(rightPane.heightProperty());

        VBox leftBox = new VBox(leftPane, infoDijkstra);
        VBox rightBox = new VBox(rightPane, infoAStar);
        VBox.setVgrow(leftPane, Priority.ALWAYS);
        VBox.setVgrow(rightPane, Priority.ALWAYS);
        stylePane(leftBox);
        stylePane(rightBox);

        HBox maps = new HBox(leftBox, rightBox);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        maps.setFillHeight(true);
        this.root = new BorderPane(maps);
        root.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, null, null)));
        root.setFocusTraversable(true);
        root.setOnMouseEntered(e -> root.requestFocus());
        root.setOnKeyPressed(this::handleKeyPress);

        centerMap();
        hookEvents();
        setupAnimationTimer();
        Platform.runLater(this::drawBoth); // ensure draw after layout sizing
    }

    public BorderPane getView() {
        return root;
    }

    private void hookEvents() {
        canvasDijkstra.setOnScroll(this::handleScroll);
        canvasAStar.setOnScroll(this::handleScroll);

        canvasDijkstra.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });
        canvasAStar.setOnMousePressed(canvasDijkstra.getOnMousePressed());

        canvasDijkstra.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                offsetX += e.getX() - lastMouseX;
                offsetY += e.getY() - lastMouseY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                userPanned = true;
                drawBoth();
            }
        });
        canvasAStar.setOnMouseDragged(canvasDijkstra.getOnMouseDragged());

        canvasDijkstra.setOnMouseClicked(e -> {
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
                drawBoth();
            }
        });
        canvasAStar.setOnMouseClicked(canvasDijkstra.getOnMouseClicked());

        canvasDijkstra.widthProperty().addListener((obs, oldV, newV) -> {
            if (!userPanned) centerMap();
            drawBoth();
        });
        canvasDijkstra.heightProperty().addListener((obs, oldV, newV) -> {
            if (!userPanned) centerMap();
            drawBoth();
        });
        canvasAStar.widthProperty().addListener((obs, oldV, newV) -> {
            if (!userPanned) centerMap();
            drawBoth();
        });
        canvasAStar.heightProperty().addListener((obs, oldV, newV) -> {
            if (!userPanned) centerMap();
            drawBoth();
        });
    }

    private void computeRoute() {
        if (startPoint != null && endPoint != null) {
            try {
                dijkstraAnim = routeService.animateDijkstra(startPoint, endPoint);
                aStarAnim = routeService.animateAStar(startPoint, endPoint);
                dijkstraRoute = null;
                aStarRoute = null;
                dijkstraSteps = new ArrayDeque<>(dijkstraAnim.visitedOrder());
                aStarSteps = new ArrayDeque<>(aStarAnim.visitedOrder());
                dijkstraEdges = new ArrayList<>(dijkstraAnim.exploredEdges());
                aStarEdges = new ArrayList<>(aStarAnim.exploredEdges());
                visitedDijkstra.clear();
                visitedAStar.clear();
                dijkstraDone = false;
                aStarDone = false;
                animTimer.start();
                dijkstraStats = buildStats(dijkstraAnim, true);
                aStarStats = buildStats(aStarAnim, false);
                updateInfoLabels();
            } catch (Exception ex) {
                dijkstraRoute = null;
                aStarRoute = null;
                dijkstraSteps.clear();
                aStarSteps.clear();
                dijkstraEdges.clear();
                aStarEdges.clear();
                dijkstraAnim = null;
                aStarAnim = null;
                visitedDijkstra.clear();
                visitedAStar.clear();
                dijkstraDone = false;
                aStarDone = false;
                dijkstraStats = Stats.empty();
                aStarStats = Stats.empty();
                updateInfoLabels();
            }
        }
    }

    private RouteResult toRouteResult(SearchAnimation anim) {
        List<Point> points = new ArrayList<>();
        for (Long nodeId : anim.result().pathNodeIds()) {
            points.add(routeService.getGraph().node(nodeId).point());
        }
        return new RouteResult(points, anim.result().cost());
    }

    private Stats buildStats(SearchAnimation anim, boolean isDijkstra) {
        double distance = computePathDistance(anim.result());
        double time = isDijkstra ? anim.result().cost() : computePathTime(anim.result());
        int iterations = anim.visitedOrder().size();
        double avgKmh = time > 0 ? (distance / time) * 3.6 : 0;
        return new Stats(time, distance, iterations, avgKmh);
    }

    private double computePathDistance(routing.PathResult result) {
        double total = 0;
        List<Long> ids = result.pathNodeIds();
        for (int i = 0; i < ids.size() - 1; i++) {
            Point a = routeService.getGraph().node(ids.get(i)).point();
            Point b = routeService.getGraph().node(ids.get(i + 1)).point();
            total += routing.Heuristics.haversineMeters(a, b);
        }
        return total;
    }

    private double computePathTime(routing.PathResult result) {
        double total = 0;
        List<Long> ids = result.pathNodeIds();
        for (int i = 0; i < ids.size() - 1; i++) {
            long from = ids.get(i);
            long to = ids.get(i + 1);
            for (routing.Edge e : routeService.getGraph().edgesFrom(from)) {
                if (e.toId() == to) {
                    total += e.timeSeconds();
                    break;
                }
            }
        }
        return total;
    }

    private void handleKeyPress(KeyEvent e) {
        if (!e.isControlDown()) return;
        if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS || e.getCode() == KeyCode.ADD) {
            scale = Math.max(MIN_SCALE, scale * 1.1);
            drawBoth();
            e.consume();
        } else if (e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT) {
            scale = Math.max(MIN_SCALE, scale * 0.9);
            drawBoth();
            e.consume();
        }
    }

    private void handleScroll(ScrollEvent e) {
        double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
        scale = Math.max(MIN_SCALE, scale * delta);
        drawBoth();
    }

    private void centerMap() {
        double minLat = mapData.minLat();
        double maxLat = mapData.maxLat();
        double minLon = mapData.minLon();
        double maxLon = mapData.maxLon();
        double width = Math.max(canvasDijkstra.getWidth(), 1);
        double height = Math.max(canvasDijkstra.getHeight(), 1);
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
        double height = canvasDijkstra.getHeight();
        double baseScale = Math.min(canvasDijkstra.getWidth() / (mapData.maxLon() - minLon), height / (mapData.maxLat() - minLat));
        double s = baseScale * scale;
        double lon = (x - offsetX) / s + minLon;
        double lat = ((height - y) + offsetY) / s + minLat;
        return new Point(lat, lon);
    }

    private void drawBoth() {
        drawSingle(canvasDijkstra.getGraphicsContext2D(), canvasDijkstra, true);
        drawSingle(canvasAStar.getGraphicsContext2D(), canvasAStar, false);
    }

    private void drawSingle(GraphicsContext gc, Canvas canvas, boolean isDijkstra) {
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
            drawPolyline(gc, minLat, minLon, height, s, pts);
        }

        Set<Long> visited = isDijkstra ? visitedDijkstra : visitedAStar;
        List<long[]> edges = isDijkstra ? dijkstraEdges : aStarEdges;
        gc.setStroke(isDijkstra ? DIJKSTRA_PATH_COLOR : ASTAR_PATH_COLOR);
        gc.setLineWidth(2.0);
        for (long[] e : edges) {
            if (!visited.contains(e[0])) continue;
            Point from = routeService.getGraph().node(e[0]).point();
            Point to = routeService.getGraph().node(e[1]).point();
            pointToScreenX(gc, minLat, minLon, height, s, from, to);
        }
        gc.setFill(isDijkstra ? DIJKSTRA_PATH_COLOR : ASTAR_PATH_COLOR);

        if (isDijkstra && dijkstraRoute != null && dijkstraDone) {
            gc.setStroke(DIJKSTRA_COLOR);
            gc.setLineWidth(6.0);
            drawRoute(gc, dijkstraRoute.points(), s, minLon, minLat, height);
        }
        if (!isDijkstra && aStarRoute != null && aStarDone) {
            gc.setStroke(ASTAR_COLOR);
            gc.setLineWidth(6.0);
            drawRoute(gc, aStarRoute.points(), s, minLon, minLat, height);
        }

        if (startPoint != null) {
            drawMarker(gc, startPoint, Color.YELLOW, s, minLon, minLat, height);
        }
        if (endPoint != null) {
            drawMarker(gc, endPoint, Color.CYAN, s, minLon, minLat, height);
        }
    }

    private void pointToScreenX(GraphicsContext gc, double minLat, double minLon, double height, double s, Point from, Point to) {
        double ax = (from.lon() - minLon) * s + offsetX;
        double ay = height - (from.lat() - minLat) * s + offsetY;
        double bx = (to.lon() - minLon) * s + offsetX;
        double by = height - (to.lat() - minLat) * s + offsetY;
        gc.strokeLine(ax, ay, bx, by);
    }

    private void drawPolyline(GraphicsContext gc, double minLat, double minLon, double height, double s, List<Point> pts) {
        for (int i = 0; i < pts.size() - 1; i++) {
            Point a = pts.get(i);
            Point b = pts.get(i + 1);
            pointToScreenX(gc, minLat, minLon, height, s, a, b);
        }
    }

    private void drawRoute(GraphicsContext gc, List<Point> pts, double s, double minLon, double minLat, double height) {
        drawPolyline(gc, minLat, minLon, height, s, pts);
    }

    private void drawMarker(GraphicsContext gc, Point p, Color color, double s, double minLon, double minLat, double height) {
        double x = (p.lon() - minLon) * s + offsetX;
        double y = height - (p.lat() - minLat) * s + offsetY;
        gc.setFill(color);
        gc.fillOval(x - 4, y - 4, 8, 8);
    }

    private void setupAnimationTimer() {
        animTimer = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                int steps = 0;
                while (steps < STEPS_PER_FRAME && (!dijkstraSteps.isEmpty() || !aStarSteps.isEmpty())) {
                    if (!dijkstraSteps.isEmpty()) {
                        visitedDijkstra.add(dijkstraSteps.poll());
                    }
                    if (!aStarSteps.isEmpty()) {
                        visitedAStar.add(aStarSteps.poll());
                    }
                    steps++;
                }
                if (dijkstraSteps.isEmpty()) dijkstraDone = true;
                if (aStarSteps.isEmpty()) aStarDone = true;
                if (dijkstraDone && dijkstraRoute == null && dijkstraAnim != null) {
                    dijkstraRoute = toRouteResult(dijkstraAnim);
                }
                if (aStarDone && aStarRoute == null && aStarAnim != null) {
                    aStarRoute = toRouteResult(aStarAnim);
                }
                if (dijkstraDone) updateInfoLabel(infoDijkstra, dijkstraStats);
                if (aStarDone) updateInfoLabel(infoAStar, aStarStats);
                drawBoth();
                if (dijkstraSteps.isEmpty() && aStarSteps.isEmpty()) {
                    stop();
                    last = 0;
                }
            }
        };
    }

    private Label buildInfoLabel(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.WHITE);
        lbl.setStyle("-fx-padding: 6; -fx-font-size: 12px;");
        return lbl;
    }

    private void stylePane(VBox box) {
        box.setSpacing(4);
        box.setStyle("-fx-border-color: #4a6fa5; -fx-border-width: 1; -fx-border-radius: 2; -fx-padding: 4;");
    }

    private void updateInfoLabels() {
        updateInfoLabel(infoDijkstra, dijkstraStats);
        updateInfoLabel(infoAStar, aStarStats);
    }

    private void updateInfoLabel(Label label, Stats stats) {
        label.setText(String.format("Algorithm: %s | Time: %s | Distance: %.1fm | Iterations: %d | Avg speed: %.1f km/h",
                label == infoDijkstra ? "Dijkstra" : "A*",
                formatDuration(stats.timeSeconds),
                stats.distanceMeters,
                stats.iterations,
                stats.avgKmh));
    }

    private String formatDuration(double seconds) {
        long total = Math.max(0, Math.round(seconds));
        long minutes = total / 60;
        long secs = total % 60;
        return String.format("%dm %02ds", minutes, secs);
    }

    private record Stats(double timeSeconds, double distanceMeters, int iterations, double avgKmh) {
        static Stats empty() { return new Stats(0, 0, 0, 0); }
    }
}
