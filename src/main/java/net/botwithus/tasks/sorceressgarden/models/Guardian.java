package net.botwithus.tasks.sorceressgarden.models;

import net.botwithus.rs3.game.Coordinate;

/**
 * Model representing an elemental guardian in Sorceress's Garden
 */
public class Guardian {
    private final int id;
    private final String name;
    private Coordinate currentPosition;
    private long lastUpdateTime;
    
    public Guardian(int id, String name, Coordinate initialPosition) {
        this.id = id;
        this.name = name;
        this.currentPosition = initialPosition;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Coordinate getCurrentPosition() {
        return currentPosition;
    }
    
    public void setCurrentPosition(Coordinate position) {
        this.currentPosition = position;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public boolean isActive() {
        return System.currentTimeMillis() - lastUpdateTime < 5000; 
    }
    
    public double distanceTo(Coordinate position) {
        if (currentPosition == null || position == null) {
            return Double.MAX_VALUE;
        }
        return currentPosition.distanceTo(position);
    }
    
    @Override
    public String toString() {
        return String.format("Guardian{id=%d, name='%s', position=%s}", 
                           id, name, currentPosition);
    }
} 