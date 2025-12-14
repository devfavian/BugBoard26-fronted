package application;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {

        // scena unica + navigazione tra schermate cambiando root
        Scene scene = new Scene(new LoginView(), 1000, 700);   // <-- dimensione “standard”
        AppNavigator.init(scene);

        scene.getStylesheets().add(
            getClass().getResource("application.css").toExternalForm()
        );

        primaryStage.setTitle("BugBoard26");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.setResizable(true);                       // <-- NON più false
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
