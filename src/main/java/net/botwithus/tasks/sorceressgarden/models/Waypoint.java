package net.botwithus.tasks.sorceressgarden.models;

import net.botwithus.rs3.game.Coordinate;

/**
 * Model representing a waypoint in the garden navigation path
 */
public class Waypoint {
    private final Coordinate position;
    private final String description;
    private final int waitTime;
    private final boolean isSafeZone;
    
    public Waypoint(Coordinate position, String description) {
        this(position, description, 0, false);
    }
    
    public Waypoint(Coordinate position, String description, int waitTime) {
        this(position, description, waitTime, false);
    }
    
    public Waypoint(Coordinate position, String description, int waitTime, boolean isSafeZone) {
        this.position = position;
        this.description = description;
        this.waitTime = waitTime;
        this.isSafeZone = isSafeZone;
    }
    
    public Coordinate getPosition() {
        return position;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getWaitTime() {
        return waitTime;
    }
    
    public boolean isSafeZone() {
        return isSafeZone;
    }
    
    public double distanceTo(Coordinate otherPosition) {
        if (position == null || otherPosition == null) {
            return Double.MAX_VALUE;
        }
        return position.distanceTo(otherPosition);
    }
    
    public boolean isAtPosition(Coordinate playerPosition) {
        if (position == null || playerPosition == null) {
            return false;
        }
        return position.distanceTo(playerPosition) < 2.0; // Within 2 tiles
    }
    
    @Override
    public String toString() {
        return String.format("Waypoint{position=%s, description='%s', waitTime=%d, safeZone=%s}", 
                           position, description, waitTime, isSafeZone);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Waypoint waypoint = (Waypoint) obj;
        return position != null && position.equals(waypoint.position);
    }
    
    @Override
    public int hashCode() {
        return position != null ? position.hashCode() : 0;
    }
} 