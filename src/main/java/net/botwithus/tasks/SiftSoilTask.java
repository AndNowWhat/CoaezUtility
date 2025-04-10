package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.script.Execution;

import java.util.Arrays;
import java.util.List;

import net.botwithus.rs3.game.actionbar.ActionBar;

public class SiftSoilTask implements Task {
    private final CoaezUtility script;
    
    private static final List<String> SOIL_ITEMS = Arrays.asList(
            "Senntisten soil",
            "Ancient gravel",
            "Fiery brimstone",
            "Saltwater mud",
            "Aerated sediment",
            "Earthen clay",
            "Volcanic ash"
    );
    
    public SiftSoilTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(1371)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(script.getRandom().nextLong(1000, 2000));
            return;
        }

        if (backpackContainsSoil()) {
            selectSoilSpell();
        } else {
            loadBankPreset();
        }
    }
    
    private void selectSoilSpell() {
        ActionBar.useAbility("Sift Soil");
        Execution.delayUntil(5000, () -> Interfaces.isOpen(1371));
    }
    
    public boolean backpackContainsSoil() {
        ResultSet<Item> backpackItems = InventoryItemQuery.newQuery(93).results();
        for (Item item : backpackItems) {
            if (SOIL_ITEMS.contains(item.getName())) {
                return true;
            }
        }
        return false;
    }
    
    private void loadBankPreset() {
        script.setPresetLoaded(false);
        script.setWaitingForPreset(true);
        Bank.loadLastPreset();

        Execution.delayUntil(script.getRandom().nextInt(5000) + 5000, () -> script.isPresetLoaded());
        script.setWaitingForPreset(false);
        Execution.delayUntil(5000, this::backpackContainsSoil);
    }
} 