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

public class AutumnGarden extends BaseGarden {
    private static final int AUTUMN_DOOR_ID = 21731;
    private static final Coordinate AUTUMN_START = new Coordinate(2913, 5462, 0);
    private static final Coordinate AUTUMN_TREE = new Coordinate(2912, 5450, 0);
    private static final int[] AUTUMN_GUARDIAN_IDS = {5533,5534,5535,5536,5537,5538};
    private static final Area AUTUMN_GARDEN_AREA = new Area.Rectangular(new Coordinate(2896, 5463, 0), new Coordinate(2919, 5446, 0));

    private static final Coordinate WAYPOINT_1 = new Coordinate(2908, 5461, 0);
    private static final Coordinate WAYPOINT_2 = new Coordinate(2904, 5459, 0);
    private static final Coordinate WAYPOINT_3 = new Coordinate(2901, 5455, 0);
    private static final Coordinate WAYPOINT_4 = new Coordinate(2901, 5451, 0);
    private static final Coordinate WAYPOINT_5 = new Coordinate(2903, 5450, 0);
    private static final Coordinate WAYPOINT_6 = new Coordinate(2908, 5456, 0);
    private static final Coordinate WAYPOINT_7 = new Coordinate(2913, 5452, 0);

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_1 = {
    };
    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_2 = {
        GuardianRequirement.createAvoidPositions(5533, new int[][] {
            {2908, 5460, 0},
            {2907, 5460, 0},
            {2906, 5460, 0},
            {2905, 5460, 0},
            {2904, 5460, 0},
        }),
        new GuardianRequirement(5533, NPCDirection.Direction.WEST),
    };
    
