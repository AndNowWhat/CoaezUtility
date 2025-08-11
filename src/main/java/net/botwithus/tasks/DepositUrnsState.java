package net.botwithus.tasks;

import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.GameObjectInteractionHelper;
import net.botwithus.tasks.clayurn.InventoryHelper;

public class DepositUrnsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[DepositUrnsState] Handling depositing urns...");

        // If skipping add runes, just deposit all items
        if (context.isSkipAddRunes()) {
            handleSkipAddRunesDeposit(context);
            return;
        }

        // Normal deposit logic for completed urns
        handleNormalDeposit(context);
    }
    
    private void handleSkipAddRunesDeposit(ClayUrnTaskRefactored context) {
        GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
        
        if (!net.botwithus.api.game.hud.inventories.DepositBox.isOpen()) {
            if (!objectHelper.interactWithDepositBox()) {
                ScriptConsole.println("[DepositUrnsState] Deposit box not found or failed to interact, using bank preset");
                Bank.loadLastPreset();
                Execution.delay(6000L);
                return;
            }
        }
        
        ScriptConsole.println("[DepositUrnsState] Deposit box is open, depositing all items...");
        InventoryHelper.depositAllItems();
        ScriptConsole.println("[DepositUrnsState] All items deposited.");
        context.setStateToMineClay();
    }
    
    private void handleNormalDeposit(ClayUrnTaskRefactored context) {
        GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
        
        if (!net.botwithus.api.game.hud.inventories.DepositBox.isOpen()) {
            if (!objectHelper.interactWithDepositBox()) {
                ScriptConsole.println("[DepositUrnsState] Deposit box not found or failed to interact, using bank preset");
                Bank.loadLastPreset();
                Execution.delay(6000L);
                return;
            }
        }

        ScriptConsole.println("[DepositUrnsState] Deposit box is open, attempting to deposit urns...");
        
        boolean allDeposited = InventoryHelper.depositSpecificUrns(context.getUrnQueue());
        
        if (allDeposited) {
            ScriptConsole.println("[DepositUrnsState] All urns should be deposited.");
        } else {
            ScriptConsole.println("[DepositUrnsState] Some urns were not deposited or an error occurred.");
        }
        
        if (!InventoryHelper.hasRemainingUrns()) {
            ScriptConsole.println("[DepositUrnsState] No urns remain in backpack after deposit.");
            context.setStateToMineClay();
        } else {
            ScriptConsole.println("[DepositUrnsState] Urns still in backpack after deposit: " + 
                    InventoryHelper.getRemainingUrnNames());
        }
        
        Execution.delay(1000L);
    }
}

