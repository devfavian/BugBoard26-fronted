package application;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

public class IssuesListView extends BorderPane {

    private final ListView<IssueItem> list = new ListView<>();
    private final Label error = new Label();
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final ComboBox<SortOpt> sortCombo = new ComboBox<>();

    public IssuesListView() {
        getStyleClass().add("root");

        // --- Top bar
        Button back = new Button("← Dashboard");
        back.getStyleClass().add("btn-ghost");
        back.setOnAction(e -> AppNavigator.goDashboard());

        Label title = new Label("Issue");
        title.getStyleClass().add("page-title");

        Button profile = new Button("👤");
        profile.getStyleClass().addAll("btn-ghost", "icon-btn");
        profile.setOnAction(e -> AppNavigator.goAccount()); // se non ce l’hai, metti un Alert

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(10, back, title, spacer, profile);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(14, 16, 8, 16));
        top.getStyleClass().add("topbar");

        // --- Controls row
        sortCombo.getItems().addAll(
                new SortOpt("Data creazione", "createdAt"),
                new SortOpt("Priorità", "priority"),
                new SortOpt("Stato", "state")
        );
        sortCombo.setValue(sortCombo.getItems().getFirst());

        SVGPath refreshIcon = new SVGPath();
        refreshIcon.setContent("M12 2a10 10 0 1 0 10 10h-2a8 8 0 1 1-8-8v2l4-3-4-3v2z");
        refreshIcon.getStyleClass().add("refresh-icon");

        Button refresh = new Button();
        refresh.setGraphic(refreshIcon);
        refresh.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        refresh.getStyleClass().add("btn-refresh-icon");
        refresh.setTooltip(new Tooltip("Aggiorna"));
        refresh.setOnAction(e -> load());

        RotateTransition rotate = new RotateTransition(Duration.millis(300), refreshIcon);
        rotate.setInterpolator(Interpolator.EASE_BOTH);
        refresh.setOnMouseEntered(e -> {
            rotate.stop();
            rotate.setToAngle(180);
            rotate.playFromStart();
        });
        refresh.setOnMouseExited(e -> {
            rotate.stop();
            rotate.setToAngle(0);
            rotate.playFromStart();
        });

        HBox controls = new HBox(8,
                new Label("Ordina per:"),
                sortCombo,
                refresh
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(refresh, new Insets(0, 0, 0, 12));
        controls.setPadding(new Insets(0, 16, 10, 16));
        controls.getStyleClass().add("subbar");

        VBox header = new VBox(top, controls);
        setTop(header);

        // --- list
        list.getStyleClass().add("issues-list");
        list.setCellFactory(lv -> new IssueCell());
        setCenter(list);

        // --- bottom status
        error.getStyleClass().add("error");
        error.setManaged(false);
        error.setVisible(false);

        spinner.setMaxSize(22, 22);
        spinner.setVisible(false);

        HBox bottom = new HBox(10, spinner, error);
        bottom.setPadding(new Insets(10, 16, 16, 16));
        bottom.setAlignment(Pos.CENTER_LEFT);
        setBottom(bottom);

        load();
    }

    private void load() {
        hideError();
        setLoading(true);

        Task<List<IssueItem>> task = new Task<>() {
            @Override
            protected List<IssueItem> call() throws Exception {
                return IssueApi.getIssues(sortCombo.getValue().api());
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            list.getItems().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof IssueApi.UnauthorizedException) {
                showError("Sessione non valida. Effettua di nuovo il login.");
                AppNavigator.goLogin();
            } else if (ex instanceof IssueApi.ForbiddenException) {
                showError("Accesso negato: " + ex.getMessage());
            } else {
                showError("Errore caricamento issue: " + ex.getMessage());
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void setLoading(boolean v) {
        spinner.setVisible(v);
        list.setDisable(v);
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setManaged(true);
        error.setVisible(true);
    }

    private void hideError() {
        error.setText("");
        error.setManaged(false);
        error.setVisible(false);
    }

    private record SortOpt(String label, String api) {
        @Override public String toString() { return label; }
    }

    private void openDetails(IssueItem item) {
        if (item == null || getScene() == null) return;

        IssueDetailView root = new IssueDetailView(item);
        Scene scene = new Scene(root, 720, 640);
        scene.getStylesheets().add(
                getClass().getResource("application.css").toExternalForm()
        );

        Stage stage = new Stage();
        stage.setTitle("Issue #" + (item.id() == null ? "-" : item.id()));
        Window owner = getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setMinWidth(620);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }

    // ---- cell card style
    private class IssueCell extends ListCell<IssueItem> {
        @Override
        protected void updateItem(IssueItem it, boolean empty) {
            super.updateItem(it, empty);
            if (empty || it == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Label title = new Label(it.title());
            title.getStyleClass().add("card-title");

            String desc = it.description() == null ? "" : it.description();
            if (desc.length() > 140) desc = desc.substring(0, 140) + "…";
            Label description = new Label(desc);
            description.getStyleClass().add("card-desc");
            description.setWrapText(true);

            HBox chips = new HBox(8,
                    chip("TYPE: " + safe(it.type())),
                    chip("STATE: " + safe(it.state())),
                    chip("PRIO: " + safe(it.priority()))
            );

            boolean mine = Session.getUserId() != null && it.creatorId() != null
                    && it.creatorId().longValue() == Session.getUserId().longValue();

            boolean canEdit = Session.isAdmin() || mine;

            Button editBtn = new Button(canEdit ? "✏ Modifica" : "🔒 Modifica");
            editBtn.getStyleClass().add("btn-secondary");

            if (!canEdit) {
                editBtn.setDisable(true);
                editBtn.getStyleClass().add("btn-locked");
            } else {
                editBtn.setOnAction(e -> {
                    AppNavigator.goModifyIssue(it);
                });
            }

            Label owner = new Label(mine ? "🟢 Creata da te" : "⚪ Altra issue");
            owner.getStyleClass().add("muted");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox footer = new HBox(10, owner, spacer, editBtn);
            footer.setAlignment(Pos.CENTER_LEFT);

            VBox card = new VBox(10, title, description, chips, footer);
            card.getStyleClass().add("issue-card");
            card.setPadding(new Insets(12));
            card.getStyleClass().add("issue-card-clickable");
            card.setOnMouseClicked(e -> {
                if (e.getButton() != MouseButton.PRIMARY) return;
                if (isInsideButton(e.getTarget())) return;
                openDetails(it);
            });

            setText(null);
            setGraphic(card);
        }

        private static Label chip(String s) {
            Label l = new Label(s);
            l.getStyleClass().add("chip");
            return l;
        }

        private static String safe(String s) {
            return (s == null || s.isBlank()) ? "-" : s;
        }

        private static boolean isInsideButton(Object target) {
            if (!(target instanceof javafx.scene.Node node)) return false;
            javafx.scene.Node cur = node;
            while (cur != null) {
                if (cur instanceof ButtonBase) return true;
                cur = cur.getParent();
            }
            return false;
        }
    }
}
