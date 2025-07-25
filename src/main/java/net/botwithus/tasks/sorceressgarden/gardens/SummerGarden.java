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

public class SummerGarden extends BaseGarden {
    private static final int SUMMER_DOOR_ID = 21687;
    private static final Coordinate SUMMER_START = new Coordinate(2910, 5481, 0);
    private static final Coordinate SUMMER_TREE = new Coordinate(2915, 5491, 0);
    private static final int[] SUMMER_GUARDIAN_IDS = {5547,5548,5549,5550,5551,5552};

    private static final Area SUMMER_GARDEN_AREA = new Area.Rectangular(new Coordinate(2904, 5497, 0), new Coordinate(2927, 5480, 0));
    
    private static final Coordinate WAYPOINT_1 = new Coordinate(2908, 5482, 0);
    private static final Coordinate WAYPOINT_2 = new Coordinate(2906, 5486, 0);
    private static final Coordinate WAYPOINT_3 = new Coordinate(2906, 5492, 0);
    private static final Coordinate WAYPOINT_4 = new Coordinate(2909, 5490, 0);
    private static final Coordinate WAYPOINT_5 = new Coordinate(2909, 5486, 0);
    private static final Coordinate WAYPOINT_6 = new Coordinate(2920, 5485, 0);
    private static final Coordinate WAYPOINT_7 = new Coordinate(2924, 5487, 0);
    private static final Coordinate WAYPOINT_8 = new Coordinate(2922, 5495, 0);

