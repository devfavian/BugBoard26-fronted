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
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.InputStream;

public class AdminCreateUserView extends BorderPane {

    private final TextField emailField = new TextField();
    private final PasswordField pswField = new PasswordField();
    private final ChoiceBox<RoleOpt> roleBox = new ChoiceBox<>();

    private final Button submitBtn = new Button("Crea utenza");
    private final Button cancelBtn = new Button("Indietro");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private final Label errorLabel = new Label();

    public AdminCreateUserView() {
        getStyleClass().add("root");
        setPadding(new Insets(22));

        if (!Session.isAdmin()) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Accesso negato");
            a.setContentText("Solo gli admin possono creare un'utenza.");
            a.showAndWait();
            AppNavigator.goDashboard();
            return;
        }

        setTop(buildTopBar());
        setCenter(buildForm());

        spinner.setVisible(false);
        spinner.setMaxSize(18, 18);

        errorLabel.getStyleClass().add("error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        emailField.setPromptText("Email (obbligatoria)");
        pswField.setPromptText("Password (obbligatoria)");
        emailField.getStyleClass().add("bb-input");
        pswField.getStyleClass().add("bb-input");

        roleBox.getItems().addAll(RoleOpt.values());
        roleBox.setValue(RoleOpt.USER);
        roleBox.getStyleClass().add("bb-choice");

        emailField.textProperty().addListener((o, a, b) -> validate());
        pswField.textProperty().addListener((o, a, b) -> validate());

        submitBtn.getStyleClass().add("btn-primary");
        cancelBtn.getStyleClass().add("btn-secondary");

        submitBtn.setOnAction(e -> doSubmit());
        cancelBtn.setOnAction(e -> AppNavigator.goDashboard());

        validate();
    }

    private Node buildTopBar() {
        Button back = new Button();
        back.getStyleClass().add("icon-btn");
        back.setGraphic(loadIcon("icons/back.png", "←", 18));
        back.setOnAction(e -> AppNavigator.goDashboard());

        Label title = new Label("Crea Utenza");
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
        Label hint = new Label("Solo admin: crea un nuovo account con ruolo specifico.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        VBox card = new VBox(20,
                hint,
                field("Email", emailField),
                field("Password", pswField),
                field("Ruolo", roleBox),
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

    private Node actionsRow() {
        HBox row = new HBox(10, submitBtn, spinner, cancelBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void validate() {
        boolean ok = !emailField.getText().trim().isBlank()
                && !pswField.getText().trim().isBlank()
                && roleBox.getValue() != null;
        submitBtn.setDisable(!ok);

        if (errorLabel.isVisible()) hideError();
    }

    private void doSubmit() {
        hideError();
        setLoading(true);

        String email = emailField.getText().trim();
        String psw = pswField.getText().trim();
        String role = roleBox.getValue().apiValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                AdminApi.registerUser(email, psw, role);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Utenza creata");
            a.setContentText("Account creato con successo.");
            a.showAndWait();
            AppNavigator.goDashboard();
        });

        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof AdminApi.UnauthorizedException) {
                showError("Sessione non valida. Effettua di nuovo il login.");
                AppNavigator.goLogin();
            } else if (ex instanceof AdminApi.ForbiddenException) {
                showError("Non hai i permessi per creare utenze.");
            } else if (ex instanceof AdminApi.ConflictException) {
                showError("Email già esistente.");
            } else {
                showError("Errore: " + ex.getMessage());
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void setLoading(boolean loading) {
        emailField.setDisable(loading);
        pswField.setDisable(loading);
        roleBox.setDisable(loading);
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

    private enum RoleOpt {
        USER("Utente", "USER"),
        ADMIN("Admin", "ADMIN");

        private final String label;
        private final String api;

        RoleOpt(String label, String api) {
            this.label = label;
            this.api = api;
        }

        public String apiValue() { return api; }

        @Override public String toString() { return label; }
    }
}
