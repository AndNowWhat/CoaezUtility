package net.botwithus.tasks;

import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.InventoryHelper;

public class AddRunesState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[AddRunesState] Adding runes to fired urns...");
        
        if (context.isSkipAddRunes()) {
            ScriptConsole.println("[AddRunesState] Skipping adding runes, transitioning to deposit urns");
            context.setStateToDepositUrns();
            return;
        }
        
        // Check if we have urns that need runes
        Item urnItem = InventoryHelper.getFirstUrnNeedingRunes();
        if (urnItem == null) {
            ScriptConsole.println("[AddRunesState] No fired urns needing runes found, transitioning to deposit");
            context.setStateToDepositUrns();
            return;
        }

        // Handle dialog interfaces
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[AddRunesState] Sink interface is open, confirming softening clay");
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            return;
        }

        // Try to add runes to the urn
        if (InventoryHelper.addRunesToUrn(urnItem)) {
            ScriptConsole.println("[AddRunesState] Successfully interacted with urn to add runes");
            Execution.delay(1500L);
        } else {
            ScriptConsole.println("[AddRunesState] Could not interact with urn to add rune");
        }
    }
}

