package net.botwithus.tasks.sorceressgarden;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.sorceressgarden.models.Waypoint;

/**
 * Handles movement between waypoints in Sorceress's Garden
 */
public class PathNavigator {
    private final CoaezUtility script;
    private final GuardianTracker guardianTracker;
    private final double MOVEMENT_THRESHOLD = 2.0; 
    private final long MAX_WAIT_TIME = 30000;
    
    public PathNavigator(CoaezUtility script, GuardianTracker guardianTracker) {
        this.script = script;
        this.guardianTracker = guardianTracker;
    }
    
    /**
     * Navigate to a specific waypoint
     */
    public boolean navigateToWaypoint(Waypoint waypoint, Area gardenArea) {
        if (waypoint == null) return false;
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return false;
        
        Coordinate targetPosition = waypoint.getPosition();
        if (targetPosition == null) return false;
        
        if (isAtPosition(player.getCoordinate(), targetPosition)) {
            return true;
        }
        
        ScriptConsole.println("Moving to waypoint: " + waypoint.getDescription() + " at " + targetPosition);
        return moveToPosition(targetPosition);
    }
    
    /**
     * Move to a specific position
     */
    public boolean moveToPosition(Coordinate targetPosition) {
        if (targetPosition == null) return false;
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return false;
        
        return walkToPosition(targetPosition);
    }
    
    /**
     * Walk to a position using standard movement
     */
    private boolean walkToPosition(Coordinate targetPosition) {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return false;
        Movement.walkTo(targetPosition.getX(), targetPosition.getY(), false);
        Execution.delayUntil(5000,() -> Client.getLocalPlayer().getCoordinate().equals(targetPosition));
        return true;
    }
    
    /**
     * Check if player is at a specific position
     */
    public boolean isAtPosition(Coordinate playerPosition, Coordinate targetPosition) {
        if (playerPosition == null || targetPosition == null) return false;
        return playerPosition.distanceTo(targetPosition) < MOVEMENT_THRESHOLD;
    }
    
    /**
     * Wait at a waypoint for the specified time
     */
    public boolean waitAtWaypoint(Waypoint waypoint, Area gardenArea) {
        if (waypoint == null) return false;
        
        int waitTime = waypoint.getWaitTime();
        if (waitTime <= 0) return true;
        
        ScriptConsole.println("Waiting at waypoint: " + waypoint.getDescription() + " for " + waitTime + "ms");
        
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitTime) {
            LocalPlayer player = Client.getLocalPlayer();
            if (player != null && !isAtPosition(player.getCoordinate(), waypoint.getPosition())) {
                ScriptConsole.println("Moved away from waypoint while waiting");
                return false;
            }
            
            guardianTracker.updateGuardianPositions(gardenArea);
            
            if (!guardianTracker.isSafeToMove(waypoint.getPosition())) {
                ScriptConsole.println("Guardian approaching, stopping wait");
                return false;
            }
            
            Execution.delay(100);
        }
        
        return true;
    }
    
    /**
     * Navigate through a series of waypoints
     */
    public boolean navigatePath(java.util.List<Waypoint> waypoints, Area gardenArea) {
        if (waypoints == null || waypoints.isEmpty()) return false;
        
        for (Waypoint waypoint : waypoints) {
            ScriptConsole.println("Navigating to waypoint: " + waypoint.getDescription());
            
            if (!navigateToWaypoint(waypoint, gardenArea)) {    
                ScriptConsole.println("Failed to reach waypoint: " + waypoint.getDescription());
                return false;
            }
            
            if (!waitAtWaypoint(waypoint, gardenArea)) {
                ScriptConsole.println("Failed to wait at waypoint: " + waypoint.getDescription());
                return false;
            }
            
            Execution.delay(100);
        }
        
        return true;
    }
    
    /**
     * Check if movement is possible to a target position
     */
    public boolean canMoveTo(Coordinate targetPosition) {
        if (targetPosition == null) return false;
        
        // Check if guardians are blocking the path
        return guardianTracker.isSafeToMove(targetPosition);
    }
    
    /**
     * Get the distance to a target position
     */
    public double getDistanceTo(Coordinate targetPosition) {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null || targetPosition == null) return Double.MAX_VALUE;
        
        return player.getCoordinate().distanceTo(targetPosition);
    }
    
    /**
     * Check if player is moving
     */
    public boolean isMoving() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && player.isMoving();
    }
    
    /**
     * Wait until player stops moving
     */
    public boolean waitUntilStopped(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!isMoving()) {
                return true;
            }
            Execution.delay(100);
        }
        
        return false;
    }
} 