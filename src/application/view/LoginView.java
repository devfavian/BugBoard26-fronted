package application.view;

import application.api.AuthApi;
import application.config.ApiConfig;
import application.core.AppNavigator;
import application.core.Session;
import application.model.LoginResponse;
import application.util.AsyncRunner;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LoginView extends BorderPane {

    private final TextField emailField = new TextField();
    private final PasswordField pswField = new PasswordField();
    private final Button loginBtn = new Button("Accedi");
    private final Label errorLabel = new Label();
    private final ProgressIndicator spinner = new ProgressIndicator();

    public LoginView() {
        getStyleClass().add("root");

        Label title = new Label("BugBoard26");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Accedi con email e password");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(6, title, subtitle);
        header.setPadding(new Insets(20, 20, 10, 20));

        emailField.setPromptText("Email");
        pswField.setPromptText("Password");

        emailField.textProperty().addListener((obs, o, n) -> validate());
        pswField.textProperty().addListener((obs, o, n) -> validate());

        errorLabel.getStyleClass().add("error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        spinner.setMaxSize(18, 18);
        spinner.setVisible(false);

        HBox actions = new HBox(10, loginBtn, spinner);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(20,
                labeled("Email", emailField),
                labeled("Password", pswField),
                actions,
                errorLabel
        );
        form.setPadding(new Insets(10, 20, 20, 20));

        VBox card = new VBox(12, header, form);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);

        BorderPane.setAlignment(card, Pos.CENTER);
        setCenter(card);

        loginBtn.setOnAction(e -> doLogin());
        validate();
    }

    private VBox labeled(String label, Control field) {
        Label l = new Label(label);
        l.getStyleClass().add("label");
        field.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(8, l, field);
        return box;
    }

    private void validate() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String psw = pswField.getText() == null ? "" : pswField.getText();

        boolean canClick = !email.isBlank() && !psw.isBlank();

        loginBtn.setDisable(!canClick);

        if (errorLabel.isVisible()) {
            hideError();
        }
    }


    private void doLogin() {
        hideError();

        String email = emailField.getText().trim();
        String psw = pswField.getText();

        setLoading(true);

        Task<LoginResponse> task = new Task<>() {
            @Override
            protected LoginResponse call() throws Exception {
                return AuthApi.login(ApiConfig.baseUrl(), email, psw);
            }
        };

        AsyncRunner.run(task, res -> {
            setLoading(false);

            Session.setUserId(res.userID());
            Session.setRole(res.role());
            Session.setEmail(email);

            String tok = res.token();
            if (tok != null && !tok.isBlank() && !tok.toLowerCase().startsWith("bearer ")) {
                tok = "Bearer " + tok;
            }
            Session.setToken(tok);


            AppNavigator.goDashboard();
        }, ex -> {
            setLoading(false);
            if (ex instanceof AuthApi.UnauthorizedException) {
                showError("Credenziali non valide (401).");
            } else {
                showError("Errore di connessione o server: " + ex.getMessage());
            }
        });
    }

    
    private void setLoading(boolean loading) {
        emailField.setDisable(loading);
        pswField.setDisable(loading);
        spinner.setVisible(loading);
        loginBtn.setDisable(loading);

        if (!loading) {
            validate();
        }
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
}
