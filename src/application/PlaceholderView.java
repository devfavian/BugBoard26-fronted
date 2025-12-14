package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class PlaceholderView extends BorderPane {

    public PlaceholderView(String pageTitle) {
        getStyleClass().add("root");

        Button back = new Button("← Home");
        back.setOnAction(e -> AppNavigator.goDashboard());

        Label t = new Label(pageTitle);
        t.getStyleClass().add("title");

        VBox box = new VBox(12, t, new Label("Pagina da implementare (solo UI)."), back);
        box.setAlignment(Pos.CENTER);

        setPadding(new Insets(18));
        setCenter(box);
    }
}
