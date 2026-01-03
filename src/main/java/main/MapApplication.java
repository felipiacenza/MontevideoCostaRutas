package main;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import mapdata.MapData;
import mapdata.MapLoader;
import routing.Graph;
import routing.GraphBuilder;
import routing.RouteService;

import java.io.IOException;
import java.nio.file.Path;

public class MapApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        MapLoader loader = new MapLoader();
        MapData mapData = loader.load(Path.of("maps/montevideo-costa-full.json"));

        Graph graph = new GraphBuilder().build(mapData);
        RouteService routeService = new RouteService(graph);

        MapView mapView = new MapView(mapData, routeService);

        BorderPane root = mapView.getView();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(1000, bounds.getWidth() * 0.8);
        double height = Math.max(700, bounds.getHeight() * 0.8);
        Scene scene = new Scene(root, width, height, Color.web("#0b0c10"));

        stage.setTitle("Montevideo & Ciudad de la Costa Map");
        stage.setScene(scene);
        stage.show();
    }
}
