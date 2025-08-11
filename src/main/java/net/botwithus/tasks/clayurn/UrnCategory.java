package net.botwithus.tasks.clayurn;

import java.util.Objects;

/**
 * Represents a category of urns (e.g., Cooking Urns, Mining Urns)
 */
public class UrnCategory {
    private final int index;
    private final String displayName;
    private final int enumId;

    public UrnCategory(int index, String displayName, int enumId) {
        this.index = index;
        this.displayName = displayName;
        this.enumId = enumId;
    }

    public int getIndex() { 
        return index; 
    }
    
    public String getDisplayName() { 
        return displayName; 
    }
    
    public int getEnumId() { 
        return enumId; 
    }

    @Override
    public String toString() {
        return displayName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UrnCategory that = (UrnCategory) obj;
        return index == that.index && enumId == that.enumId && Objects.equals(displayName, that.displayName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(index, displayName, enumId);
    }
}