    private static final Coordinate WAYPOINT_9 = new Coordinate(2920, 5488, 0);


    
    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_1 = {
        
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_2 = {
        new GuardianRequirement(5547, NPCDirection.Direction.NORTH),
        new GuardianRequirement(5547, new Coordinate(2907, 5484, 0)),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_3 = {
        new GuardianRequirement(5547, NPCDirection.Direction.SOUTH),
        new GuardianRequirement(5547, new Coordinate(2907, 5486, 0)),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_4 = {
        new GuardianRequirement(5548, NPCDirection.Direction.SOUTH),
        new GuardianRequirement(5548, new Coordinate(2907, 5492, 0)),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_5 = {
        new GuardianRequirement(5549, NPCDirection.Direction.NORTH),
        new GuardianRequirement(5549, new Coordinate(2910, 5490, 0)),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_6 = {
        new GuardianRequirement(5550, NPCDirection.Direction.WEST),
        new GuardianRequirement(5550, new Coordinate(2915, 5485, 0)),
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_7 = {
        new GuardianRequirement(5551, NPCDirection.Direction.NORTH),
        new GuardianRequirement(5551, new Coordinate(2923, 5487, 0)),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_8 = {
        new GuardianRequirement(5551, NPCDirection.Direction.SOUTH),
        new GuardianRequirement(5551, new Coordinate(2923, 5487, 0)),

        GuardianRequirement.createAvoidPositions(5552, new int[][] {
            {2921, 5494, 0},
            {2921, 5493, 0},
            {2921, 5492, 0},
        }),
    };


    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_9 = {

    };


    public SummerGarden(CoaezUtility script) {
        super(script, GardenType.SUMMER, SUMMER_DOOR_ID, SUMMER_START, SUMMER_TREE, SUMMER_GUARDIAN_IDS, SUMMER_GARDEN_AREA);
    }

    @Override
    protected List<Waypoint> getWaypoints() {
        return Arrays.asList(
            new Waypoint(WAYPOINT_1, "Summer Garden - First Checkpoint", 0),
            new Waypoint(WAYPOINT_2, "Summer Garden - Second Checkpoint", 0),
            new Waypoint(WAYPOINT_3, "Summer Garden - Third Checkpoint", 0),
            new Waypoint(WAYPOINT_4, "Summer Garden - Fourth Checkpoint", 0),
            new Waypoint(WAYPOINT_5, "Summer Garden - Fifth Checkpoint", 0),
            new Waypoint(WAYPOINT_6, "Summer Garden - Sixth Checkpoint", 0),
            new Waypoint(WAYPOINT_7, "Summer Garden - Seventh Checkpoint", 0),
            new Waypoint(WAYPOINT_8, "Summer Garden - Eighth Checkpoint", 0),
            new Waypoint(WAYPOINT_9, "Summer Garden - Ninth Checkpoint", 0)
        );
    }

    @Override
    protected boolean enterGarden() {
        ScriptConsole.println("Attempting to enter Summer Garden through door ID: " + SUMMER_DOOR_ID);
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(SUMMER_DOOR_ID).option("Open").hidden(false).results();
        if (!results.isEmpty()) {
            results.nearest().interact("Open");
        } else {
            return false;
        }
        return waitForStartPosition();
    }

    @Override
    protected boolean navigateToTree() {
        ScriptConsole.println("Navigating to Summer Garden tree with guardian tracking");
        List<Waypoint> waypoints = getWaypoints();
        if (waypoints.isEmpty()) return false;
        for (int i = 0; i < waypoints.size(); i++) {
            if(!script.isActive()) break;
            if(!isInSummerGardenArea()) break;
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
        ScriptConsole.println("Harvesting Summer sq'irk fruit");
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Sq'irk tree").option("Pick-fruit").results();
        if (!results.isEmpty()) {
            results.nearest().interact("Pick-fruit");
        }
        Execution.delayUntil(8000, ()  -> !SUMMER_GARDEN_AREA.contains(LocalPlayer.LOCAL_PLAYER) && LocalPlayer.LOCAL_PLAYER.getAnimationId() == -1);
        return true;
    }

    @Override
    protected boolean returnToCenter() {
        ScriptConsole.println("Returning to central garden from Summer Garden");
        if(Backpack.contains("Broomstick")) {
            Backpack.interact("Broomstick", "Teleport");
            return true;
        }
        ScriptConsole.println("No broomstick found, returning to central garden");
        return false;
    }

    private boolean waitForStartPosition() {
        ScriptConsole.println("Waiting to reach Summer Garden start position");
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        if (player.getCoordinate().equals(SUMMER_START)) {
            return true;
        }
        Execution.delayUntil(10000,() -> Client.getLocalPlayer().getCoordinate().equals(SUMMER_START));
        return true;
    }

    @Override
    public int getDoorId() {
        return SUMMER_DOOR_ID;
    }

    @Override
    public Coordinate getStartPosition() {
        return SUMMER_START;
    }

    @Override
    public Coordinate getTreePosition() {
        return SUMMER_TREE;
    }

    @Override
    public List<Integer> getGuardianIds() {
        return Arrays.stream(SUMMER_GUARDIAN_IDS).boxed().toList();
    }

    private boolean waitForGuardiansAtWaypoint(int waypointIndex) {
        ScriptConsole.println("Waiting for guardians to meet requirements for waypoint " + waypointIndex);
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
            case 2 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
            case 3 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
            case 4 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
            case 5 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
            case 6 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_6;
            case 7 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_7;
            case 8 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_8;
            case 9 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_9;
            default -> {
                return true;
            }
        }

        List<GuardianRequirement> guardianRequirementList = Arrays.asList(guardianRequirements);
        ScriptConsole.println("Waiting for " + guardianRequirementList.size() + " guardian requirements:");
        for (GuardianRequirement requirement : guardianRequirementList) {
            if(!script.isActive()) break;
            if(!isInSummerGardenArea()) break;
            ScriptConsole.println("  - Guardian " + requirement.getGuardianId());
        }
        guardianTracker.updateGuardianPositions(gardenArea);
        if (!guardianTracker.waitForGuardianRequirements(guardianRequirementList, 120000, gardenArea)) {
            if(!script.isActive()) return false;
            if(!isInSummerGardenArea()) return false;
            ScriptConsole.println("Timeout waiting for guardians to meet requirements for waypoint " + waypointIndex);
            return false;
        }
        ScriptConsole.println("All " + guardianRequirementList.size() + " guardian requirements met for waypoint " + waypointIndex);
        return true;
    }

    public int getGuardianCountForWaypoint(int waypointIndex) {
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
            case 2 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
            case 3 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
            case 4 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
            case 5 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
            case 6 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_6;
            case 7 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_7;
            case 8 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_8;
            case 9 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_9;
            default -> {
                return 0;
            }
        }
        return guardianRequirements.length;
    }

    public List<GuardianRequirement> getGuardianRequirementsForWaypoint(int waypointIndex) {
        GuardianRequirement[] guardianRequirements;
        switch (waypointIndex) {
            case 1 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_1;
            case 2 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_2;
            case 3 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_3;
            case 4 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_4;
            case 5 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_5;
            case 6 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_6;
            case 7 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_7;
            case 8 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_8;
            case 9 -> guardianRequirements = GUARDIAN_REQUIREMENTS_WAYPOINT_9;
            default -> {
                return new ArrayList<>();
            }
        }
        return Arrays.asList(guardianRequirements);
    }

    private boolean isInSummerGardenArea() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && SUMMER_GARDEN_AREA.contains(player.getCoordinate());
    }

    @Override
    public void reset() {
        this.isCompleted = false;
        this.hasFailed = false;
        this.currentState = GardenState.IDLE;
    }

    @Override
    public String toString() {
        return "SummerGarden{doorId=" + SUMMER_DOOR_ID + ", startPosition=" + SUMMER_START + ", treePosition=" + SUMMER_TREE + "}";
    }
} 