package net.botwithus.tasks;

import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.GameObjectInteractionHelper;
import net.botwithus.tasks.clayurn.InventoryHelper;
import net.botwithus.tasks.clayurn.UrnCraftingManager;
import net.botwithus.tasks.clayurn.UrnType;

public class SpinUrnsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTaskRefactored context) {
        ScriptConsole.println("[SpinUrnsState] Spinning urns...");
        
        // Check if we have enough soft clay and urns in queue
        if (!context.getUrnDataManager().isDataLoaded() || context.getUrnQueue().isEmpty()) {
            ScriptConsole.println("[SpinUrnsState] No urns in queue or data not loaded");
            Execution.delay(1000);
            return;
        }

        // Handle dialog interfaces
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }

        int softClayCount = InventoryHelper.getSoftClayCount();
        if (softClayCount < 2) {
            ScriptConsole.println("[SpinUrnsState] Not enough soft clay (need at least 2), transitioning to firing");
            context.setStateToFireUrns();
            return;
        }

        // Get the next urn to craft from queue
        UrnType nextUrn = context.getUrnQueue().getNextUrn();
        context.setSelectedUrn(nextUrn);

        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[SpinUrnsState] Pottery wheel interface is open, handling urn selection");
            UrnCraftingManager craftingManager = new UrnCraftingManager(context.getScript());
            craftingManager.handlePotteryWheelInterface(nextUrn);
            
            // Check if we should continue or transition after crafting
            handlePostCrafting(context, craftingManager, nextUrn);
        } else {
            // Try to open pottery wheel interface
            GameObjectInteractionHelper objectHelper = new GameObjectInteractionHelper(context.getScript());
            if (objectHelper.interactWithPotteryWheel()) {
                ScriptConsole.println("[SpinUrnsState] Interacting with pottery wheel");
            } else {
                ScriptConsole.println("[SpinUrnsState] Pottery wheel not found");
                Execution.delay(1000L);
            }
        }
    }
    
    private void handlePostCrafting(ClayUrnTaskRefactored context, UrnCraftingManager craftingManager, UrnType urnType) {
        // Wait for crafting to complete
        Execution.delay(3000L);
        
        // Check if we still have enough soft clay for another urn
        if (!InventoryHelper.hasSoftClay()) {
            ScriptConsole.println("[SpinUrnsState] No more soft clay, transitioning to firing");
            context.setStateToFireUrns();
            return;
        }
        
        int requiredSoftClay = craftingManager.getRequiredSoftClayForUrn(urnType);
        int softClayCount = InventoryHelper.getSoftClayCount();
        
        if (softClayCount < requiredSoftClay) {
            ScriptConsole.println("[SpinUrnsState] Not enough soft clay for another urn, transitioning to firing");
            context.setStateToFireUrns();
        }
    }
}

