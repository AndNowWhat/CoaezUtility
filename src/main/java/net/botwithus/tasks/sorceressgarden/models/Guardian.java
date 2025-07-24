package net.botwithus.tasks.sorceressgarden.models;

import net.botwithus.rs3.game.Coordinate;
import net.botwithus.tasks.sorceressgarden.models.NPCDirection.Direction;

/**
 * Model representing an elemental guardian in Sorceress's Garden
 */
public class Guardian {
    private final int id;
    private final String name;
    private Coordinate currentPosition;
    private float direction1;
    private float direction2;
    private GuardianState state;
    private long lastUpdateTime;
    
    public enum GuardianState {
        PATROLLING,
        IDLE,
        MOVING,
        ALERTED
    }
    
    public Guardian(int id, String name, Coordinate initialPosition) {
        this.id = id;
        this.name = name;
        this.currentPosition = initialPosition;
        this.direction1 = 0.0f;
        this.direction2 = 0.0f;
        this.state = GuardianState.PATROLLING;
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
    
    public float getDirection1() {
        return direction1;
    }
    
    public void setDirection1(float direction1) {
        this.direction1 = direction1;
    }
    
    public float getDirection2() {
        return direction2;
    }
    
    public void setDirection2(float direction2) {
        this.direction2 = direction2;
    }
    
    public void setRotation(float direction1, float direction2) {
        this.direction1 = direction1;
        this.direction2 = direction2;
    }
    
    /**
     * Get the current direction as a Direction enum
     */
    public Direction getCurrentDirection() {
        return NPCDirection.Direction.getFacingDirection(direction1, direction2);
    }
    
    /**
     * Check if the guardian is facing a specific direction
     */
    public boolean isFacingDirection(Direction targetDirection) {
        Direction currentDirection = getCurrentDirection();
        return currentDirection == targetDirection;
    }
    
    public GuardianState getState() {
        return state;
    }
    
    public void setState(GuardianState state) {
        this.state = state;
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
    
    public boolean isFacing(Coordinate position) {
        if (currentPosition == null || position == null) {
            return false;
        }
        
        double dx = position.getX() - currentPosition.getX();
        double dy = position.getY() - currentPosition.getY();
        double angle = Math.atan2(dy, dx) * 180 / Math.PI;
        
        if (angle < 0) angle += 360;
        
        double guardianAngle = Math.toDegrees(direction1);
        if (guardianAngle < 0) guardianAngle += 360;
        
        double rotationDiff = Math.abs(angle - guardianAngle);
        if (rotationDiff > 180) rotationDiff = 360 - rotationDiff;
        
        return rotationDiff <= 45;
    }
    
    @Override
    public String toString() {
        return String.format("Guardian{id=%d, name='%s', position=%s, direction1=%.2f, direction2=%.2f, state=%s}", 
                           id, name, currentPosition, direction1, direction2, state);
    }
} 