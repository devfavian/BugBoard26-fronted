package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class AccountView extends BorderPane {

    public AccountView() {
        getStyleClass().add("root");

        setPadding(new Insets(22));
        setTop(buildTopBar());
        setCenter(buildCenter());
    }

    private Node buildTopBar() {
        Label title = new Label("BugBoard26");
        title.getStyleClass().add("h1");

        Label subtitle = new Label("Account");
        subtitle.getStyleClass().add("muted");

        VBox left = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("← Dashboard");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> AppNavigator.goDashboard());

        HBox top = new HBox(12, left, spacer, back);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(14, 16, 14, 16));
        return top;
    }

    private Node buildCenter() {
        VBox card = new VBox(14);
        card.getStyleClass().add("section-card");
        card.setPadding(new Insets(18));
        card.setMaxWidth(520);

        Label h = new Label("Informazioni account");
        h.getStyleClass().add("h2");

        String email = Session.getEmail() == null ? "-" : Session.getEmail();
        String role = Session.getRole() == null ? "-" : Session.getRole();

        card.getChildren().addAll(
                h,
                kvRow("Email", email),
                kvRow("Ruolo", role)
        );

        BorderPane.setAlignment(card, Pos.CENTER);
        return card;
    }

    private Node kvRow(String k, String v) {
        Label key = new Label(k);
        key.getStyleClass().add("muted");

        Label val = new Label(v);
        val.getStyleClass().add("kv-value");

        VBox box = new VBox(4, key, val);
        box.getStyleClass().add("kv-row");
        return box;
    }
}
