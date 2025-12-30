package application.view.common;

/**
 * UI options for issue types mapped to API values.
 */
public enum IssueTypeOption {
    QUESTION("Question", "QUESTION"),
    BUG("Bug", "BUG"),
    DOCUMENTATION("Documentation", "DOCUMENTATION"),
    FEATURE("Feature", "FEATURE");

    private final String label;
    private final String api;

    IssueTypeOption(String label, String api) {
        this.label = label;
        this.api = api;
    }

    public String apiValue() {
        return api;
    }

    public static IssueTypeOption fromApi(String api) {
        if (api != null) {
            for (IssueTypeOption t : values()) {
                if (t.api.equalsIgnoreCase(api)) return t;
            }
        }
        return BUG;
    }

    @Override
    public String toString() {
        return label;
    }
}
