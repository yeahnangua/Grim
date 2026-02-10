package ac.grim.grimac.checks;

public enum SetbackMode {
    SETBACK,      // Normal: setback when VL exceeds threshold
    ALERT_ONLY,   // Detect + alert, but never setback
    DISABLED;     // Check completely disabled

    public static SetbackMode fromString(String value) {
        return switch (value.toLowerCase()) {
            case "alert-only" -> ALERT_ONLY;
            case "disabled" -> DISABLED;
            default -> SETBACK;
        };
    }
}
