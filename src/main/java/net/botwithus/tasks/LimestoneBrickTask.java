package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class LimestoneBrickTask implements Task {
    private final CoaezUtility script;
    
    public LimestoneBrickTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        ScriptConsole.println("[LimestoneBrickTask] Executing limestone brick task");
        
        // Check if we're already making something (interface 1251)
        if (Interfaces.isOpen(1251)) {
            ScriptConsole.println("[LimestoneBrickTask] Interface 1251 is open (crafting in progress), waiting...");
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        // Check if interface 1370 is open (Make-X interface)
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[LimestoneBrickTask] Interface 1370 is open, confirming...");
            // Confirm the make-X interface
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(script.getRandom().nextLong(800, 1500));
            return;
        }
        
        // Check if we have Limestone brick in backpack
        if (!Backpack.contains("Limestone brick")) {
            ScriptConsole.println("[LimestoneBrickTask] No Limestone brick in backpack, loading preset...");
            Bank.loadLastPreset();
            Execution.delay(script.getRandom().nextLong(1200, 2000));
            return;
        }
        
        // Find and interact with Stonecutter
        ScriptConsole.println("[LimestoneBrickTask] Looking for Stonecutter...");
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Stonecutter").option("Cut stone").results();
        SceneObject stonecutter = results.nearest();
        
        if (stonecutter != null) {
            ScriptConsole.println("[LimestoneBrickTask] Found Stonecutter, interacting...");
            if (stonecutter.interact("Cut stone")) {
                ScriptConsole.println("[LimestoneBrickTask] Successfully interacted with Stonecutter");
                // Wait for interface to open or for some time
                Execution.delayUntil(5000L, () -> Interfaces.isOpen(1370));
                Execution.delay(script.getRandom().nextLong(500, 1000));
            } else {
                ScriptConsole.println("[LimestoneBrickTask] Failed to interact with Stonecutter");
                Execution.delay(script.getRandom().nextLong(1000, 2000));
            }
        } else {
            ScriptConsole.println("[LimestoneBrickTask] No Stonecutter found nearby");
            Execution.delay(script.getRandom().nextLong(1000, 2000));
        }
    }
} 