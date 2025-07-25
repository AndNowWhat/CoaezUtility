package net.botwithus.tasks.sorceressgarden;

import java.util.HashSet;
import java.util.Set;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.inventories.Bank;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.Task;
import net.botwithus.tasks.sorceressgarden.gardens.AutumnGarden;
import net.botwithus.tasks.sorceressgarden.gardens.BaseGarden;
import net.botwithus.tasks.sorceressgarden.gardens.SpringGarden;
import net.botwithus.tasks.sorceressgarden.gardens.WinterGarden;
import net.botwithus.tasks.sorceressgarden.models.GardenType;

/**
 * Main task coordinator for Sorceress's Garden minigame
 */
public class SorceressGardenTask implements Task {
    private final CoaezUtility script;
    private final GardenManager gardenManager;
    private final GuardianTracker guardianTracker;
    private final PathNavigator pathNavigator;
    
    private Set<GardenType> selectedGardens;
    
    private static final Area CENTRAL_GARDEN_AREA = new Area.Rectangular(new Coordinate(2905, 5478, 0), new Coordinate(2918, 5465, 0));
    private static final Area WINTER_GARDEN_AREA = new Area.Rectangular(new Coordinate(2886, 5487, 0), new Coordinate(2903, 5464, 0));
    private static final Area SPRING_GARDEN_AREA = new Area.Rectangular(new Coordinate(2920, 5479, 0), new Coordinate(2937, 5456, 0));
    private static final Area AUTUMN_GARDEN_AREA = new Area.Rectangular(new Coordinate(2896, 5463, 0), new Coordinate(2919, 5446, 0));
    private static final Area ALKHARID_BANK_AREA = new Area.Rectangular(new Coordinate(3302, 3125, 0), new Coordinate(3309, 3118, 0));
    private static final Area APPRENTICE_AREA = new Area.Rectangular(new Coordinate(3318, 3141, 0), new Coordinate(3324, 3137, 0));
    
    public SorceressGardenTask(CoaezUtility script) {
        this.script = script;
        this.gardenManager = new GardenManager(script);
        this.guardianTracker = new GuardianTracker(script);
        this.pathNavigator = new PathNavigator(script, guardianTracker);
        this.selectedGardens = new HashSet<>();
        
        // Register garden implementations
        gardenManager.registerGarden(new WinterGarden(script));
        gardenManager.registerGarden(new SpringGarden(script));
        gardenManager.registerGarden(new AutumnGarden(script));
        // gardenManager.registerGarden(new SummerGarden(script));

    }
    
    @Override
    public void execute() {        
        try {
            ScriptConsole.println("Executing Sorceress Garden task...");
            
            if (selectedGardens.isEmpty()) {
                ScriptConsole.println("No gardens selected, waiting for selection");
                return;
            }

            if (Backpack.isFull()) {
                ScriptConsole.println("Backpack is full, banking at Shantay-pass");
                handleBankingAndReturn();
                return;
            }

            ScriptConsole.println("Checking if we're in Sorceress's Garden central area or the garden itself...");
            if (!isInSorceressGarden() && (!isInWinterGarden() && !isInSpringGarden() && !isInAutumnGarden())) {
                ScriptConsole.println("Not in Sorceress's Garden central area or gardens, teleporting...");
                teleportToGarden();
                return;
            }
            
            ScriptConsole.println("In Sorceress's Garden central area, executing garden logic...");
            gardenManager.executeCurrentGarden();
            
        } catch (Exception e) {
            ScriptConsole.println("Error in Sorceress Garden task: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if player is in the Sorceress's Garden central area
     */
    private boolean isInSorceressGarden() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("Player is null, cannot check location");
            return false;
        }
        
        boolean inCentralArea = CENTRAL_GARDEN_AREA.contains(player.getCoordinate());
        ScriptConsole.println("Player position: " + player.getCoordinate() + ", In central garden area: " + inCentralArea);
        
        return inCentralArea;
    }

    private boolean isInWinterGarden() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("Player is null, cannot check location");
            return false;
        }

        boolean inWinterGarden = WINTER_GARDEN_AREA.contains(player.getCoordinate());
        ScriptConsole.println("Player position: " + player.getCoordinate() + ", In winter garden area: " + inWinterGarden);
        
