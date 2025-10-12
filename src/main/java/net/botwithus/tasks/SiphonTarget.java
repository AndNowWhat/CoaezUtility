package net.botwithus.tasks;

import java.util.Objects;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.util.RandomGenerator;

public class SiphonTarget implements Task {
    
    private final WorldHopService worldHopService = new WorldHopService();
    
    private enum State {
        FIND_NEX,
        INTERACT_NEX,
        FIND_SIEGE_ENGINE,
        WAIT_FOR_TARGET_PROXIMITY,
        INTERACT_COMPONENT
    }
    
    private State currentState = State.FIND_NEX;
    private final Coordinate siegeEngineCoordinate = new Coordinate(1695, 1240, 0);
    private final Coordinate targetCoordinate = new Coordinate(1703, 1248, 0);
    private int componentInteractionCount = 0;
    boolean siegeEngineAvailable = false;

    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return;
        }
        
        switch (currentState) {
            case FIND_NEX:
                findAndInteractWithNex();
                break;
                
            case INTERACT_NEX:
                waitForNexProximity();
                break;
                
            case FIND_SIEGE_ENGINE:
                findAndAttackSiegeEngine();
                break;
                
            case WAIT_FOR_TARGET_PROXIMITY:
                waitForTargetProximity();
                break;
                
            case INTERACT_COMPONENT:
                interactWithComponent();
                break;
        }
    }
    
    private void findAndInteractWithNex() {
        ScriptConsole.println("[SiphonTarget] Looking for Nex with Battlefield ability option");
        EntityResultSet<Npc> results = NpcQuery.newQuery().name("Nex").option("Battlefield ability").results();

        if (!results.isEmpty()) {
            Npc nex = results.first();
            if (nex != null) {
                ScriptConsole.println("[SiphonTarget] Found Nex, interacting with Battlefield ability");
                if (nex.interact("Battlefield ability")) {
                    currentState = State.INTERACT_NEX;
                }
            }
        } else {
            ScriptConsole.println("[SiphonTarget] No Nex found with Battlefield ability option");
            Execution.delay(RandomGenerator.nextInt(1000, 2000));
        }
    }
    
    private void waitForNexProximity() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return;
        
        EntityResultSet<Npc> results = NpcQuery.newQuery().name("Nex").results();
        if (!results.isEmpty()) {
            Npc nex = results.first();
            if (nex != null && player.getCoordinate().distanceTo(nex.getCoordinate()) <= 1) {
                ScriptConsole.println("[SiphonTarget] Player is within 1 tile of Nex, moving to next state");
                currentState = State.FIND_SIEGE_ENGINE;
            }
        }
    }
    
    private void findAndAttackSiegeEngine() {
        ScriptConsole.println("[SiphonTarget] Looking for Siege engine at coordinates " + siegeEngineCoordinate);
        EntityResultSet<Npc> results = NpcQuery.newQuery().name("Siege engine").option("Attack").results();

        if (!results.isEmpty()) {
            for (Npc siegeEngine : results) {
                if (Objects.equals(siegeEngine.getCoordinate(), siegeEngineCoordinate)) {
                    ScriptConsole.println("[SiphonTarget] Found Siege engine at correct coordinates, attacking");
                    // On the 4th overall component interaction (0-based count == 3), swap to Staff of air before we move to the tile
                    if (componentInteractionCount == 3) {
                        ScriptConsole.println("[SiphonTarget] Swapping to Staff of air before final component interaction");
                        Backpack.interact("Staff of air", "Wield");
                    }
                    if (siegeEngine.interact("Attack")) {
                        currentState = State.WAIT_FOR_TARGET_PROXIMITY;
                        return;
                    }
                }
            }
        }

        ScriptConsole.println("[SiphonTarget] No Siege engine found at specified coordinates");

        // Handle world hopping when siege engine is not available
        if (worldHopService.isWorldHopEnabled()) {
            ScriptConsole.println("[SiphonTarget] World hopping enabled, hopping to next world");
            int nextWorld = worldHopService.getNextAvailableWorld();
            ScriptConsole.println("[SiphonTarget] Switching to world " + nextWorld);

            LoginManager.setAutoLogin(true);
            LoginManager.setWorld(nextWorld);
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93913156);
            Execution.delayUntil(3000, () -> Client.getGameState() != Client.GameState.LOGGED_IN);
            ScriptConsole.println("[SiphonTarget] Logged out, will auto-login to world " + nextWorld);
        } else {
            ScriptConsole.println("[SiphonTarget] World hopping disabled, waiting for respawn");
            Execution.delay(RandomGenerator.nextInt(2000, 4000));
        }
    }
    
    
    private void waitForTargetProximity() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return;
        
        if (player.getServerCoordinate().distanceTo(targetCoordinate) <= 1) {
            ScriptConsole.println("[SiphonTarget] Player reached target coordinates naturally, starting component interaction");
            currentState = State.INTERACT_COMPONENT;
        } else {
            Execution.delay(RandomGenerator.nextInt(300, 600));
        }
    }
    
    private void interactWithComponent() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return;
        
        if (player.getServerCoordinate().distanceTo(targetCoordinate) <= 1) {
            ScriptConsole.println("[SiphonTarget] Interacting with component (count=" + componentInteractionCount + ")");
            if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 48693249)) {
                componentInteractionCount++;
                ScriptConsole.println("[SiphonTarget] Component interaction successful (new count=" + componentInteractionCount + ")");

                // After the 4th interaction the target dies → reset and swap back to regular Staff
                if (componentInteractionCount >= 4) {
                    ScriptConsole.println("[SiphonTarget] Final interaction done, swapping back to Staff and resetting counter");
                    Backpack.interact("Staff", "Wield");
                    componentInteractionCount = 0;
                }
                currentState = State.FIND_NEX;
            } else {
                ScriptConsole.println("[SiphonTarget] Component interaction failed");
                Execution.delay(RandomGenerator.nextInt(500, 1200));
            }
        } else {
            ScriptConsole.println("[SiphonTarget] Player moved away from target location, waiting for proximity");
            currentState = State.WAIT_FOR_TARGET_PROXIMITY;
        }
    }
    
    // World hopping control methods
    public void setWorldHopEnabled(boolean enabled) {
        worldHopService.setWorldHopEnabled(enabled);
    }
    
    public boolean isWorldHopEnabled() {
        return worldHopService.isWorldHopEnabled();
    }
    
    public void setHopDelayMs(int delayMs) {
        worldHopService.setHopDelay(delayMs);
    }
    
    public int getHopDelayMs() {
        return worldHopService.getHopDelayMs();
    }
}