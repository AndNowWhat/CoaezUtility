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

public class SpringGarden extends BaseGarden {
    private static final int SPRING_DOOR_ID = 21753;
    private static final Coordinate SPRING_START = new Coordinate(2921, 5473, 0);
    private static final Coordinate SPRING_TREE = new Coordinate(2931, 5463, 0);
    private static final int[] SPRING_GUARDIAN_IDS = {5539, 5540, 5541, 5544, 5545,5546};
    private static final Area SPRING_GARDEN_AREA = new Area.Rectangular(new Coordinate(2920, 5479, 0), new Coordinate(2937, 5456, 0));

    private static final Coordinate WAYPOINT_1 = new Coordinate(2923, 5471, 0);
    private static final Coordinate WAYPOINT_2 = new Coordinate(2923, 5465, 0);
    private static final Coordinate WAYPOINT_3 = new Coordinate(2923, 5459, 0);
    private static final Coordinate WAYPOINT_4 = new Coordinate(2926, 5468, 0);
    private static final Coordinate WAYPOINT_5 = new Coordinate(2928, 5470, 0);
    private static final Coordinate WAYPOINT_6 = new Coordinate(2932, 5465, 0);
    

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_1 = {
        
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_2 = {
        new GuardianRequirement(5539, 5.0),
        new GuardianRequirement(5539, NPCDirection.Direction.SOUTH),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_3 = {
        GuardianRequirement.createAvoidPositions(5539, new int[][] {
            {2923, 5460, 0},
            {2923, 5461, 0},
            {2923, 5462, 0},
            {2923, 5463, 0},
            {2923, 5464, 0},
            {2923, 5465, 0},
        }),
        new GuardianRequirement(5539, NPCDirection.Direction.NORTH),
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_4 = {
        GuardianRequirement.createAvoidPositions(5540, new int[][] {
            {2927, 5461, 0},
            {2926, 5461, 0},
            {2925, 5461, 0},

        }),
        GuardianRequirement.createAvoidPositions(5541, new int[][] {
            {2926, 5458, 0},
            {2925, 5458, 0},
            {2924, 5458, 0},
            {2924, 5460, 0},

        }),
        new GuardianRequirement(5545, NPCDirection.Direction.NORTH),
        GuardianRequirement.createAvoidPositions(5545, new int[][] {
            {2925, 5475, 0},
        }),
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_5 = {
        GuardianRequirement.createAvoidPositions(5545, new int[][] {
            {2925, 5468, 0},
            {2925, 5469, 0},
            {2925, 5470, 0},
            {2925, 5471, 0},
            {2925, 5472, 0},
            {2925, 5473, 0},
            {2925, 5474, 0},
            {2925, 5475, 0},
        }),
        new GuardianRequirement(5545, NPCDirection.Direction.SOUTH),
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_6 = {
        new GuardianRequirement(5544, new Coordinate(2932, 5469, 0), NPCDirection.Direction.EAST),

    };


    public SpringGarden(CoaezUtility script) {
        super(script, GardenType.SPRING, SPRING_DOOR_ID, SPRING_START, SPRING_TREE, SPRING_GUARDIAN_IDS, SPRING_GARDEN_AREA);
    }

    @Override
    protected List<Waypoint> getWaypoints() {
        return Arrays.asList(
            new Waypoint(WAYPOINT_1, "Spring Garden - First Checkpoint", 0),
            new Waypoint(WAYPOINT_2, "Spring Garden - Second Checkpoint", 0),
            new Waypoint(WAYPOINT_3, "Spring Garden - Third Checkpoint", 0),
            new Waypoint(WAYPOINT_4, "Spring Garden - Fourth Checkpoint", 0),
            new Waypoint(WAYPOINT_5, "Spring Garden - Fifth Checkpoint", 0),
            new Waypoint(getRandomizedWaypoint6(), "Spring Garden - Sixth Checkpoint", 0)
        );
    }

    @Override
    protected boolean enterGarden() {
        ScriptConsole.println("Attempting to enter Spring Garden through door ID: " + SPRING_DOOR_ID);
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(SPRING_DOOR_ID).option("Open").hidden(false).results();
        if (!results.isEmpty()) {
            results.nearest().interact("Open");
        } else {
            return false;
        }
        return waitForStartPosition();
    }

    @Override
    protected boolean navigateToTree() {
        ScriptConsole.println("Navigating to Spring Garden tree with guardian tracking");
        List<Waypoint> waypoints = getWaypoints();
        if (waypoints.isEmpty()) return false;
        for (int i = 0; i < waypoints.size(); i++) {
            if(!script.isActive()) break;
            if(!isInSpringGardenArea()) break;
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
        ScriptConsole.println("Harvesting Spring sq'irk fruit");
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Sq'irk tree").option("Pick-fruit").results();
        if (!results.isEmpty()) {
            results.nearest().interact("Pick-fruit");
        }
        Execution.delayUntil(8000, ()  -> !SPRING_GARDEN_AREA.contains(LocalPlayer.LOCAL_PLAYER) && LocalPlayer.LOCAL_PLAYER.getAnimationId() == -1);
        return true;
    }

    @Override
    protected boolean returnToCenter() {
        ScriptConsole.println("Returning to central garden from Spring Garden");
        if(Backpack.contains("Broomstick")) {
            Backpack.interact("Broomstick", "Teleport");
            return true;
        }
        ScriptConsole.println("No broomstick found, returning to central garden");
        return false;
    }

    private boolean waitForStartPosition() {
        ScriptConsole.println("Waiting to reach Spring Garden start position");
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        if (player.getCoordinate().equals(SPRING_START)) {
            return true;
        }
        Execution.delayUntil(10000,() -> Client.getLocalPlayer().getCoordinate().equals(SPRING_START));
        return true;
    }

    public int getDoorId() {
        return SPRING_DOOR_ID;
    }

    public Coordinate getStartPosition() {
        return SPRING_START;
    }

    public Coordinate getTreePosition() {
        return SPRING_TREE;
    }

    public List<Integer> getGuardianIds() {
        return Arrays.stream(SPRING_GUARDIAN_IDS).boxed().toList();
    }

    private boolean waitForGuardiansAtWaypoint(int waypointIndex) {
        ScriptConsole.println("Waiting for guardians to meet requirements for waypoint " + waypointIndex);
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
                break;
            case 2:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
                break;
            case 3:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
                break;
            case 4:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
                break;
            case 5:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
                break;
            case 6:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_6;
                break;
            default:
                return true;
        }

        List<GuardianRequirement> guardianRequirementList = Arrays.asList(guardianRequirements);
        ScriptConsole.println("Waiting for " + guardianRequirementList.size() + " guardian requirements:");
        for (GuardianRequirement requirement : guardianRequirementList) {
            ScriptConsole.println("  - Guardian " + requirement.getGuardianId());
        }
        guardianTracker.updateGuardianPositions(gardenArea);
        if (!guardianTracker.waitForGuardianRequirements(guardianRequirementList, 120000, gardenArea)) {
            if(!script.isActive()) return false;
            if(!isInSpringGardenArea()) return false;
            ScriptConsole.println("Timeout waiting for guardians to meet requirements for waypoint " + waypointIndex);
            return false;
        }
        ScriptConsole.println("All " + guardianRequirementList.size() + " guardian requirements met for waypoint " + waypointIndex);
        return true;
    }

    public int getGuardianCountForWaypoint(int waypointIndex) {
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
                break;
            case 2:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
                break;
            case 3:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
                break;
            case 4:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
                break;
            case 5:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
                break;
            case 6:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_6;
                break;
            default:
                return 0;
        }
        return guardianRequirements.length;
    }

    public List<GuardianRequirement> getGuardianRequirementsForWaypoint(int waypointIndex) {
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
                break;
            case 2:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
                break;
            case 3:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
                break;
            case 4:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
                break;
            case 5:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
                break;
            case 6:
                guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_6;
                break;
            default:
                return new ArrayList<>();
        }
        return Arrays.asList(guardianRequirements);
    }

    private boolean isInSpringGardenArea() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && SPRING_GARDEN_AREA.contains(player.getCoordinate());
    }

    private Coordinate getRandomizedWaypoint6() {
        if (Math.random() < 0.5) {
            return new Coordinate(WAYPOINT_6.getX(), WAYPOINT_6.getY() - 1, WAYPOINT_6.getZ());
        } else {
            return WAYPOINT_6;
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
        return "SpringGarden{doorId=" + SPRING_DOOR_ID + ", startPosition=" + SPRING_START + ", treePosition=" + SPRING_TREE + "}";
    }
} 