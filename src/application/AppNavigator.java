package application;

import javafx.scene.Scene;

public final class AppNavigator {
    private static Scene scene;

    private AppNavigator() {}

    public static void init(Scene s) {
        scene = s;
    }

    public static void goLogin() {
        scene.setRoot(new LoginView());
    }

    public static void goDashboard() {
        scene.setRoot(new DashboardView());
    }

    public static void goAccount() {
        scene.setRoot(new AccountView());
    }

    public static void goReportIssue() {
        scene.setRoot(new ReportIssueView());
    }

    public static void goAdminCreateUser() {
        scene.setRoot(new AdminCreateUserView());
    }

    public static void goModifyIssue(IssueItem item) {
        scene.setRoot(new ModifyIssueView(item));
    }


    public static void goViewIssues() {
        scene.setRoot(new PlaceholderView("Visualizza Issue"));
    }

    public static void goEditIssue() {
        scene.setRoot(new PlaceholderView("Modifica Issue"));
    }

    public static void goIssuesList() {
        scene.setRoot(new IssuesListView());
    }

}
