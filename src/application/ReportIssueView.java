package application;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.InputStream;

public class ReportIssueView extends BorderPane {

    private final TextField titleField = new TextField();
    private final TextArea descArea = new TextArea();

    private final ChoiceBox<TypeOpt> typeBox = new ChoiceBox<>();
    private final ChoiceBox<PriorityOpt> priorityBox = new ChoiceBox<>();

    private final Button pickImgBtn = new Button("Allega immagine");
    private final Label fileLabel = new Label("Nessun file selezionato");
    private final ImageView preview = new ImageView();

    private File selectedImage = null;

    private final Button submitBtn = new Button("Invia segnalazione");
    private final Button cancelBtn = new Button("Indietro");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private final Label errorLabel = new Label();

    public ReportIssueView() {
        getStyleClass().add("root");
        setPadding(new Insets(22));

        setTop(buildTopBar());
        setCenter(buildForm());

        spinner.setVisible(false);
        spinner.setMaxSize(18, 18);

        errorLabel.getStyleClass().add("error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        typeBox.getItems().addAll(TypeOpt.values());
        typeBox.setValue(TypeOpt.BUG);

        priorityBox.getItems().addAll(PriorityOpt.values());
        priorityBox.setValue(PriorityOpt.NONE);

        titleField.setPromptText("Titolo (obbligatorio)");
        titleField.getStyleClass().add("bb-input");

        descArea.setPromptText("Descrizione (obbligatoria)");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(6);
        descArea.getStyleClass().addAll("bb-input", "bb-textarea");

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
        cancelBtn.setOnAction(e -> AppNavigator.goDashboard());

        validate();
    }

    private Node buildTopBar() {
        Button back = new Button();
        back.getStyleClass().add("icon-btn");
        back.setGraphic(loadIcon("icons/back.png", "←", 18));
        back.setOnAction(e -> AppNavigator.goDashboard());

        Label title = new Label("Segnala Issue");
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
        Label hint = new Label("Compila almeno titolo e descrizione. Lo stato iniziale sarà “todo”.");
        hint.getStyleClass().add("muted");

        VBox card = new VBox(12,
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
        card.setMaxWidth(740);

        VBox wrap = new VBox(card);
        wrap.setAlignment(Pos.CENTER);
        return wrap;
    }

    private Node field(String label, Control control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(6, l, control);
        return box;
    }

    private Node twoCols(Node left, Node right) {
        HBox row = new HBox(14, left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    private Node imagePickerRow() {
        fileLabel.getStyleClass().add("file-muted");
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
                new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg", "*.webp")
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

    private void doSubmit() {
        hideError();
        setLoading(true);

        String title = titleField.getText().trim();
        String desc = descArea.getText().trim();

        String type = typeBox.getValue().apiValue(); // "BUG" ...
        String priority = priorityBox.getValue().apiValueOrNull(); // null se NONE
        String path = (selectedImage == null) ? null : selectedImage.toURI().toString(); // opzionale

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                IssueApi.createIssue(title, desc, type, priority, path);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Issue creata");
            a.setContentText("Segnalazione inviata con successo.");
            a.showAndWait();
            AppNavigator.goDashboard();
        });

        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof IssueApi.UnauthorizedException) {
                showError("Sessione non valida o scaduta. Effettua di nuovo il login.");
                AppNavigator.goLogin();
            } else if (ex instanceof IssueApi.ForbiddenException) {
                showError("Non hai i permessi per creare issue: " + ex.getMessage());
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

        @Override public String toString() { return label; }
    }
}
