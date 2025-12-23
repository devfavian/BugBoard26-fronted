package application;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.InputStream;

public class ModifyIssueView extends BorderPane {

    private final IssueItem item;

    private final TextField titleField = new TextField();
    private final TextArea descArea = new TextArea();

    private final ChoiceBox<TypeOpt> typeBox = new ChoiceBox<>();
    private final ChoiceBox<PriorityOpt> priorityBox = new ChoiceBox<>();

    private final Button pickImgBtn = new Button("Sostituisci immagine");
    private final Label fileLabel = new Label("Nessun file selezionato");
    private final ImageView preview = new ImageView();

    private File selectedImage = null;

    private final Button submitBtn = new Button("Salva modifiche");
    private final Button cancelBtn = new Button("Indietro");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private final Label errorLabel = new Label();

    public ModifyIssueView(IssueItem item) {
        this.item = item;

        getStyleClass().add("root");
        setPadding(new Insets(22));

        if (item == null || item.id() == null) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Issue non valida");
            a.setContentText("Impossibile modificare questa issue.");
            a.showAndWait();
            AppNavigator.goIssuesList();
            return;
        }

        if (!canEdit(item)) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Accesso negato");
            a.setContentText("Puoi modificare solo le issue create da te.");
            a.showAndWait();
            AppNavigator.goIssuesList();
            return;
        }

        setTop(buildTopBar());
        setCenter(buildForm());

        spinner.setVisible(false);
        spinner.setMaxSize(18, 18);

        errorLabel.getStyleClass().add("error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        typeBox.getItems().addAll(TypeOpt.values());
        typeBox.setValue(TypeOpt.fromApi(item.type()));

        priorityBox.getItems().addAll(PriorityOpt.values());
        priorityBox.setValue(PriorityOpt.fromApi(item.priority()));

        titleField.setPromptText("Titolo (obbligatorio)");
        titleField.getStyleClass().add("bb-input");
        titleField.setText(item.title() == null ? "" : item.title());

        descArea.setPromptText("Descrizione (obbligatoria)");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(6);
        descArea.getStyleClass().addAll("bb-input", "bb-textarea");
        descArea.setText(item.description() == null ? "" : item.description());

        typeBox.getStyleClass().add("bb-choice");
        priorityBox.getStyleClass().add("bb-choice");

        preview.setFitWidth(220);
        preview.setFitHeight(140);
        preview.setPreserveRatio(true);
        preview.getStyleClass().add("img-preview");

        pickImgBtn.getStyleClass().add("btn-secondary");
        submitBtn.getStyleClass().add("btn-primary");
        cancelBtn.getStyleClass().add("btn-secondary");

        titleField.textProperty().addListener((o, a, b) -> validate());
        descArea.textProperty().addListener((o, a, b) -> validate());

        pickImgBtn.setOnAction(e -> chooseImage());
        submitBtn.setOnAction(e -> doSubmit());
        cancelBtn.setOnAction(e -> AppNavigator.goIssuesList());

        validate();
        loadExistingImage();
    }

    private Node buildTopBar() {
        Button back = new Button();
        back.getStyleClass().add("icon-btn");
        back.setGraphic(loadIcon("icons/back.png", "←", 18));
        back.setOnAction(e -> AppNavigator.goIssuesList());

        Label title = new Label("Modifica Issue");
        title.getStyleClass().add("h2");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(12, back, title, spacer);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(14, 16, 14, 16));
        return top;
    }

    private Node buildForm() {
        Label hint = new Label("Aggiorna i campi desiderati. Se scegli una nuova immagine verrà sostituita.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        VBox card = new VBox(20,
                hint,
                field("Titolo", titleField),
                field("Descrizione", descArea),
                twoCols(
                        field("Tipo", typeBox),
                        field("Priorità (opzionale)", priorityBox)
                ),
                imagePickerRow(),
                preview,
                actionsRow(),
                errorLabel
        );
        card.getStyleClass().add("form-card");
        card.setMaxWidth(600);
        card.setPadding(new Insets(24));

        VBox wrap = new VBox(card);
        wrap.setAlignment(Pos.CENTER);
        return wrap;
    }

    private Node field(String label, Control control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(8, l, control);
        return box;
    }

    private Node twoCols(Node left, Node right) {
        HBox row = new HBox(20, left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    private Node imagePickerRow() {
        fileLabel.getStyleClass().add("file-muted");
        fileLabel.setWrapText(true);
        fileLabel.setMaxWidth(360);
        pickImgBtn.setWrapText(true);
        pickImgBtn.setPrefWidth(200);
        HBox row = new HBox(12, pickImgBtn, fileLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node actionsRow() {
        HBox row = new HBox(10, submitBtn, spinner, cancelBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void validate() {
        boolean ok = !titleField.getText().trim().isBlank() && !descArea.getText().trim().isBlank();
        submitBtn.setDisable(!ok);

        if (errorLabel.isVisible()) hideError();
    }

    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleziona immagine");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.webp")
        );

        File f = fc.showOpenDialog(getScene().getWindow());
        if (f == null) return;

        selectedImage = f;
        fileLabel.setText(f.getName());
        try {
            preview.setImage(new Image(f.toURI().toString()));
        } catch (Exception ex) {
            preview.setImage(null);
        }
    }

    private void loadExistingImage() {
        if (item.id() == null) return;
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                byte[] data = IssueApi.downloadIssueImageWithFallback(item.id(), item.path());
                return new Image(new java.io.ByteArrayInputStream(data));
            }
        };

        task.setOnSucceeded(e -> preview.setImage(task.getValue()));
        task.setOnFailed(e -> {
            // se non c'è immagine, ignoriamo silenziosamente
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void doSubmit() {
        hideError();
        setLoading(true);

        String title = titleField.getText().trim();
        String desc = descArea.getText().trim();

        String type = typeBox.getValue().apiValue();
        String priority = priorityBox.getValue().apiValueOrNull();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                IssueApi.modifyIssue(item.id(), title, desc, type, priority, item.state());
                if (selectedImage != null) {
                    IssueApi.uploadIssueImage(item.id(), selectedImage);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Issue aggiornata");
            a.setContentText("Modifiche salvate con successo.");
            a.showAndWait();
            AppNavigator.goIssuesList();
        });

        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof IssueApi.UnauthorizedException) {
                showError("Sessione non valida o scaduta. Effettua di nuovo il login.");
                AppNavigator.goLogin();
            } else if (ex instanceof IssueApi.ForbiddenException) {
                showError("Non hai i permessi per modificare questa issue.");
            } else {
                showError("Errore: " + ex.getMessage());
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void setLoading(boolean loading) {
        titleField.setDisable(loading);
        descArea.setDisable(loading);
        typeBox.setDisable(loading);
        priorityBox.setDisable(loading);
        pickImgBtn.setDisable(loading);
        submitBtn.setDisable(loading || submitBtn.isDisable());
        cancelBtn.setDisable(loading);
        spinner.setVisible(loading);

        if (!loading) validate();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private Node loadIcon(String resourcePath, String fallbackEmoji, int size) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}
        return new Label(fallbackEmoji);
    }

    private static boolean canEdit(IssueItem it) {
        boolean mine = Session.getUserId() != null && it.creatorId() != null
                && it.creatorId().longValue() == Session.getUserId().longValue();
        return Session.isAdmin() || mine;
    }

    private enum TypeOpt {
        QUESTION("Question", "QUESTION"),
        BUG("Bug", "BUG"),
        DOCUMENTATION("Documentation", "DOCUMENTATION"),
        FEATURE("Feature", "FEATURE");

        private final String label;
        private final String api;

        TypeOpt(String label, String api) {
            this.label = label;
            this.api = api;
        }

        public String apiValue() { return api; }

        public static TypeOpt fromApi(String api) {
            if (api != null) {
                for (TypeOpt t : values()) {
                    if (t.api.equalsIgnoreCase(api)) return t;
                }
            }
            return BUG;
        }

        @Override public String toString() { return label; }
    }

    private enum PriorityOpt {
        NONE("Nessuna", null),
        LOW("Bassa", "LOW"),
        MEDIUM("Media", "MEDIUM"),
        HIGH("Alta", "HIGH");

        private final String label;
        private final String api;

        PriorityOpt(String label, String api) {
            this.label = label;
            this.api = api;
        }

        public String apiValueOrNull() { return api; }

        public static PriorityOpt fromApi(String api) {
            if (api != null) {
                for (PriorityOpt p : values()) {
                    if (p.api != null && p.api.equalsIgnoreCase(api)) return p;
                }
            }
            return NONE;
        }

        @Override public String toString() { return label; }
    }
}
