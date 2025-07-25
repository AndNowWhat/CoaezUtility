package net.botwithus.tasks.sorceressgarden.gardens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.sorceressgarden.models.GardenState;
import net.botwithus.tasks.sorceressgarden.models.GardenType;
import net.botwithus.tasks.sorceressgarden.models.GuardianRequirement;
import net.botwithus.tasks.sorceressgarden.models.NPCDirection;
import net.botwithus.tasks.sorceressgarden.models.Waypoint;


public class WinterGarden extends BaseGarden {
    private static final int WINTER_DOOR_ID = 21709;
    private static final Coordinate WINTER_START = new Coordinate(2902, 5470, 0); 
    private static final Coordinate WINTER_TREE = new Coordinate(2892, 5477, 0);
    private static final int[] WINTER_GUARDIAN_IDS = {5553, 5554, 5555, 5556, 5557, 5558}; 
    
    private static final Area WINTER_GARDEN_AREA = new Area.Rectangular(new Coordinate(2886, 5487, 0), new Coordinate(2903, 5464, 0));
    
    private static final Coordinate WAYPOINT_1 = new Coordinate(2900, 5476, 0);
    private static final Coordinate WAYPOINT_2 = new Coordinate(2898, 5481, 0);
    private static final Coordinate WAYPOINT_3 = new Coordinate(2891, 5482, 0);
    private static final Coordinate WAYPOINT_4 = new Coordinate(2892, 5484, 0);
    private static final Coordinate WAYPOINT_5 = new Coordinate(2892, 5477, 0);

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_1 = {
        new GuardianRequirement(5555, new int[][] {
            {2899, 5474, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2899, 5475, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2899, 5476, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2899, 5477, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2899, 5478, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2897, 5478, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2897, 5477, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2897, 5476, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2897, 5475, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2897, 5474, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2898, 5478, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2898, 5477, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2898, 5476, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2898, 5475, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2898, 5474, 0, NPCDirection.Direction.SOUTH.ordinal()}
        })
    };
    
    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_2 = {
        GuardianRequirement.createAvoidPositions(5556, new int[][] {
            {2899, 5483, 0},
            {2900, 5483, 0},
            {2900, 5482, 0},
            {2900, 5481, 0},
        }),
        GuardianRequirement.createAvoidPositions(5555, new int[][] {
            {2899, 5471, 0},
            {2899, 5472, 0},
            {2899, 5473, 0},
            {2899, 5474, 0},
            {2899, 5475, 0},
            {2899, 5476, 0},
            {2899, 5477, 0},
        }),
    };
    
    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_3 = {
        new GuardianRequirement(5557, new int[][] {
            {2892, 5481, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2891, 5481, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2891, 5482, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2891, 5483, 0, NPCDirection.Direction.SOUTH.ordinal()},
            {2892, 5483, 0, NPCDirection.Direction.SOUTH.ordinal()}
        }),

        GuardianRequirement.createAvoidPositions(5556, new int[][] {
            {2897, 5481, 0},
            {2897, 5480, 0},
            {2898, 5480, 0},
        }),
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_4 = {
        GuardianRequirement.createAvoidPositions(5557, new int[][] {
            {2893, 5481, 0},
            {2892, 5481, 0},
            {2891, 5481, 0},
            {2892, 5482, 0}
        })
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_5 = {
        new GuardianRequirement(5558, 4.0)
    };
    
    public WinterGarden(CoaezUtility script) {
        super(script, GardenType.WINTER, WINTER_DOOR_ID, WINTER_START, WINTER_TREE, WINTER_GUARDIAN_IDS, WINTER_GARDEN_AREA);
    }
    
    @Override
    protected List<Waypoint> getWaypoints() {
        return Arrays.asList(
            new Waypoint(WAYPOINT_1, "Winter Garden - First Checkpoint", 0),
            new Waypoint(WAYPOINT_2, "Winter Garden - Second Checkpoint", 0),
            new Waypoint(WAYPOINT_3, "Winter Garden - Third Checkpoint", 0),
            new Waypoint(WAYPOINT_4, "Winter Garden - Fourth Checkpoint", 0),
            new Waypoint(getRandomizedWaypoint5(), "Winter Garden - Fifth Checkpoint", 0)
        );
    }
    
    @Override
    protected boolean enterGarden() {
        ScriptConsole.println("Attempting to enter Winter Garden through door ID: " + WINTER_DOOR_ID);
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(WINTER_DOOR_ID).option("Open").hidden(false).results();
        if (!results.isEmpty()) {
            results.nearest().interact("Open");
           
        } else{
            return false;
        }        
        return waitForStartPosition();
    }
    
    @Override
    protected boolean navigateToTree() {
        ScriptConsole.println("Navigating to Winter Garden tree with guardian tracking");
        
        List<Waypoint> waypoints = getWaypoints();
        if (waypoints.isEmpty()) return false;
        
        for (int i = 0; i < waypoints.size(); i++) {
            if(!script.isActive()) break;
            if(!isInWinterGardenArea()) break;

            Waypoint waypoint = waypoints.get(i);
            ScriptConsole.println("Moving to waypoint " + (i + 1) + ": " + waypoint.getDescription());
            
            if (!waitForGuardiansAtWaypoint(i + 1)) {
                ScriptConsole.println("Failed to wait for guardians at waypoint " + (i + 1));
                return false;
            }
            
            ScriptConsole.println("Guardians in position - MOVING to waypoint " + (i + 1));
            if (!pathNavigator.navigateToWaypoint(waypoint, gardenArea)) {
                ScriptConsole.println("Failed to reach waypoint " + (i + 1));
                return false;
            }
            
            if (i < waypoints.size() - 1) {
                ScriptConsole.println("Proceeding to next waypoint");
            }
        }
        
        return true;
    }
    
    @Override
    protected boolean harvestFruit() {
        ScriptConsole.println("Harvesting Winter sq'irk fruit");
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Sq'irk tree").option("Pick-fruit").results();
        if (!results.isEmpty()) {
            results.nearest().interact("Pick-fruit");
        }
        Execution.delayUntil(8000, ()  -> !WINTER_GARDEN_AREA.contains(LocalPlayer.LOCAL_PLAYER) && LocalPlayer.LOCAL_PLAYER.getAnimationId() == -1);
        return true;
    }
    
    @Override
    protected boolean returnToCenter() {
        ScriptConsole.println("Returning to central garden from Winter Garden");
        if(Backpack.contains("Broomstick")) {
            Backpack.interact("Broomstick", "Teleport");
            return true;
        }
        ScriptConsole.println("No broomstick found, returning to central garden");
        return false;
    }
    
    /**
     * Wait for player to reach the start position
     */
    private boolean waitForStartPosition() {
        ScriptConsole.println("Waiting to reach Winter Garden start position");
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        if (player.getCoordinate().equals(WINTER_START)) {
            return true;
        }
        Execution.delayUntil(10000,() -> Client.getLocalPlayer().getCoordinate().equals(WINTER_START));
        return true;
    }
    
    /**
     * Get the door ID for this garden
     */
    @Override
    public int getDoorId() {
        return WINTER_DOOR_ID;
    }
    
    /**
     * Get the start position for this garden
     */
    @Override
    public Coordinate getStartPosition() {
        return WINTER_START;
    }
    
    /**
     * Get the tree position for this garden
     */
    @Override
    public Coordinate getTreePosition() {
        return WINTER_TREE;
    }
    
    /**
     * Get the guardian IDs for this garden
     */
    @Override
    public List<Integer> getGuardianIds() {
        return Arrays.stream(WINTER_GUARDIAN_IDS).boxed().toList();
    }
    
    /**
     * Wait for guardians to meet their requirements before moving to a waypoint
     */
    private boolean waitForGuardiansAtWaypoint(int waypointIndex) {
        ScriptConsole.println("Waiting for guardians to meet requirements for waypoint " + waypointIndex);
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
            case 2 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
            case 3 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
            case 4 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
            case 5 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
            default -> {
                return true;
            }
        }
        List<GuardianRequirement> guardianRequirementList = Arrays.asList(guardianRequirements);
        ScriptConsole.println("Waiting for " + guardianRequirementList.size() + " guardian requirements:");
        for (GuardianRequirement requirement : guardianRequirementList) {
            if(!script.isActive()) break;
            if(!isInWinterGardenArea()) break;
            if (null == requirement.getType()) {
                ScriptConsole.println("  - Guardian " + requirement.getGuardianId() +
                        " must NOT be at: " + requirement.getAvoidPositions());
            } else switch (requirement.getType()) {
                case EXACT_POSITION -> ScriptConsole.println("  - Guardian " + requirement.getGuardianId() +
                        " must be at " + requirement.getExactPosition() + " facing " + requirement.getMovingDirection().name());
                case MULTIPLE_POSITIONS -> ScriptConsole.println("  - Guardian " + requirement.getGuardianId() +
                        " must be at ANY of: " + requirement.getValidPositions());
                case MIN_DISTANCE -> ScriptConsole.println("  - Guardian " + requirement.getGuardianId() +
                        " must be at least " + requirement.getMinDistance() + " tiles from the player");
                default -> ScriptConsole.println("  - Guardian " + requirement.getGuardianId() +
                        " must NOT be at: " + requirement.getAvoidPositions());
            }
        }
        ScriptConsole.println("Forcing guardian position update before waiting...");
        guardianTracker.updateGuardianPositions(gardenArea);
        if (!guardianTracker.waitForGuardianRequirements(guardianRequirementList, 120000, gardenArea)) {
            if(!script.isActive()) return false;
            if(!isInWinterGardenArea()) return false;
            ScriptConsole.println("Timeout waiting for guardians to meet requirements for waypoint " + waypointIndex);
            return false;
        }
        ScriptConsole.println("All " + guardianRequirementList.size() + " guardian requirements met for waypoint " + waypointIndex);
        return true;
    }
    
    /**
     * Get the number of guardian requirements for a specific waypoint
     */
    public int getGuardianCountForWaypoint(int waypointIndex) {
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
            case 2 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
            case 3 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
            case 4 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
            case 5 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
            default -> {
                return 0;
            }
        }
        return guardianRequirements.length;
    }
    
    /**
     * Get all guardian requirements for a specific waypoint
     */
    public List<GuardianRequirement> getGuardianRequirementsForWaypoint(int waypointIndex) {
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
            case 2 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
            case 3 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
            case 4 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
            case 5 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
            default -> {
                return new ArrayList<>();
            }
        }
        return Arrays.asList(guardianRequirements);
    }
    
    /**
     * Helper: check if player is in the Winter Garden area
     */
    private boolean isInWinterGardenArea() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && WINTER_GARDEN_AREA.contains(player.getCoordinate());
    }

    private Coordinate getRandomizedWaypoint5() {
        if (Math.random() < 0.5) {
            return new Coordinate(WAYPOINT_5.getX() - 1, WAYPOINT_5.getY(), WAYPOINT_5.getZ());
        } else {
            return WAYPOINT_5;
        }
    }    
    
    @Override
    public void reset() {
        this.isCompleted = false;
        this.hasFailed = false;
        this.currentState = GardenState.IDLE;
    }
    
    @Override
    public String toString() {
        return "WinterGarden{doorId=" + WINTER_DOOR_ID + ", startPosition=" + WINTER_START + ", treePosition=" + WINTER_TREE + "}";
    }
} 