package OBDFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("dash.fxml"));
        primaryStage.setTitle("OBDFX");
        primaryStage.setScene(new Scene(root, Integer.parseInt(config.get("width")), Integer.parseInt(config.get("height"))));
        primaryStage.show();
    }

    static Process proc;

    static HashMap<String,String> config;

    public static void main(String[] args) {
        config = new HashMap<>();
        String[] configArray = io.readFileArranged("config","::");
        config.put("width",configArray[0]);
        config.put("height",configArray[1]);
        launch(args);
    }
}
