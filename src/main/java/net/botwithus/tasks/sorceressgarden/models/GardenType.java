package net.botwithus.tasks.sorceressgarden.models;

/**
 * Enum representing the four seasonal gardens in Sorceress's Garden
 */
public enum GardenType {
    WINTER("Winter Garden", 1, 5),
    SPRING("Spring Garden", 25, 4),
    SUMMER("Summer Garden", 65, 2),
    AUTUMN("Autumn Garden", 45, 3);
    
    private final String displayName;
    private final int requiredThievingLevel;
    private final int fruitsPerJuice;
    
    GardenType(String displayName, int requiredThievingLevel, int fruitsPerJuice) {
        this.displayName = displayName;
        this.requiredThievingLevel = requiredThievingLevel;
        this.fruitsPerJuice = fruitsPerJuice;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getRequiredThievingLevel() {
        return requiredThievingLevel;
    }
    
    public int getFruitsPerJuice() {
        return fruitsPerJuice;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 