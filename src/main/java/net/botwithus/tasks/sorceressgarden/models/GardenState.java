package net.botwithus.tasks.sorceressgarden.models;

/**
 * Enum representing the different states of garden execution
 */
public enum GardenState {
    IDLE("Idle"),
    ENTERING("Entering Garden"),
    NAVIGATING("Navigating to Tree"),
    HARVESTING("Harvesting Fruit"),
    RETURNING("Returning to Center"),
    COMPLETED("Completed"),
    FAILED("Failed");
    
    private final String displayName;
    
    GardenState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 