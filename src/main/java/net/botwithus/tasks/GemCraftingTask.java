package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.script.Execution;

import java.util.regex.Pattern;

public class GemCraftingTask implements Task {
    private final CoaezUtility script;
    
    public GemCraftingTask(CoaezUtility script) {
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

        if (hasUncutGems()) {
            craftGems();
        } else {
            Bank.loadLastPreset();
        }
    }
    
    private boolean hasUncutGems() {
        ResultSet<Item> backpackItems = InventoryItemQuery.newQuery(93).results();
        for (Item item : backpackItems) {
            if (item.getName().contains("Uncut")) {
                return true;
            }
        }
        return false;
    }
    
    private void craftGems() {
        Item gem = InventoryItemQuery.newQuery(93).option("Craft").results().first();
        if (gem != null) {
            if (inventoryInteract("Craft", gem.getName())) {
                Execution.delayUntil(5000, () -> Interfaces.isOpen(1371));
            }
        }
    }
    
    private boolean inventoryInteract(String option, String... items) {
        Pattern pattern = Pattern.compile(String.join("|", items), Pattern.CASE_INSENSITIVE);
        Item item = InventoryItemQuery.newQuery().name(pattern).results().first();

        if (item != null) {
            String itemName = item.getName();
            Component itemComponent = net.botwithus.rs3.game.queries.builders.components.ComponentQuery.newQuery(1473).componentIndex(5).itemName(itemName).results().first();
            if (itemComponent != null) {
                return itemComponent.interact(option);
            }
        }
        return false;
    }
} 