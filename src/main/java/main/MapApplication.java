package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
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
        Scene scene = new Scene(root, 1500, 600, Color.web("#0b0c10"));

        stage.setTitle("Mapa Montevideo & Ciudad de la Costa");
        stage.setScene(scene);
        stage.show();
    }

}
