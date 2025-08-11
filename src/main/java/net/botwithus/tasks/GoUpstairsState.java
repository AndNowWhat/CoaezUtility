package net.botwithus.tasks;

import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.GameObjectInteractionHelper;

public class GoUpstairsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[GoUpstairsState] Going upstairs...");
        
        // Handle dialog interfaces
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
        
        // Try to exit cave
        if (objectHelper.interactWithCaveExit()) {
            ScriptConsole.println("[GoUpstairsState] Exiting cave, transitioning to soften clay");
            context.setStateToSoftenClay();
        } else {
            // If no cave exit found, assume we're already upstairs
            context.setStateToSoftenClay();
        }
    }
}

