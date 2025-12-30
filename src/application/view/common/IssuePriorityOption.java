package application.view.common;

/**
 * UI options for issue priority mapped to API values.
 */
public enum IssuePriorityOption {
    NONE("Nessuna", null),
    LOW("Bassa", "LOW"),
    MEDIUM("Media", "MEDIUM"),
    HIGH("Alta", "HIGH");

    private final String label;
    private final String api;

    IssuePriorityOption(String label, String api) {
        this.label = label;
        this.api = api;
    }

    public String apiValueOrNull() {
        return api;
    }

    public static IssuePriorityOption fromApi(String api) {
        if (api != null) {
            for (IssuePriorityOption p : values()) {
                if (p.api != null && p.api.equalsIgnoreCase(api)) return p;
            }
        }
        return NONE;
    }

    @Override
    public String toString() {
        return label;
    }
}
