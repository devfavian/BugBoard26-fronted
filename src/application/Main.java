package application;

import javafx.application.Application;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    private static ButtonBase hoverButton;

    @Override
    public void start(Stage primaryStage) {

        // scena unica + navigazione tra schermate cambiando root
        Scene scene = new Scene(new LoginView(), 1000, 700);   // <-- dimensione “standard”
        AppNavigator.init(scene);

        scene.getStylesheets().add(
            getClass().getResource("application.css").toExternalForm()
        );

        installButtonHover(scene);

        primaryStage.setTitle("BugBoard26");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.setResizable(true);                       // <-- NON più false
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void installButtonHover(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            ButtonBase btn = findButtonBase(e.getTarget());
            if (btn == hoverButton) return;
            if (hoverButton != null) {
                animateButton(hoverButton, 1.0);
            }
            hoverButton = btn;
            if (hoverButton != null && !hoverButton.isDisabled()) {
                animateButton(hoverButton, 1.02);
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (hoverButton != null) {
                animateButton(hoverButton, 1.0);
                hoverButton = null;
            }
        });
    }

    private static void animateButton(ButtonBase btn, double scale) {
        ScaleTransition st = (ScaleTransition) btn.getProperties().get("bb-hover-scale");
        if (st != null) {
            st.stop();
        }
        st = new ScaleTransition(Duration.millis(130), btn);
        st.setToX(scale);
        st.setToY(scale);
        st.setInterpolator(Interpolator.EASE_BOTH);
        btn.getProperties().put("bb-hover-scale", st);
        st.play();
    }

    private static ButtonBase findButtonBase(Object target) {
        if (!(target instanceof Node node)) return null;
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ButtonBase btn) return btn;
            cur = cur.getParent();
        }
        return null;
    }
}
