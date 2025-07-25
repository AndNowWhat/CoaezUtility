package net.botwithus.tasks.sorceressgarden.gardens;

import java.util.Arrays;
import java.util.List;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.sorceressgarden.GuardianTracker;
import net.botwithus.tasks.sorceressgarden.PathNavigator;
import net.botwithus.tasks.sorceressgarden.models.GardenPath;
import net.botwithus.tasks.sorceressgarden.models.GardenState;
import net.botwithus.tasks.sorceressgarden.models.GardenType;
import net.botwithus.tasks.sorceressgarden.models.Waypoint;

/**
 * Abstract base class for all garden implementations
 */
public abstract class BaseGarden {
    protected final CoaezUtility script;
    protected final GuardianTracker guardianTracker;
    protected final PathNavigator pathNavigator;
    
    protected final GardenType gardenType;
    protected final int doorId;
    protected final Coordinate startPosition;
    protected final Coordinate treePosition;
    protected final List<Integer> guardianIds;
    protected final Area gardenArea;
    
    protected GardenState currentState;
    protected GardenPath gardenPath;
    protected boolean isCompleted;
    protected boolean hasFailed;
    protected boolean isStopped;
    protected long lastCompletionTime;
    protected static final long COMPLETION_COOLDOWN = 60000;
    
    public BaseGarden(CoaezUtility script, GardenType gardenType, int doorId, 
                     Coordinate startPosition, Coordinate treePosition, int[] guardianIds, Area gardenArea) {
        this.script = script;
        this.gardenType = gardenType;
        this.doorId = doorId;
        this.startPosition = startPosition;
        this.treePosition = treePosition;
        this.guardianIds = Arrays.stream(guardianIds).boxed().toList();
        this.gardenArea = gardenArea;
        
        this.guardianTracker = new GuardianTracker(script);
        this.pathNavigator = new PathNavigator(script, guardianTracker);
        this.currentState = GardenState.IDLE;
        this.isCompleted = false;
        this.hasFailed = false;
        this.isStopped = false;
        this.lastCompletionTime = 0;
        
        initializeGardenPath();
        initializeGuardians();
    }
    
