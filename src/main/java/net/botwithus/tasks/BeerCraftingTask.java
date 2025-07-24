package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;

public class BeerCraftingTask implements Task {
    private final CoaezUtility script;
    private static final int MOLTEN_GLASS_ID = 1775;
    private static final int CRAFTING_INTERFACE_ID = 1370;
    private static final int CONFIRM_INTERFACE_ID = 1371;
    private static final String BEER_PRODUCT_NAME = "Beer";

    public BeerCraftingTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(CONFIRM_INTERFACE_ID)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(script.getRandom().nextLong(1000, 2000));
            return;
        }

        if (hasMoltenGlass()) {
            craftBeer();
        } else {
            Bank.loadLastPreset();
        }
    }

    private boolean hasMoltenGlass() {
        return Backpack.contains(MOLTEN_GLASS_ID);
    }

    private void craftBeer() {
        if(Backpack.contains(MOLTEN_GLASS_ID)) {
            Backpack.interact(MOLTEN_GLASS_ID, "Craft");
            Execution.delayUntil(5000, () -> Interfaces.isOpen(CRAFTING_INTERFACE_ID) || Interfaces.isOpen(CONFIRM_INTERFACE_ID));
        }        
    }
} 