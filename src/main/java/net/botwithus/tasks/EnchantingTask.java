package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import static net.botwithus.rs3.util.Regex.getPatternForContainsString;
import static net.botwithus.rs3.util.Regex.getPatternForNotContainingAnyString;

public class EnchantingTask implements Task {
    private final CoaezUtility script;

    public EnchantingTask(CoaezUtility script) {
        this.script = script;
    }

    private BoltInventoryState checkBoltInventoryState() {
        return new BoltInventoryState();
    }

    private class BoltInventoryState {
        final int unenchantedBoltsCount;
        final int enchantedBoltsCount;

        public BoltInventoryState() {
            this.unenchantedBoltsCount = countUnenchantedBolts();
            this.enchantedBoltsCount = countEnchantedBolts();
        }

        private int countUnenchantedBolts() {
            return Backpack.getItems().stream()
                    .filter(item -> item.getName().contains("bolts") && !item.getName().contains("(e)"))
                    .mapToInt(item -> item.getStackSize())
                    .sum();
        }

        private int countEnchantedBolts() {
            return Backpack.getItems().stream()
                    .filter(item -> item.getName().contains("bolts") && item.getName().contains("(e)"))
                    .mapToInt(item -> item.getStackSize())
                    .sum();
        }

        public void logState() {
            ScriptConsole.println("==== Bolt Inventory State ====");
            ScriptConsole.println("Unenchanted bolts: " + unenchantedBoltsCount);
            ScriptConsole.println("Enchanted bolts: " + enchantedBoltsCount);
            ScriptConsole.println("==============================");
        }
    }

    @Override
    public void execute() {
        ScriptConsole.println("[EnchantingTask] Executing enchanting task");
        BoltInventoryState boltState = checkBoltInventoryState();
        boltState.logState();

        if (boltState.unenchantedBoltsCount == 0) {
            ScriptConsole.println("[EnchantingTask] No unenchanted bolts found in backpack, stopping task");
            return;
        }

        if (Interfaces.isOpen(1251)) {
            ScriptConsole.println("[EnchantingTask] Interface 1251 is open, waiting...");
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[EnchantingTask] Interface 1370 is open, interacting with button");
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(script.getRandom().nextLong(1000, 2000));
        } else {
            ScriptConsole.println("[EnchantingTask] Using Enchant Crossbow Bolt ability");
            ActionBar.useAbility("Enchant Crossbow Bolt");
            Execution.delayUntil(5000, () -> Interfaces.isOpen(1370));
        }
        
    }
}