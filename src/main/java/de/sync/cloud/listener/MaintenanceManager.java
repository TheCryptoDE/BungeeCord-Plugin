package de.sync.cloud.listener;

public class MaintenanceManager {
    private static boolean maintenance = false;

    public static boolean isMaintenance() {
        return maintenance;
    }

    public static void setMaintenance(boolean value) {
        maintenance = value;
    }
}
