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
        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("card-grid");
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.setPrefWrapLength(760);

        Button report = card(
                "Segnala Issue",
                "Crea una nuova segnalazione con dettagli e priorità",
                loadIcon("icons/report.png", "📝", 42),
                AppNavigator::goReportIssue,
                "card-report"
        );

        Button view = card(
                "Visualizza Issue",
                "Vedi tutte le issue e modifica solo le tue",
                loadIcon("icons/view.png", "📋", 42),
                AppNavigator::goIssuesList,   // <-- IMPORTANT: qui deve portare alla lista
                "card-view"
        );

        grid.getChildren().addAll(report, view);

        if (Session.isAdmin()) {
            Button createUser = card(
                    "Crea Utenza",
                    "Genera un account per utenti o admin",
                    loadIcon("icons/user-add.png", "➕", 42),
                    AppNavigator::goAdminCreateUser,
                    "card-admin"
            );
            grid.getChildren().add(createUser);
        }

        VBox center = new VBox(18, grid);
        center.setAlignment(Pos.CENTER);
        return center;
    }

    private Button card(String title, String subtitle, Node icon, Runnable action, String... extraClasses) {
        Label t = new Label(title);
        t.getStyleClass().add("dash-card-title");

        Label s = new Label(subtitle);
        s.getStyleClass().add("dash-card-subtitle");
        s.setWrapText(true);

        icon.getStyleClass().add("dash-card-icon");
        VBox content = new VBox(10, icon, t, s);
        content.setAlignment(Pos.TOP_LEFT);

        Button b = new Button();
        b.getStyleClass().add("card-tile");
        if (extraClasses != null) {
            b.getStyleClass().addAll(extraClasses);
        }
        b.setGraphic(content);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setOnAction(e -> action.run());
        b.setMaxWidth(360);
        b.setPrefWidth(320);
        b.setPrefHeight(185);

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