        return inWinterGarden;
    }

    private boolean isInSpringGarden() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("Player is null, cannot check location");
            return false;
        }

        boolean inSpringGarden = SPRING_GARDEN_AREA.contains(player.getCoordinate());
        ScriptConsole.println("Player position: " + player.getCoordinate() + ", In spring garden area: " + inSpringGarden);
        
        return inSpringGarden;
    }

    private boolean isInAutumnGarden() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("Player is null, cannot check location");
            return false;
        }

        boolean inAutumnGarden = AUTUMN_GARDEN_AREA.contains(player.getCoordinate());
        ScriptConsole.println("Player position: " + player.getCoordinate() + ", In autumn garden area: " + inAutumnGarden);
        
        return inAutumnGarden;
    }
    
    /**
     * Teleport to the Sorceress's Garden (via apprentice only)
     */
    private void teleportToGarden() {
        ScriptConsole.println("Teleporting to Sorceress's Garden");
        if(Backpack.contains("Broomstick")) {
            ScriptConsole.println("Using broomstick to teleport to Sorceress's Garden");
            Backpack.interact("Broomstick", "Teleport");
            Execution.delayUntil(8000,() -> isInSorceressGarden());
            return;
        } else {
            ScriptConsole.println("No broomstick found, moving to apprentice...");
        }

        if (!isInApprenticeArea()) {
            ScriptConsole.println("Not in apprentice area, navigating to apprentice...");
            navigateToApprentice();
            return;
        }

        EntityResultSet<Npc> apprenticeResults = NpcQuery.newQuery()
            .name("Apprentice")
            .option("Teleport")
            .results();

        if (!apprenticeResults.isEmpty()) {
            ScriptConsole.println("Using apprentice to teleport to Sorceress's Garden");
            apprenticeResults.nearest().interact("Teleport");

            Execution.delayUntil(8000,() -> isInSorceressGarden());

            if (isInSorceressGarden()) {
                ScriptConsole.println("Successfully teleported to Sorceress's Garden central area");
                return;
            }
        } else {
            ScriptConsole.println("Could not find apprentice to teleport");
        }
    }

    /**
     * Handle banking at Shantay-pass and return to apprentice
     */
    private void handleBankingAndReturn() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return;

        if(isInSorceressGarden()) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Fountain").option("Drink-from").results();
            if(!results.isEmpty()) {
                results.nearest().interact("Drink-from");
                Execution.delayUntil(5000, () -> !isInSorceressGarden());
            }
        }

        if (!isInAlKharidBank()) {
            ScriptConsole.println("Navigating to Shantay-pass bank...");
            navigateToAlKharidBank();
            return;
        }

        ScriptConsole.println("At Shantay-pass bank...");
        if(!Bank.isOpen() && Backpack.isFull()) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Shantay chest").option("Open").results();
            if(!results.isEmpty()) {
                results.nearest().interact("Open");
                Execution.delayUntil(5000, () -> Bank.isOpen());
                return;
            }
        } else {
            ScriptConsole.println("Bank is open");
            Bank.depositAll();
            Execution.delayUntil(5000, () -> !Backpack.isFull());
            return;
        }

        if (!isInApprenticeArea()) {
            ScriptConsole.println("Banking done, navigating to apprentice...");
            navigateToApprentice();
        }
    }

    private boolean isInAlKharidBank() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && ALKHARID_BANK_AREA.contains(player.getCoordinate());
    }

    private boolean isInApprenticeArea() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && APPRENTICE_AREA.contains(player.getCoordinate());
    }

    private void navigateToAlKharidBank() {
        Coordinate target = ALKHARID_BANK_AREA.getRandomWalkableCoordinate();
        NavPath path = NavPath.resolve(target, Movement.DISABLE_TELEPORTS);
        if (path != null) {
            Movement.traverse(path);
        } else {
            ScriptConsole.println("No path found to Shantay-pass bank");
        }
    }

    private void navigateToApprentice() {
        ScriptConsole.println("Navigating to apprentice area...");
        Coordinate target = APPRENTICE_AREA.getRandomWalkableCoordinate();
        NavPath path = NavPath.resolve(target, Movement.DISABLE_TELEPORTS);
        if (path != null) {
            Movement.traverse(path);
        } else {
            ScriptConsole.println("No path found to apprentice area");
        }
    }
    
    /**
     * Set the selected gardens from GUI
     */
    public void setSelectedGardens(Set<GardenType> selectedGardens) {
        this.selectedGardens = new HashSet<>(selectedGardens);
        gardenManager.setSelectedGardens(this.selectedGardens);
        ScriptConsole.println("Updated selected gardens: " + this.selectedGardens);
    }
    
    
    /**
     * Stop the Sorceress Garden task
     */
    public void stop() {
        gardenManager.stop();
        ScriptConsole.println("Stopped Sorceress Garden task");
    }

    /**
     * Get the current progress information
     */
    public String getProgress() {
        return gardenManager.getCurrentProgress();
    }
    
    /**
     * Get the current garden being executed
     */
    public GardenType getCurrentGarden() {
        return gardenManager.getCurrentGarden();
    }
    
    /**
     * Get the selected gardens
     */
    public Set<GardenType> getSelectedGardens() {
        return new HashSet<>(selectedGardens);
    }
    
    /**
     * Register a garden implementation
     */
    public void registerGarden(BaseGarden garden) {
        gardenManager.registerGarden(garden);
    }
    
    /**
     * Get the garden manager
     */
    public GardenManager getGardenManager() {
        return gardenManager;
    }
    
    /**
     * Get the guardian tracker
     */
    public GuardianTracker getGuardianTracker() {
        return guardianTracker;
    }
    
    /**
     * Get the path navigator
     */
    public PathNavigator getPathNavigator() {
        return pathNavigator;
    }
    
    /**
     * Reset all gardens
     */
    public void reset() {
        gardenManager.reset();
        ScriptConsole.println("Reset all gardens");
    }
    
    /**
     * Check if any garden is currently active
     */
    public boolean isGardenActive() {
        return gardenManager.isGardenActive();
    }
    
    @Override
    public String toString() {
        return String.format("SorceressGardenTask{selectedGardens=%d, currentGarden=%s}", 
                           selectedGardens.size(), getCurrentGarden());
    }
} 