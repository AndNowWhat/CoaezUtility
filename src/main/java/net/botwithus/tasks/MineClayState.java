package net.botwithus.tasks;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.GameObjectInteractionHelper;

public class MineClayState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[MineClayState] Mining clay underground...");
        
        // Transition if backpack is full
        if (Backpack.isFull()) {
            ScriptConsole.println("[MineClayState] Backpack full, transitioning to upstairs");
            context.setStateToGoUpstairs();
            return;
        }

        // Handle dialog interfaces
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }

        GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
        objectHelper.interactWithClayRock();
    }
}

