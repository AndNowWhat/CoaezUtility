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

public class SoftClayTask implements Task {
    private final CoaezUtility script;
    
    public SoftClayTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        ScriptConsole.println("[SoftClayTask] Executing soft clay task");
        
        // Check if we're already making something (interface 1251)
        if (Interfaces.isOpen(1251)) {
            ScriptConsole.println("[SoftClayTask] Interface 1251 is open (crafting in progress), waiting...");
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        // Check if interface 1370 is open (Make-X interface)
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[SoftClayTask] Interface 1370 is open, confirming...");
            // Confirm the make-X interface
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(script.getRandom().nextLong(800, 1500));
            return;
        }
        
        // Check if we have Clay in backpack
        if (!Backpack.contains("Clay")) {
            ScriptConsole.println("[SoftClayTask] No Clay in backpack, loading preset...");
            Bank.loadLastPreset();
            Execution.delay(script.getRandom().nextLong(1200, 2000));
            return;
        }
        
        // Find and interact with Fountain
        ScriptConsole.println("[SoftClayTask] Looking for Fountain...");
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Fountain").option("Fill").results();
        SceneObject fountain = results.nearest();
        
        if (fountain != null) {
            ScriptConsole.println("[SoftClayTask] Found Fountain, interacting...");
            if (fountain.interact("Fill")) {
                ScriptConsole.println("[SoftClayTask] Successfully interacted with Fountain");
                // Wait for interface to open or for some time
                Execution.delayUntil(5000L, () -> Interfaces.isOpen(1370));
                Execution.delay(script.getRandom().nextLong(500, 1000));
            } else {
                ScriptConsole.println("[SoftClayTask] Failed to interact with Fountain");
                Execution.delay(script.getRandom().nextLong(1000, 2000));
            }
        } else {
            ScriptConsole.println("[SoftClayTask] No Fountain found nearby");
            Execution.delay(script.getRandom().nextLong(1000, 2000));
        }
    }
} 