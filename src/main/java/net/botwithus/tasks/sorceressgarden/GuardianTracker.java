package net.botwithus.tasks.sorceressgarden;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.sorceressgarden.models.Guardian;
import net.botwithus.tasks.sorceressgarden.models.GuardianPosition;
import net.botwithus.tasks.sorceressgarden.models.GuardianRequirement;
import net.botwithus.tasks.sorceressgarden.models.NPCDirection;
import net.botwithus.tasks.sorceressgarden.models.NPCDirection.Direction;

/**
 * Tracks guardian positions and movements in Sorceress's Garden
 */
public class GuardianTracker {
    private final CoaezUtility script;
    private final Map<Integer, Guardian> activeGuardians;
    private final List<Coordinate> guardianPositions;
    private final double SAFE_DISTANCE = 3.0; 
    private final int UPDATE_INTERVAL = 100;
    private long lastUpdateTime;
    private final Map<Integer, Coordinate> previousPositions = new ConcurrentHashMap<>();
    
    public GuardianTracker(CoaezUtility script) {
        this.script = script;
        this.activeGuardians = new ConcurrentHashMap<>();
        this.guardianPositions = new ArrayList<>();
        this.lastUpdateTime = 0;
    }
    
    /**
     * Update guardian positions and states
     */
    public void updateGuardianPositions(Area gardenArea) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }
        
        guardianPositions.clear();
        
        for (Guardian guardian : activeGuardians.values()) {
            Coordinate oldPosition = guardian.getCurrentPosition();
            updateGuardian(guardian, gardenArea);
            if (guardian.getCurrentPosition() != null) {
                guardianPositions.add(guardian.getCurrentPosition());
                // Track previous position for movement direction
                if (oldPosition != null) {
                    previousPositions.put(guardian.getId(), oldPosition);
                }
            }
        }
        
        int removedCount = 0;
        for (Map.Entry<Integer, Guardian> entry : activeGuardians.entrySet()) {
            if (!entry.getValue().isActive()) {
                ScriptConsole.println("Removing inactive guardian: " + entry.getKey());
                removedCount++;
            }
        }
        activeGuardians.entrySet().removeIf(entry -> !entry.getValue().isActive());
        
        if (removedCount > 0) {
            ScriptConsole.println("Removed " + removedCount + " inactive guardians");
        }
        
        ScriptConsole.println("Updated " + activeGuardians.size() + " active guardians");
        lastUpdateTime = currentTime;
    }
    
    /**
     * Update a specific guardian's position and state
     */
    private void updateGuardian(Guardian guardian, Area gardenArea) {
        Npc npc = NpcQuery.newQuery()
            .byType(guardian.getId())
            .inside(gardenArea)
            .results()
            .first();
            
        if (npc != null) {
            Coordinate oldPosition = guardian.getCurrentPosition();
            guardian.setCurrentPosition(npc.getServerCoordinate());
            
            if (oldPosition != null && oldPosition.distanceTo(npc.getServerCoordinate()) > 2.0) {
                ScriptConsole.println("Guardian " + guardian.getId() + " moved from " + oldPosition + 
                    " to " + npc.getServerCoordinate());
            }
        } else {
            ScriptConsole.println("Guardian " + guardian.getId() + " not found in game world");
        }
    }
    
    /**
     * Add a guardian to track
     */
    public void addGuardian(int guardianId, String guardianName, Coordinate initialPosition) {
        Guardian guardian = new Guardian(guardianId, guardianName, initialPosition);
        activeGuardians.put(guardianId, guardian);
        ScriptConsole.println("Added guardian " + guardianId + " (" + guardianName + ") to tracking at " + initialPosition);
    }
    
    /**
     * Remove a guardian from tracking
     */
    public void removeGuardian(int guardianId) {
        activeGuardians.remove(guardianId);
    }
    
    /**
     * Check if a guardian is at a specific position
     */
    public boolean isGuardianAtPosition(Coordinate position) {
        return guardianPositions.stream()
            .anyMatch(pos -> pos.distanceTo(position) < SAFE_DISTANCE);
    }
    
    /**
     * Check if it's safe to move to a target position
     */
    public boolean isSafeToMove(Coordinate targetPosition) {
        if (targetPosition == null) return false;
        
        for (Guardian guardian : activeGuardians.values()) {
            if (guardian.getCurrentPosition() != null) {
                double distance = guardian.getCurrentPosition().distanceTo(targetPosition);
                if (distance < SAFE_DISTANCE) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a guardian is at a specific position
     */
    public boolean isGuardianAtPosition(int guardianId, Coordinate position, double tolerance) {
        Guardian guardian = activeGuardians.get(guardianId);
        if (guardian == null) return false;
        
        if (guardian.getCurrentPosition() == null || position == null) return false;
        return guardian.getCurrentPosition().distanceTo(position) <= tolerance;
    }
    
    /**
     * Wait until a guardian reaches a specific position
     */
    public boolean waitForGuardianPosition(int guardianId, Coordinate position, long timeoutMs, Area gardenArea) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            updateGuardianPositions(gardenArea);
            
            if (isGuardianAtPosition(guardianId, position, 1.0)) {
                return true;
            }
            
            Execution.delay(100);
        }
        
        return false;
    }
    
    /**
     * Wait for a guardian to reach a specific position using GuardianPosition
     */
    public boolean waitForGuardianPosition(GuardianPosition guardianPosition, long timeoutMs, Area gardenArea) {
        Guardian guardian = activeGuardians.get(guardianPosition.getGuardianId());
        if (guardian == null) return false;
        
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            updateGuardianPositions(gardenArea);
            
            if (guardian.getCurrentPosition() == null || 
                guardian.getCurrentPosition().distanceTo(guardianPosition.getPosition()) > 1.0) {
                Execution.delay(100);
                continue;
            }
            
            // Check movement direction if required
            if (guardianPosition.getMovingDirection() != null) {
                Coordinate prev = previousPositions.get(guardian.getId());
                Coordinate curr = guardian.getCurrentPosition();
                if (prev != null && curr != null) {
                    int dx = curr.getX() - prev.getX();
                    int dy = curr.getY() - prev.getY();
                    Direction required = guardianPosition.getMovingDirection();
                    boolean movingCorrect = false;
                    switch (required) {
                        case NORTH: movingCorrect = (dy > 0); break;
                        case SOUTH: movingCorrect = (dy < 0); break;
                        case EAST:  movingCorrect = (dx > 0); break;
                        case WEST:  movingCorrect = (dx < 0); break;
                        default: break;
                    }
                    if (movingCorrect) {
                        return true;
                    }
                }
            } else {
                return true;
            }
            
            Execution.delay(100);
        }
        
        return false;
    }
    
    /**
     * Wait for multiple guardians to reach their positions
     */
    public boolean waitForGuardianPositions(List<GuardianPosition> guardianPositions, long timeoutMs, Area gardenArea) {
        long startTime = System.currentTimeMillis();
        ScriptConsole.println("Starting to wait for " + guardianPositions.size() + " guardians to reach their positions");
        ScriptConsole.println("Active guardians in tracking: " + activeGuardians.keySet());
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            updateGuardianPositions(gardenArea);
            
            boolean allGuardiansInPosition = true;
            for (GuardianPosition guardianPosition : guardianPositions) {
                Guardian guardian = activeGuardians.get(guardianPosition.getGuardianId());
                if (guardian == null) {
                    ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + " not found in active guardians");
                    ScriptConsole.println("Available guardians: " + activeGuardians.keySet());
                    allGuardiansInPosition = false;
                    break;
                }
                
                // Check position
                if (guardian.getCurrentPosition() == null) {
                    ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + " position is null");
                    allGuardiansInPosition = false;
                    break;
                }
                
                double distance = guardian.getCurrentPosition().distanceTo(guardianPosition.getPosition());
                ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + 
                    " distance check: " + String.format("%.2f", distance) + " (max: 1.0)");
                if (distance > 1.0) {
                    ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + 
                        " at " + guardian.getCurrentPosition() + 
                        " but needs to be at " + guardianPosition.getPosition() + 
                        " (distance: " + String.format("%.2f", distance) + ")");
                    allGuardiansInPosition = false;
                    break;
                }
                
                // Check movement direction if required
                if (guardianPosition.getMovingDirection() != null) {
                    Coordinate prev = previousPositions.get(guardian.getId());
                    Coordinate curr = guardian.getCurrentPosition();
                    if (prev != null && curr != null) {
                        int dx = curr.getX() - prev.getX();
                        int dy = curr.getY() - prev.getY();
                        Direction required = guardianPosition.getMovingDirection();
                        boolean movingCorrect = false;
                        switch (required) {
                            case NORTH: movingCorrect = (dy > 0); break;
                            case SOUTH: movingCorrect = (dy < 0); break;
                            case EAST:  movingCorrect = (dx > 0); break;
                            case WEST:  movingCorrect = (dx < 0); break;
                            default: break;
                        }
                        if (!movingCorrect) {
                            ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + 
                                " is not moving " + required + " (dx=" + dx + ", dy=" + dy + ")");
                            allGuardiansInPosition = false;
                            break;
                        }
                        ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + 
                            " is moving " + required + " ✓");
                    } else {
                        ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + 
                            " movement direction check failed - missing previous position");
                        allGuardiansInPosition = false;
                        break;
                    }
                }
                
                ScriptConsole.println("Guardian " + guardianPosition.getGuardianId() + 
                    " is in correct position ✓");
            }
            
            if (allGuardiansInPosition) {
                ScriptConsole.println("All guardians are in position! Proceeding...");
                return true;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed % 2000 < 50) {
                ScriptConsole.println("Still waiting for guardians... (" + (elapsed / 1000) + "s elapsed)");
            }
            
            Execution.delay(100);
        }
        
        ScriptConsole.println("Timeout waiting for guardians to reach positions after " + (timeoutMs / 1000) + " seconds");
        return false;
    }
    
    /**
     * Wait for multiple guardians to meet their requirements (exact positions or avoid positions)
     */
    public boolean waitForGuardianRequirements(List<GuardianRequirement> guardianRequirements, long timeoutMs, Area gardenArea) {
        long startTime = System.currentTimeMillis();
        ScriptConsole.println("Starting to wait for " + guardianRequirements.size() + " guardians to reach their positions");
        ScriptConsole.println("Active guardians in tracking: " + activeGuardians.keySet());
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            updateGuardianPositions(gardenArea);
            
            boolean allGuardiansInPosition = true;
            OUTER:
            for (GuardianRequirement guardianRequirement : guardianRequirements) {
                if (guardianRequirement.getType() == GuardianRequirement.RequirementType.MIN_DISTANCE) {
                    Guardian guardian = getGuardianById(guardianRequirement.getGuardianId());
                    net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer player = net.botwithus.rs3.game.Client.getLocalPlayer();
                    if (guardian == null || guardian.getCurrentPosition() == null || player == null) {
                        allGuardiansInPosition = false;
                        break;
                    }
                    double distance = guardian.getCurrentPosition().distanceTo(player.getServerCoordinate());
                    if (distance < guardianRequirement.getMinDistance()) {
                        allGuardiansInPosition = false;
                        break;
                    }
                }
                Guardian guardian = activeGuardians.get(guardianRequirement.getGuardianId());
                if (guardian == null) {
                    ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() + " not found in active guardians");
                    ScriptConsole.println("Available guardians: " + activeGuardians.keySet());
                    allGuardiansInPosition = false;
                    break;
                }
                if (null != guardianRequirement.getType()) {
                    switch (guardianRequirement.getType()) {
                        case EXACT_POSITION -> {
                            // Check exact position
                            if (guardian.getCurrentPosition() == null) {
                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() + " position is null");
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                            double distance = guardian.getCurrentPosition().distanceTo(guardianRequirement.getExactPosition());
                            ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                    " distance check: " + String.format("%.2f", distance) + " (max: " + guardianRequirement.getTolerance() + ")");
                            if (distance > guardianRequirement.getTolerance()) {
                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                        " at " + guardian.getCurrentPosition() +
                                        " but needs to be at " + guardianRequirement.getExactPosition() +
                                        " (distance: " + String.format("%.2f", distance) + ")");
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                            if (guardianRequirement.getExactDirection() != null) {
                                Coordinate prev = previousPositions.get(guardian.getId());
                                Coordinate curr = guardian.getCurrentPosition();
                                if (prev != null && curr != null) {
                                    int dx = curr.getX() - prev.getX();
                                    int dy = curr.getY() - prev.getY();
                                    boolean movingCorrect = false;
                                    switch (guardianRequirement.getExactDirection()) {
                                        case EAST -> movingCorrect = (dx > 0);
                                        case WEST -> movingCorrect = (dx < 0);
                                        case NORTH -> movingCorrect = (dy < 0);
                                        case SOUTH -> movingCorrect = (dy > 0);
                                        case NORTH_EAST -> movingCorrect = (dx > 0 && dy < 0);
                                        case NORTH_WEST -> movingCorrect = (dx < 0 && dy < 0);
                                        case SOUTH_EAST -> movingCorrect = (dx > 0 && dy > 0);
                                        case SOUTH_WEST -> movingCorrect = (dx < 0 && dy > 0);
                                        default -> throw new IllegalArgumentException("Unexpected value: " + guardianRequirement.getExactDirection());
                                    }       if (!movingCorrect) {
                                        ScriptConsole.println("Guardian " + guardian.getId() + " is not moving " + guardianRequirement.getExactDirection() + " (dx=" + dx + ", dy=" + dy + ")");
                                        allGuardiansInPosition = false;
                                        break OUTER;
                                    }
                                }
                            }
                            ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                    " meets EXACT_POSITION requirement ✓");
                        }
                        case MULTIPLE_POSITIONS -> {
                            // Check that guardian is at ANY of the valid positions
                            if (guardian.getCurrentPosition() == null) {
                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() + " position is null");
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                            boolean isAtValidPosition = false;
                            for (GuardianPosition validPos : guardianRequirement.getValidPositions()) {
                                double distance = guardian.getCurrentPosition().distanceTo(validPos.getPosition());
                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                        " checking position " + validPos.getPosition() +
                                        " (distance: " + String.format("%.2f", distance) + ", max: " + guardianRequirement.getTolerance() + ")");
                                
                                if (distance <= guardianRequirement.getTolerance()) {
                                    // Check movement direction if required
                                    if (validPos.getMovingDirection() != null) {
                                        Coordinate prev = previousPositions.get(guardian.getId());
                                        Coordinate curr = guardian.getCurrentPosition();
                                        if (prev != null && curr != null) {
                                            int dx = curr.getX() - prev.getX();
                                            int dy = curr.getY() - prev.getY();
                                            Direction required = validPos.getMovingDirection();
                                            boolean movingCorrect = false;
                                            switch (required) {
                                                case NORTH: movingCorrect = (dy > 0); break;
                                                case SOUTH: movingCorrect = (dy < 0); break;
                                                case EAST:  movingCorrect = (dx > 0); break;
                                                case WEST:  movingCorrect = (dx < 0); break;
                                                default: break;
                                            }
                                            if (movingCorrect) {
                                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                                        " meets MULTIPLE_POSITIONS requirement at " + validPos.getPosition() +
                                                        " moving " + validPos.getMovingDirection() + " ✓");
                                                isAtValidPosition = true;
                                                break;
                                            } else {
                                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                                        " at correct position but not moving " + validPos.getMovingDirection() +
                                                        " (dx=" + dx + ", dy=" + dy + ")");
                                            }
                                        } else {
                                            ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                                    " movement direction check failed - missing previous position");
                                        }
                                    } else {
                                        ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                                " meets MULTIPLE_POSITIONS requirement at " + validPos.getPosition() + " ✓");
                                        isAtValidPosition = true;
                                        break;
                                    }
                                }
                            }       if (!isAtValidPosition) {
                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                        " at " + guardian.getCurrentPosition() +
                                        " but needs to be at ANY of: " + guardianRequirement.getValidPositions());
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                        }
                        case AVOID_POSITIONS -> {
                            if (guardian.getCurrentPosition() == null) {
                                ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() + " position is null");
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                            boolean isAtAvoidPosition = false;
                            for (Coordinate avoidPos : guardianRequirement.getAvoidPositions()) {
                                double distance = guardian.getCurrentPosition().distanceTo(avoidPos);
                                if (distance <= guardianRequirement.getTolerance()) {
                                    ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                            " at " + guardian.getCurrentPosition() +
                                            " but should NOT be at " + avoidPos +
                                            " (distance: " + String.format("%.2f", distance) + ")");
                                    isAtAvoidPosition = true;
                                    break;
                                }
                            }       if (isAtAvoidPosition) {
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                            ScriptConsole.println("Guardian " + guardianRequirement.getGuardianId() +
                                    " meets AVOID_POSITIONS requirement ✓");
                        }
                        case MOVING_DIRECTION -> {
                            Coordinate prev = previousPositions.get(guardian.getId());
                            Coordinate curr = guardian.getCurrentPosition();
                            if (prev == null || curr == null) {
                                allGuardiansInPosition = false;
                                break OUTER;
                            }
                            int dx = curr.getX() - prev.getX();
                            int dy = curr.getY() - prev.getY();
                            NPCDirection.Direction required = guardianRequirement.getMovingDirection();
                            boolean movingCorrect = false;
                             switch (required) {
                                case NORTH -> movingCorrect = (dy > 0);
                                case SOUTH -> movingCorrect = (dy < 0);
                                case EAST -> movingCorrect = (dx > 0);
                                case WEST -> movingCorrect = (dx < 0);
                                default -> throw new IllegalArgumentException("Unexpected value: " + required);
                            }
                            if (guardianRequirement.isAvoidDirection()) {
                                if (movingCorrect) {
                                    ScriptConsole.println("Guardian " + guardian.getId() + " is moving " + required + " but should NOT be!");
                                    allGuardiansInPosition = false;
                                    break OUTER;
                                } else {
                                    ScriptConsole.println("Guardian " + guardian.getId() + " is NOT moving " + required + " ✓");
                                }
                            } else {
                                if (!movingCorrect) {
                                    ScriptConsole.println("Guardian " + guardian.getId() + " is not moving " + required + " (dx=" + dx + ", dy=" + dy + ")");
                                    allGuardiansInPosition = false;
                                    break OUTER;
                                }
                                ScriptConsole.println("Guardian " + guardian.getId() + " is moving " + required + " ✓");
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
            
            if (allGuardiansInPosition) {
                ScriptConsole.println("All guardian requirements met! Proceeding...");
                return true;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed % 2000 < 50) {
                ScriptConsole.println("Still waiting for guardian requirements... (" + (elapsed / 1000) + "s elapsed)");
            }
            
            Execution.delay(200);
        }
        
        ScriptConsole.println("Timeout waiting for guardian requirements after " + (timeoutMs / 1000) + " seconds");
        return false;
    }
    
    /**
     * Get the closest guardian to a position
     */
    public Guardian getClosestGuardian(Coordinate position) {
        if (position == null) return null;
        
        return activeGuardians.values().stream()
            .filter(guardian -> guardian.getCurrentPosition() != null)
            .min(Comparator.comparingDouble(guardian -> 
                guardian.getCurrentPosition().distanceTo(position)))
            .orElse(null);
    }
    
    /**
     * Get all active guardians
     */
    public Collection<Guardian> getActiveGuardians() {
        return activeGuardians.values();
    }
    
    /**
     * Get guardian positions
     */
    public List<Coordinate> getGuardianPositions() {
        return new ArrayList<>(guardianPositions);
    }
    
    /**
     * Wait until it's safe to move to a position
     */
    public boolean waitUntilSafe(Coordinate targetPosition, long timeoutMs, Area gardenArea) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            updateGuardianPositions(gardenArea);
            
            if (isSafeToMove(targetPosition)) {
                return true;
            }
            
            Execution.delay(100); // Reduced delay for faster response
        }
        
        return false;
    }
    
    /**
     * Get the number of active guardians
     */
    public int getActiveGuardianCount() {
        return activeGuardians.size();
    }
    
    /**
     * Clear all tracked guardians
     */
    public void clear() {
        activeGuardians.clear();
        guardianPositions.clear();
    }

    public Guardian getGuardianById(int guardianId) {
        return activeGuardians.get(guardianId);
    }
    
    @Override
    public String toString() {
        return String.format("GuardianTracker{activeGuardians=%d, lastUpdate=%d}", 
                           activeGuardians.size(), lastUpdateTime);
    }
} 