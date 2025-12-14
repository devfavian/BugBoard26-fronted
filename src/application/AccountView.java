package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AccountView extends BorderPane {

    public AccountView() {
        getStyleClass().add("root");
        setTop(topBar());
        setCenter(content());
        setPadding(new Insets(18));
    }

    private HBox topBar() {
        Button back = new Button("← Indietro");
        back.setOnAction(e -> AppNavigator.goDashboard());

        Label title = new Label("Modifica Account");
        title.getStyleClass().add("title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(12, back, spacer, title);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(10));
        return top;
    }

    private Pane content() {
        TextField email = new TextField();
        PasswordField psw = new PasswordField();

        email.setPromptText("Nuova email");
        psw.setPromptText("Nuova password");

        Button save = new Button("Salva");
        save.getStyleClass().add("big-btn");

        // SOLO FRONTEND: per ora simuliamo il salvataggio
        save.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Frontend");
            a.setContentText("Qui andrà la chiamata API per aggiornare account.\n"
                    + "Email: " + email.getText());
            a.showAndWait();
        });

        VBox box = new VBox(12,
                new Label("Email"), email,
                new Label("Password"), psw,
                save
        );
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(420);

        return new StackPane(box);
    }
}
