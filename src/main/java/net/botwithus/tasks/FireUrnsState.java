package net.botwithus.tasks;

import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.GameObjectInteractionHelper;
import net.botwithus.tasks.clayurn.InventoryHelper;

public class FireUrnsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[FireUrnsState] Handling firing urns...");
        
        // Check if we have unfired urns to fire
        if (!InventoryHelper.hasUnfiredUrns()) {
            ScriptConsole.println("[FireUrnsState] No unfired urns found, transitioning to add runes");
            context.setStateToAddRunes();
            return;
        }
        
        // Handle dialog interfaces
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        // Handle pottery oven interface
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[FireUrnsState] Pottery oven interface is open, confirming firing");
            confirmFiringUrns(context);
            return;
        }
        
        // Try to interact with pottery oven
        GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
        if (objectHelper.interactWithPotteryOven()) {
            ScriptConsole.println("[FireUrnsState] Found pottery oven, attempting to fire urns");
        } else {
            ScriptConsole.println("[FireUrnsState] Pottery oven not found");
            Execution.delay(1000L);
        }
    }
    
    private void confirmFiringUrns(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[FireUrnsState] Confirming firing using dialogue action (component ID: 89784350)");
        
        if (MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350)) {
            ScriptConsole.println("[FireUrnsState] Successfully started firing urns");
            
            // Wait for firing to complete
            Execution.delay(3000L);
            
            // Check if we still have unfired urns
            if (!InventoryHelper.hasUnfiredUrns()) {
                ScriptConsole.println("[FireUrnsState] All urns fired, transitioning to deposit");
                context.setStateToDepositUrns();
            }
        } else {
            ScriptConsole.println("[FireUrnsState] Failed to start firing using dialogue action");
        }
    }
}

