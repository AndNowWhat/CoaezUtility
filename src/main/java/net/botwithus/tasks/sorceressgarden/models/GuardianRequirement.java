package net.botwithus.tasks.sorceressgarden.models;

import java.util.ArrayList;
import java.util.List;

import net.botwithus.rs3.game.Coordinate;

/**
 * Represents a guardian requirement that can be either:
 * 1. Exact position and direction requirement
 * 2. Multiple valid positions and directions (guardian must be at ANY of these positions)
 * 3. Avoid specific positions (guardian must NOT be at these coordinates)
 */
public class GuardianRequirement {
    private final int guardianId;
    private final RequirementType type;
    private final Coordinate exactPosition;
    private final NPCDirection.Direction exactDirection;
    private final List<Coordinate> avoidPositions;
    private final List<GuardianPosition> validPositions;
    private final double tolerance;
    private final Double minDistance;
    private final NPCDirection.Direction movingDirection;
    private final boolean isAvoidDirection;
    
    public enum RequirementType {
        EXACT_POSITION,    // Guardian must be at specific position and direction
        MULTIPLE_POSITIONS, // Guardian must be at ANY of the specified positions with directions
        AVOID_POSITIONS,    // Guardian must NOT be at any of the specified positions
        MIN_DISTANCE,       // Guardian must be at least a certain distance from a position
        MOVING_DIRECTION    // Guardian must be moving in a specific direction
    }
    
    /**
     * Create an exact position requirement
     */
    public GuardianRequirement(int guardianId, Coordinate position, NPCDirection.Direction direction) {
        this(guardianId, position, direction, 1.0);
    }
    
    /**
     * Create an exact position requirement with custom tolerance
     */
    public GuardianRequirement(int guardianId, Coordinate position, NPCDirection.Direction direction, double tolerance) {
        this.guardianId = guardianId;
        this.type = RequirementType.EXACT_POSITION;
        this.exactPosition = position;
        this.exactDirection = direction;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        this.tolerance = tolerance;
        this.minDistance = null;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }
    
    /**
     * Create an exact position requirement using coordinates
     */
    public GuardianRequirement(int guardianId, int x, int y, int z, NPCDirection.Direction direction) {
        this(guardianId, new Coordinate(x, y, z), direction);
    }
    
    /**
     * Create a multiple positions requirement - guardian must be at ANY of the specified positions
     */
    public GuardianRequirement(int guardianId, List<GuardianPosition> validPositions) {
        this(guardianId, validPositions, 1.0);
    }
    
    /**
     * Create a multiple positions requirement with custom tolerance
     */
    public GuardianRequirement(int guardianId, List<GuardianPosition> validPositions, double tolerance) {
        this.guardianId = guardianId;
        this.type = RequirementType.MULTIPLE_POSITIONS;
        this.exactPosition = null;
        this.exactDirection = null;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>(validPositions);
        this.tolerance = tolerance;
        this.minDistance = null;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }
    
    /**
     * Create a multiple positions requirement using coordinate arrays with directions
     * Format: {{x1, y1, z1, direction1}, {x2, y2, z2, direction2}, ...}
     * Where direction is 0=NORTH, 1=EAST, 2=SOUTH, 3=WEST
     */
    public GuardianRequirement(int guardianId, int[][] validCoordinatesWithDirections) {
        this.guardianId = guardianId;
        this.type = RequirementType.MULTIPLE_POSITIONS;
        this.exactPosition = null;
        this.exactDirection = null;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        this.tolerance = 1.0;
        this.minDistance = null;
        this.movingDirection = null;
        this.isAvoidDirection = false;
        
        for (int[] coords : validCoordinatesWithDirections) {
            if (coords.length >= 4) {
                NPCDirection.Direction direction = NPCDirection.Direction.values()[coords[3]];
                this.validPositions.add(new GuardianPosition(guardianId, coords[0], coords[1], coords[2], direction));
            }
        }
    }
    
    /**
     * Create an avoid positions requirement using coordinate arrays
     */
    public GuardianRequirement(int guardianId, int[][] avoidCoordinates, boolean isAvoidPositions) {
        this.guardianId = guardianId;
        this.type = RequirementType.AVOID_POSITIONS;
        this.exactPosition = null;
        this.exactDirection = null;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        for (int[] coords : avoidCoordinates) {
            this.avoidPositions.add(new Coordinate(coords[0], coords[1], coords[2]));
        }
        this.tolerance = 1.0;
        this.minDistance = null;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }
    
    /**
     * Create a min distance requirement - guardian must be at least minDistance from position
     */
    public GuardianRequirement(int guardianId, Coordinate position, double minDistance) {
        this.guardianId = guardianId;
        this.type = RequirementType.MIN_DISTANCE;
        this.exactPosition = position;
        this.minDistance = minDistance;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        this.exactDirection = null;
        this.tolerance = 1.0;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }
    
    /**
     * Create a min distance requirement - guardian must be at least minDistance from the player
     */
    public GuardianRequirement(int guardianId, double minDistance) {
        this.guardianId = guardianId;
        this.type = RequirementType.MIN_DISTANCE;
        this.exactPosition = null;
        this.minDistance = minDistance;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        this.exactDirection = null;
        this.tolerance = 1.0;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }

