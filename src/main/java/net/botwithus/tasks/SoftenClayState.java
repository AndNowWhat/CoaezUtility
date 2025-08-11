package net.botwithus.tasks;

import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.GameObjectInteractionHelper;
import net.botwithus.tasks.clayurn.InventoryHelper;

public class SoftenClayState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[SoftenClayState] Softening clay at sink...");
        
        // Handle dialog interfaces
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        // Handle sink interface
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[SoftenClayState] Sink interface is open, confirming softening clay");
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            return;
        }
        
        // Check if we still have regular clay to soften
        if (!InventoryHelper.hasRegularClay()) {
            ScriptConsole.println("[SoftenClayState] No regular clay in backpack, moving to spin urns");
            context.setStateToSpinUrns();
            return;
        }
        
        // Interact with sink
        GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
        if (objectHelper.interactWithSink()) {
            ScriptConsole.println("[SoftenClayState] Using sink to soften clay");
            Execution.delayUntil(10000, () -> Interfaces.isOpen(1370));
        }
    }
}

