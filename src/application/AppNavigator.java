package application;

import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;

public final class AppNavigator {
    private static Scene scene;

    private AppNavigator() {}

    public static void init(Scene s) {
        scene = s;
    }

    public static void goLogin() {
        setRoot(new LoginView());
    }

    public static void goDashboard() {
        setRoot(new DashboardView());
    }

    public static void goAccount() {
        setRoot(new AccountView());
    }

    public static void goReportIssue() {
        setRoot(new ReportIssueView());
    }

    public static void goAdminCreateUser() {
        setRoot(new AdminCreateUserView());
    }

    public static void goModifyIssue(IssueItem item) {
        setRoot(new ModifyIssueView(item));
    }


    public static void goViewIssues() {
        setRoot(new PlaceholderView("Visualizza Issue"));
    }

    public static void goEditIssue() {
        setRoot(new PlaceholderView("Modifica Issue"));
    }

    public static void goIssuesList() {
        setRoot(new IssuesListView());
    }

    private static void setRoot(Parent root) {
        if (scene == null) {
            return;
        }
        root.setOpacity(0);
        scene.setRoot(root);
        FadeTransition ft = new FadeTransition(Duration.millis(180), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}