    /**
     * Create a moving direction requirement - guardian must be moving in a specific direction
     */
    public GuardianRequirement(int guardianId, NPCDirection.Direction movingDirection, boolean isAvoidDirection) {
        this.guardianId = guardianId;
        this.type = RequirementType.MOVING_DIRECTION;
        this.exactPosition = null;
        this.exactDirection = null;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        this.tolerance = 1.0;
        this.minDistance = null;
        this.movingDirection = movingDirection;
        this.isAvoidDirection = isAvoidDirection;
    }
    
    /**
     * Create a position-only requirement (guardian must be at this coordinate, any direction)
     */
    public GuardianRequirement(int guardianId, Coordinate position) {
        this.guardianId = guardianId;
        this.type = RequirementType.EXACT_POSITION;
        this.exactPosition = position;
        this.exactDirection = null;
        this.avoidPositions = new ArrayList<>();
        this.validPositions = new ArrayList<>();
        this.tolerance = 1.0;
        this.minDistance = null;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }
    
    /**
     * Private constructor for avoid positions with Coordinate list
     */
    private GuardianRequirement(int guardianId, List<Coordinate> avoidPositions, double tolerance, boolean isAvoidPositions) {
        this.guardianId = guardianId;
        this.type = RequirementType.AVOID_POSITIONS;
        this.exactPosition = null;
        this.exactDirection = null;
        this.avoidPositions = new ArrayList<>(avoidPositions);
        this.validPositions = new ArrayList<>();
        this.tolerance = tolerance;
        this.minDistance = null;
        this.movingDirection = null;
        this.isAvoidDirection = false;
    }
    
    /**
     * Add back the two-argument constructor for compatibility
     */
    public GuardianRequirement(int guardianId, NPCDirection.Direction movingDirection) {
        this(guardianId, movingDirection, false);
    }
    
    /**
     * Create a multiple positions requirement using GuardianPosition list
     */
    public static GuardianRequirement createMultiplePositions(int guardianId, List<GuardianPosition> validPositions) {
        return new GuardianRequirement(guardianId, validPositions);
    }
    
    /**
     * Create a multiple positions requirement using GuardianPosition list with custom tolerance
     */
    public static GuardianRequirement createMultiplePositions(int guardianId, List<GuardianPosition> validPositions, double tolerance) {
        return new GuardianRequirement(guardianId, validPositions, tolerance);
    }
    
    /**
     * Create an avoid positions requirement using Coordinate list
     */
    public static GuardianRequirement createAvoidPositions(int guardianId, List<Coordinate> avoidPositions) {
        return new GuardianRequirement(guardianId, avoidPositions, 1.0, true);
    }
    
    /**
     * Create an avoid positions requirement using coordinate arrays
     */
    public static GuardianRequirement createAvoidPositions(int guardianId, int[][] avoidCoordinates) {
        return new GuardianRequirement(guardianId, avoidCoordinates, true);
    }
    
    /**
     * Create a moving direction requirement - guardian must NOT be moving in the specified direction.
     * This is to be interpreted as: guardian must NOT be facing/moving in the given direction.
     * The logic that checks requirements must treat this as a negative check.
     */
    public static GuardianRequirement createAvoidDirection(int guardianId, NPCDirection.Direction direction) {
        return new GuardianRequirement(guardianId, direction, true);
    }
    
    public int getGuardianId() {
        return guardianId;
    }
    
    public RequirementType getType() {
        return type;
    }
    
    public Coordinate getExactPosition() {
        return exactPosition;
    }
    
    public NPCDirection.Direction getExactDirection() {
        return exactDirection;
    }
    
    public List<Coordinate> getAvoidPositions() {
        return new ArrayList<>(avoidPositions);
    }
    
    public List<GuardianPosition> getValidPositions() {
        return new ArrayList<>(validPositions);
    }
    
    public double getTolerance() {
        return tolerance;
    }
    
    public Double getMinDistance() {
        return minDistance;
    }

    public NPCDirection.Direction getMovingDirection() {
        return movingDirection;
    }

    public boolean isAvoidDirection() {
        return isAvoidDirection;
    }
    
    @Override
    public String toString() {
        if (null == type) {
            return String.format("GuardianRequirement{guardianId=%d, type=AVOID_POSITIONS, avoidPositions=%s}",
                    guardianId, avoidPositions);
        } else return switch (type) {
            case EXACT_POSITION -> String.format("GuardianRequirement{guardianId=%d, type=EXACT_POSITION, position=%s, direction=%s, tolerance=%.1f}",
                    guardianId, exactPosition, movingDirection, tolerance);
            case MULTIPLE_POSITIONS -> String.format("GuardianRequirement{guardianId=%d, type=MULTIPLE_POSITIONS, validPositions=%s, tolerance=%.1f}",
                    guardianId, validPositions, tolerance, movingDirection);
            case MIN_DISTANCE -> String.format("GuardianRequirement{guardianId=%d, type=MIN_DISTANCE, position=%s, minDistance=%.1f}",
                    guardianId, exactPosition, minDistance);
            case MOVING_DIRECTION -> String.format("GuardianRequirement{guardianId=%d, type=MOVING_DIRECTION, movingDirection=%s}",
                    guardianId, movingDirection);
            default -> String.format("GuardianRequirement{guardianId=%d, type=AVOID_POSITIONS, avoidPositions=%s}",
                    guardianId, avoidPositions);
        };
    }
} 