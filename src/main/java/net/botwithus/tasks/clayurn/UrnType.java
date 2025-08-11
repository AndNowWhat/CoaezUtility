package net.botwithus.tasks.clayurn;

import java.util.Objects;

/**
 * Represents a specific type of urn that can be crafted
 */
public class UrnType {
    private final int id;
    private final String displayName;
    private final UrnCategory category;
    
    public UrnType(int id, String displayName, UrnCategory category) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
    }
    
    public int getId() { 
        return id; 
    }
    
    public String getDisplayName() { 
        return displayName; 
    }
    
    public UrnCategory getCategory() { 
        return category; 
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UrnType urnType = (UrnType) obj;
        return id == urnType.id && Objects.equals(displayName, urnType.displayName) && 
               Objects.equals(category, urnType.category);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, category);
    }
}