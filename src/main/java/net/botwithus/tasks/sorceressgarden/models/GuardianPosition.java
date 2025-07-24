package net.botwithus.tasks.sorceressgarden.models;

import net.botwithus.rs3.game.Coordinate;

/**
 * Represents a guardian's position and required moving direction for waiting logic.
 * The direction field now means the required MOVING direction (based on coordinate deltas), not facing direction.
 */
public class GuardianPosition {
    private final int guardianId;
    private final Coordinate position;
    private final NPCDirection.Direction movingDirection;
    
    public GuardianPosition(int guardianId, Coordinate position, NPCDirection.Direction movingDirection) {
        this.guardianId = guardianId;
        this.position = position;
        this.movingDirection = movingDirection;
    }
    
    public GuardianPosition(int guardianId, int x, int y, int z, NPCDirection.Direction movingDirection) {
        this.guardianId = guardianId;
        this.position = new Coordinate(x, y, z);
        this.movingDirection = movingDirection;
    }
    
    public int getGuardianId() {
        return guardianId;
    }
    
    public Coordinate getPosition() {
        return position;
    }
    
    public NPCDirection.Direction getMovingDirection() {
        return movingDirection;
    }
    
    @Override
    public String toString() {
        return String.format("GuardianPosition{guardianId=%d, position=%s, movingDirection=%s}", 
                           guardianId, position, movingDirection);
    }
} 