        private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_3 = {
        GuardianRequirement.createAvoidPositions(5533, new int[][] {
            {2898, 5460, 0},
            {2899, 5460, 0},
            {2900, 5460, 0},
            {2901, 5460, 0},
            {2902, 5460, 0},
            {2903, 5460, 0},
            {2904, 5460, 0},
        }),
        new GuardianRequirement(5533, NPCDirection.Direction.EAST),
    };
    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_4 = {
        new GuardianRequirement(5534, new Coordinate(2900, 5452, 0)),
        new GuardianRequirement(5534, NPCDirection.Direction.SOUTH)
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_5 = {
        new GuardianRequirement(5535, new Coordinate(2901, 5449, 0)),
        new GuardianRequirement(5535, new Coordinate(2902, 5449, 0)),
        new GuardianRequirement(5535, NPCDirection.Direction.EAST),

        new GuardianRequirement(5534, NPCDirection.Direction.NORTH),
    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_6 = {
        GuardianRequirement.createAvoidPositions(5536, new int[][] {
            {2905, 5451, 0},
            {2904, 5451, 0},
            {2903, 5451, 0},
            {2903, 5452, 0},
            {2903, 5453, 0},
        }),
        GuardianRequirement.createAvoidPositions(5537, new int[][] {
            {2917, 5457, 0},
            {2916, 5457, 0},
            {2915, 5457, 0},
            {2914, 5457, 0},
            {2913, 5457, 0},
        }),
        new GuardianRequirement(5537, NPCDirection.Direction.EAST),

    };

    private static final GuardianRequirement[] GUARDIAN_REQUIREMENTS_WAYPOINT_7 = {
        new GuardianRequirement(5538, NPCDirection.Direction.EAST),
        GuardianRequirement.createAvoidPositions(5538, new int[][] {
            {2912, 5455, 0},
            {2911, 5455, 0},
            {2910, 5455, 0},
            {2909, 5455, 0},
            {2908, 5455, 0},
        }),
    };

    public AutumnGarden(CoaezUtility script) {
        super(script, GardenType.AUTUMN, AUTUMN_DOOR_ID, AUTUMN_START, AUTUMN_TREE, AUTUMN_GUARDIAN_IDS, AUTUMN_GARDEN_AREA);
    }

    @Override
    protected List<Waypoint> getWaypoints() {
        return Arrays.asList(
            new Waypoint(WAYPOINT_1, "Autumn Garden - First Checkpoint", 0),
            new Waypoint(WAYPOINT_2, "Autumn Garden - Second Checkpoint", 0),
            new Waypoint(WAYPOINT_3, "Autumn Garden - Third Checkpoint", 0),
            new Waypoint(WAYPOINT_4, "Autumn Garden - Fourth Checkpoint", 0),
            new Waypoint(WAYPOINT_5, "Autumn Garden - Fifth Checkpoint", 0),
            new Waypoint(WAYPOINT_6, "Autumn Garden - Sixth Checkpoint", 0),
            new Waypoint(getRandomizedWaypoint7(), "Autumn Garden - Seventh Checkpoint", 0)
        );
    }

    @Override
    protected boolean enterGarden() {
        ScriptConsole.println("Attempting to enter Autumn Garden through door ID: " + AUTUMN_DOOR_ID);
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(AUTUMN_DOOR_ID).option("Open").hidden(false).results();
        if (!results.isEmpty()) {
            results.nearest().interact("Open");
        } else {
            return false;
        }
        return waitForStartPosition();
    }

    @Override
    protected boolean navigateToTree() {
        ScriptConsole.println("Navigating to Autumn Garden tree with guardian tracking");
        List<Waypoint> waypoints = getWaypoints();
        if (waypoints.isEmpty()) return false;
        for (int i = 0; i < waypoints.size(); i++) {
            if(!script.isActive()) break;
            if(!isInAutumnGardenArea()) break;
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
        ScriptConsole.println("Harvesting Autumn sq'irk fruit");
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Sq'irk tree").option("Pick-fruit").results();
        if (!results.isEmpty()) {
            results.nearest().interact("Pick-fruit");
        }
        Execution.delayUntil(8000, ()  -> !AUTUMN_GARDEN_AREA.contains(LocalPlayer.LOCAL_PLAYER) && LocalPlayer.LOCAL_PLAYER.getAnimationId() == -1);
        return true;
    }

    @Override
    protected boolean returnToCenter() {
        ScriptConsole.println("Returning to central garden from Autumn Garden");
        if(Backpack.contains("Broomstick")) {
            Backpack.interact("Broomstick", "Teleport");
            return true;
        }
        ScriptConsole.println("No broomstick found, returning to central garden");
        return false;
    }

    private boolean waitForStartPosition() {
        ScriptConsole.println("Waiting to reach Autumn Garden start position");
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        if (player.getCoordinate().equals(AUTUMN_START)) {
            return true;
        }
        Execution.delayUntil(10000,() -> Client.getLocalPlayer().getCoordinate().equals(AUTUMN_START));
        return true;
    }

    @Override
    public int getDoorId() {
        return AUTUMN_DOOR_ID;
    }

    @Override
    public Coordinate getStartPosition() {
        return AUTUMN_START;
    }

    @Override
    public Coordinate getTreePosition() {
        return AUTUMN_TREE;
    }

    @Override
    public List<Integer> getGuardianIds() {
        return Arrays.stream(AUTUMN_GUARDIAN_IDS).boxed().toList();
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
            default -> {
                return true;
            }
        }

        List<GuardianRequirement> guardianRequirementList = Arrays.asList(guardianRequirements);
        ScriptConsole.println("Waiting for " + guardianRequirementList.size() + " guardian requirements:");
        for (GuardianRequirement requirement : guardianRequirementList) {
            if(!script.isActive()) break;
            if(!isInAutumnGardenArea()) break;
            ScriptConsole.println("  - Guardian " + requirement.getGuardianId());
        }
        guardianTracker.updateGuardianPositions(gardenArea);
        if (!guardianTracker.waitForGuardianRequirements(guardianRequirementList, 120000, gardenArea)) {
            if(!script.isActive()) return false;
            if(!isInAutumnGardenArea()) return false;
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
            default -> {
                return new ArrayList<>();
            }
        }
        return Arrays.asList(guardianRequirements);
    }

    private boolean isInAutumnGardenArea() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && AUTUMN_GARDEN_AREA.contains(player.getCoordinate());
    }

    private Coordinate getRandomizedWaypoint7() {
        if (Math.random() < 0.5) {
            return new Coordinate(WAYPOINT_7.getX() - 1, WAYPOINT_7.getY(), WAYPOINT_7.getZ());
        } else {
            return WAYPOINT_7;
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
        return "AutumnGarden{doorId=" + AUTUMN_DOOR_ID + ", startPosition=" + AUTUMN_START + ", treePosition=" + AUTUMN_TREE + "}";
    }
} 