    /**
     * Main execution method
     */
    public void execute() {
        if (isStopped) return;
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null || !gardenArea.contains(player.getCoordinate())) {
            if (currentState == GardenState.NAVIGATING || currentState == GardenState.HARVESTING || currentState == GardenState.RETURNING) {
                ScriptConsole.println("[FAILSAFE] Player is no longer in " + gardenType.getDisplayName() + " area (caught/teleported out). Resetting run.");
                reset();
            }
        }
        try {
            switch (currentState) {
                case IDLE -> currentState = GardenState.ENTERING;
                    
                case ENTERING -> {
                    if (enterGarden()) {
                        currentState = GardenState.NAVIGATING;
                        ScriptConsole.println("Successfully entered " + gardenType.getDisplayName());
                    }
                }
                    
                case NAVIGATING -> {
                    if (navigateToTree()) {
                        currentState = GardenState.HARVESTING;
                        ScriptConsole.println("Reached tree in " + gardenType.getDisplayName());
                    }
                }
                    
                case HARVESTING -> {
                    if (harvestFruit()) {
                        currentState = GardenState.RETURNING;
                        ScriptConsole.println("Harvested fruit from " + gardenType.getDisplayName());
                    }
                }
                    
                case RETURNING -> {
                    if (returnToCenter()) {
                        currentState = GardenState.COMPLETED;
                        isCompleted = true;
                        lastCompletionTime = System.currentTimeMillis();
                        ScriptConsole.println("Completed " + gardenType.getDisplayName());
                    }
                }
                    
                case COMPLETED, FAILED -> {
                }
            }
        } catch (Exception e) {
            ScriptConsole.println("Error in " + gardenType.getDisplayName() + ": " + e.getMessage());
            currentState = GardenState.FAILED;
            hasFailed = true;
        }
    }
    
    /**
     * Initialize the garden path with waypoints
     */
    protected void initializeGardenPath() {
        this.gardenPath = new GardenPath(gardenType.getDisplayName());
        List<Waypoint> waypoints = getWaypoints();
        for (Waypoint waypoint : waypoints) {
            gardenPath.addWaypoint(waypoint);
        }
    }
    
    /**
     * Initialize guardians for this garden
     */
    protected void initializeGuardians() {
        ScriptConsole.println("Initializing " + guardianIds.size() + " guardians for " + gardenType.getDisplayName());
        for (Integer guardianId : guardianIds) {
            guardianTracker.addGuardian(guardianId, "Guardian " + guardianId, null);
        }
        ScriptConsole.println("Guardian initialization complete for " + gardenType.getDisplayName());
    }
    
    /**
     * Enter the garden through the door
     */
    protected boolean enterGarden() {
        ScriptConsole.println("Attempting to enter " + gardenType.getDisplayName());
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player != null && startPosition != null) {
            return pathNavigator.isAtPosition(player.getCoordinate(), startPosition);
        }
        
        return false;
    }
    
    /**
     * Navigate to the tree
     */
    protected boolean navigateToTree() {
        if (gardenPath == null) return false;
        
        guardianTracker.updateGuardianPositions(gardenArea);
        
        return pathNavigator.navigatePath(gardenPath.getWaypoints(), gardenArea);
    }
    
    /**
     * Harvest fruit from the tree
     */
    protected boolean harvestFruit() {
        ScriptConsole.println("Harvesting fruit from " + gardenType.getDisplayName());
        Execution.delay(script.getRandom().nextInt(1000, 2000));
        return true;
    }
    
    /**
     * Return to the central garden
     */
    protected boolean returnToCenter() {
        ScriptConsole.println("Returning to center from " + gardenType.getDisplayName());
        Execution.delay(script.getRandom().nextInt(1000, 2000));
        
        return true;
    }
    
    /**
     * Get waypoints for this garden - to be implemented by subclasses
     */
    protected abstract List<Waypoint> getWaypoints();
    
    /**
     * Check if guardian is safe at a waypoint - to be implemented by subclasses
     */
    protected boolean isGuardianSafe(Waypoint waypoint) {
        return guardianTracker.isSafeToMove(waypoint.getPosition());
    }
    
    // Getters
    public GardenType getGardenType() {
        return gardenType;
    }
    
    public int getDoorId() {
        return doorId;
    }
    
    public Coordinate getStartPosition() {
        return startPosition;
    }
    
    public Coordinate getTreePosition() {
        return treePosition;
    }
    
    public List<Integer> getGuardianIds() {
        return guardianIds;
    }
    
    public GardenState getCurrentState() {
        return currentState;
    }
    
    public GardenPath getGardenPath() {
        return gardenPath;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public boolean hasFailed() {
        return hasFailed;
    }
    
    public boolean isRecentlyCompleted() {
        return System.currentTimeMillis() - lastCompletionTime < COMPLETION_COOLDOWN;
    }
    
    public double getProgressPercentage() {
        if (gardenPath == null) return 0.0;
        return gardenPath.getProgressPercentage();
    }
    
    public void reset() {
        currentState = GardenState.IDLE;
        isCompleted = false;
        hasFailed = false;
        isStopped = false;
        if (gardenPath != null) {
            gardenPath.reset();
        }
        guardianTracker.clear();
        initializeGuardians();
    }
    
    public void resetCompletion() {
        isCompleted = false;
        lastCompletionTime = 0;
    }
    
    public void markAsCompleted() {
        isCompleted = true;
        lastCompletionTime = System.currentTimeMillis();
    }
    
    public void stop() {
        isStopped = true;
        currentState = GardenState.IDLE;
    }
    
    @Override
    public String toString() {
        return String.format("BaseGarden{gardenType=%s, state=%s, progress=%.1f%%}", 
                           gardenType, currentState, getProgressPercentage());
    }
} 