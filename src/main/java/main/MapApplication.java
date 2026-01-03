package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;

public class MapApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        MapLoader loader = new MapLoader();
        MapData mapData = loader.load(Path.of("maps/montevideo-costa-full.json"));

        MapView mapView = new MapView(mapData);

        BorderPane root = new BorderPane(mapView.getCanvas());
        Scene scene = new Scene(root, 1000, 1000, Color.web("#0b0c10"));

        stage.setTitle("Mapa Montevideo & Ciudad de la Costa");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
