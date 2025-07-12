package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class DeployDummyTask implements Task {
    private final CoaezUtility script;
    private int lastPlayerAnimation = -1;
    private long lastAnimationChangeTime = 0;
    private static final long ANIMATION_CHECK_DURATION = 10000; // 10 seconds in milliseconds
    
    public DeployDummyTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[DeployDummyTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }

        if(Interfaces.isOpen(847)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 55509005);
            Execution.delay(1200);
            return;
        }
        
        // Track player animation changes
        int currentAnimation = player.getAnimationId();
        long currentTime = System.currentTimeMillis();
        
        // Check if animation has changed
        if (currentAnimation != lastPlayerAnimation) {
            lastPlayerAnimation = currentAnimation;
            lastAnimationChangeTime = currentTime;
            ScriptConsole.println("[DeployDummyTask] Player animation changed to: " + currentAnimation);
        }
        
        // Check if animation hasn't changed in the last 10 seconds
        boolean animationStale = (currentTime - lastAnimationChangeTime) > ANIMATION_CHECK_DURATION;
        ScriptConsole.println("[DeployDummyTask] Animation stale (>10s): " + animationStale + 
                            " (Time since change: " + (currentTime - lastAnimationChangeTime) + "ms)");
        
        if (animationStale) {
            // Check if there's a dummy within 3 tiles that we can practice with
            EntityResultSet<Npc> nearbyDummies = NpcQuery.newQuery()
                .name("Agility skill training dummy")
                .option("Practice")
                .results();
                
            Npc nearbyDummy = null;
            for (Npc dummy : nearbyDummies) {
                if (dummy.distanceTo(player) <= 3) {
                    nearbyDummy = dummy;
                    break;
                }
            }
            
            if (nearbyDummy != null) {
                ScriptConsole.println("[DeployDummyTask] Found dummy within 3 tiles, interacting with Practice");
                if (nearbyDummy.interact("Practice")) {
                    ScriptConsole.println("[DeployDummyTask] Successfully started practicing with dummy");
                    // Reset animation tracking since we started a new activity
                    lastAnimationChangeTime = currentTime;
                    Execution.delay(script.getRandom().nextLong(1500, 3000));
                } else {
                    ScriptConsole.println("[DeployDummyTask] Failed to interact with dummy");
                    Execution.delay(1000);
                }
            } else {
                ScriptConsole.println("[DeployDummyTask] No dummy within 3 tiles, deploying new one");
                deployDummy();
            }
        } else {
            // Animation has changed recently, just wait
            ScriptConsole.println("[DeployDummyTask] Player animation changed recently, waiting...");
            Execution.delay(1000);
        }
    }
    
    private void deployDummy() {
        ScriptConsole.println("[DeployDummyTask] Attempting to auto-deploy agility dummy");
        
        if (Backpack.contains("Agility skill training dummy")) {
            ScriptConsole.println("[DeployDummyTask] Found dummy in backpack, using auto-deploy...");
            
            // Use Auto-deploy directly from backpack
            if (Backpack.interact("Agility skill training dummy", "Auto-deploy")) {
                ScriptConsole.println("[DeployDummyTask] Auto-deploy activated successfully!");
                
                // Reset animation tracking since we're deploying
                lastAnimationChangeTime = System.currentTimeMillis();
                
                // Wait a bit for auto-deploy to activate
                Execution.delay(script.getRandom().nextLong(5000, 8000));
            } else {
                ScriptConsole.println("[DeployDummyTask] Failed to activate auto-deploy");
            }
        } else {
            ScriptConsole.println("[DeployDummyTask] No agility dummy found in backpack");
        }
    }
} 