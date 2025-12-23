package application;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IssueDetailView extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IssueItem item;

    public IssueDetailView(IssueItem item) {
        this.item = item;

        getStyleClass().add("root");
        setPadding(new Insets(22));
        setTop(buildTopBar());
        setCenter(buildContent());
    }

    private Node buildTopBar() {
        Label title = new Label("Dettagli Issue");
        title.getStyleClass().add("h2");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("Chiudi");
        close.getStyleClass().add("btn-secondary");
        close.setOnAction(e -> {
            Stage stage = (Stage) getScene().getWindow();
            stage.close();
        });

        HBox top = new HBox(12, title, spacer, close);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(14, 16, 14, 16));
        return top;
    }

    private Node buildContent() {
        Label title = new Label(safe(item.title()));
        title.getStyleClass().add("detail-title");

        Label desc = new Label(safe(item.description(), "Nessuna descrizione"));
        desc.getStyleClass().add("detail-desc");
        desc.setWrapText(true);

        HBox chips = new HBox(8,
                chip("TYPE: " + safe(item.type(), "-")),
                chip("STATE: " + safe(item.state(), "-")),
                chip("PRIO: " + safe(item.priority(), "-"))
        );

        VBox meta = new VBox(6,
                metaRow("ID", safe(item.id())),
                metaRow("Creatore", safe(item.creatorId())),
                metaRow("Creata il", fmtDate(item.createdAt())),
                metaRow("Aggiornata il", fmtDate(item.updatedAt()))
        );
        meta.getStyleClass().add("detail-meta");

        VBox attachment = buildAttachment();

        VBox card = new VBox(20, title, desc, chips, meta, attachment);
        card.getStyleClass().addAll("section-card", "detail-card");
        card.setPadding(new Insets(24));
        card.setMaxWidth(600);

        VBox wrap = new VBox(card);
        wrap.setAlignment(Pos.CENTER);
        return wrap;
    }

    private VBox buildAttachment() {
        Label header = new Label("Allegato");
        header.getStyleClass().add("detail-section-title");

        String path = item.path();
        if (path == null || path.isBlank()) {
            Label none = new Label("Nessun allegato");
            none.getStyleClass().add("muted");
            return new VBox(8, header, none);
        }

        String resolvedPath = resolvePath(path);

        ImageView img = new ImageView();
        img.setPreserveRatio(true);
        img.setFitWidth(520);
        img.setFitHeight(320);
        img.getStyleClass().add("detail-image");

        Label error = new Label("Impossibile caricare l'allegato.");
        error.getStyleClass().add("error");
        error.setVisible(false);
        error.setManaged(false);

        ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxSize(18, 18);

        loadImage(item.id(), resolvedPath, img, loading, error);

        VBox box = new VBox(10, header, img, loading, error);
        box.getStyleClass().add("detail-attachment-box");
        return box;
    }

    private Node metaRow(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("muted");

        Label v = new Label(value);
        v.getStyleClass().add("detail-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, k, spacer, v);
        row.getStyleClass().add("detail-row");
        return row;
    }

    private static Label chip(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("chip");
        return l;
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String safe(Long v) {
        return v == null ? "-" : String.valueOf(v);
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static String resolvePath(String path) {
        String p = path == null ? "" : path.trim();
        if (p.isEmpty()) return p;
        String lower = p.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) {
            return p;
        }
        Path local = Paths.get(p);
        if (local.isAbsolute() && Files.exists(local)) {
            return local.toUri().toString();
        }
        if (p.startsWith("/")) {
            return ApiConfig.BASE_URL + p;
        }
        return ApiConfig.BASE_URL + "/" + p;
    }

    private static void loadImage(Long issueId, String resolvedPath, ImageView img, ProgressIndicator loading, Label error) {
        if (resolvedPath == null || resolvedPath.isBlank()) return;

        loading.setVisible(true);
        if (resolvedPath.toLowerCase().startsWith("file:")) {
            Image image = new Image(resolvedPath, true);
            img.setImage(image);
            image.errorProperty().addListener((obs, was, is) -> {
                if (Boolean.TRUE.equals(is)) {
                    error.setVisible(true);
                    error.setManaged(true);
                }
            });
            loading.setVisible(false);
            return;
        }

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                byte[] data = IssueApi.downloadIssueImageWithFallback(issueId, resolvedPath);
                return new Image(new ByteArrayInputStream(data));
            }
        };

        task.setOnSucceeded(e -> {
            loading.setVisible(false);
            Image image = task.getValue();
            img.setImage(image);
            if (image.isError()) {
                error.setVisible(true);
                error.setManaged(true);
            }
        });
        task.setOnFailed(e -> {
            loading.setVisible(false);
            error.setVisible(true);
            error.setManaged(true);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static String fmtDate(LocalDateTime dt) {
        return dt == null ? "-" : DATE_FMT.format(dt);
    }
}
