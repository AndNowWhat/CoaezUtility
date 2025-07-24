package net.botwithus.tasks.sorceressgarden.models;

import net.botwithus.rs3.game.Coordinate;

/**
 * Represents a guardian's position and direction for waiting logic
 * Makes it easy to specify guardian positions using coordinates and cardinal directions
 */
public class GuardianPosition {
    private final int guardianId;
    private final Coordinate position;
    private final NPCDirection.Direction direction;
    
    public GuardianPosition(int guardianId, Coordinate position, NPCDirection.Direction direction) {
        this.guardianId = guardianId;
        this.position = position;
        this.direction = direction;
    }
    
    public GuardianPosition(int guardianId, int x, int y, int z, NPCDirection.Direction direction) {
        this.guardianId = guardianId;
        this.position = new Coordinate(x, y, z);
        this.direction = direction;
    }
    
    public int getGuardianId() {
        return guardianId;
    }
    
    public Coordinate getPosition() {
        return position;
    }
    
    public NPCDirection.Direction getDirection() {
        return direction;
    }
    
    @Override
    public String toString() {
        return String.format("GuardianPosition{guardianId=%d, position=%s, direction=%s}", 
                           guardianId, position, direction);
    }
} 