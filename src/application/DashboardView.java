package application;

import java.io.InputStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class DashboardView extends BorderPane {

    public DashboardView() {
        getStyleClass().add("root");

        setTop(buildTopBar());
        setCenter(buildCenter());
        setPadding(new Insets(22));
    }

    private Node buildTopBar() {
        Label title = new Label("BugBoard26");
        title.getStyleClass().add("h1");

        Label subtitle = new Label("Dashboard");
        subtitle.getStyleClass().add("muted");

        VBox left = new VBox(2, title, subtitle);

        Button profileBtn = new Button();
        profileBtn.getStyleClass().add("icon-btn");
        profileBtn.setGraphic(loadIcon("icons/user.png", "👤", 22));
        profileBtn.setOnAction(e -> AppNavigator.goAccount());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(12, left, spacer, profileBtn);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(14, 16, 14, 16));
        return top;
    }

    private Node buildCenter() {
        Label welcome = new Label("Cosa vuoi fare?");
        welcome.getStyleClass().add("h2");

        Label role = new Label("Ruolo: " + (Session.getRole() == null ? "?" : Session.getRole()));
        role.getStyleClass().add("muted");

        VBox header = new VBox(4, welcome, role);
        header.setAlignment(Pos.CENTER);

        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("card-grid");
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);

        Button report = card(
                "Segnala Issue",
                "Crea una nuova segnalazione",
                loadIcon("icons/report.png", "📝", 42),
                AppNavigator::goReportIssue
        );

        Button edit = card(
                "Modifica Issue",
                "Aggiorna una segnalazione esistente",
                loadIcon("icons/edit.png", "✏️", 42),
                AppNavigator::goEditIssue
        );

        Button view = card(
                "Visualizza Issue",
                "Vista completa (solo Admin)",
                loadIcon("icons/view.png", "👁️", 42),
                AppNavigator::goViewIssues
        );

        boolean isAdmin = "ADMIN".equalsIgnoreCase(Session.getRole());
        view.setVisible(isAdmin);
        view.setManaged(isAdmin);

        grid.getChildren().addAll(report, view, edit);

        VBox center = new VBox(18, header, grid);
        center.setAlignment(Pos.CENTER);
        return center;
    }

    private Button card(String title, String subtitle, Node icon, Runnable action) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");

        Label s = new Label(subtitle);
        s.getStyleClass().add("card-subtitle");
        s.setWrapText(true);

        VBox content = new VBox(10, icon, t, s);
        content.setAlignment(Pos.TOP_LEFT);

        Button b = new Button();
        b.getStyleClass().add("card-tile");
        b.setGraphic(content);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setOnAction(e -> action.run());
        b.setMaxWidth(360);
        b.setPrefWidth(320);
        b.setPrefHeight(170);

        return b;
    }

    private Node loadIcon(String resourcePath, String fallbackEmoji, int size) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                iv.getStyleClass().add("icon-img");
                return iv;
            }
        } catch (Exception ignored) {}

        Label emoji = new Label(fallbackEmoji);
        emoji.getStyleClass().add("icon-emoji");
        return emoji;
    }
}
