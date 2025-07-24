package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.script.Execution;

public class WinterSqirkjuiceTask implements Task {
    private final CoaezUtility script;
    private static final String BEER_GLASS_NAME = "Beer glass";
    // {fruit name, required amount}
    private static final Object[][] FRUITS = {
        {"Winter sq'irk", 5},
        {"Spring sq'irk", 4},
        {"Autumn sq'irk", 3},
        {"Summer sq'irk", 2}
    };

    public WinterSqirkjuiceTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        int glassCount = getItemCount(BEER_GLASS_NAME);
        for (Object[] fruit : FRUITS) {
            String fruitName = (String) fruit[0];
            int required = (int) fruit[1];
            int fruitCount = getItemCount(fruitName);
            if (fruitCount >= required && glassCount >= 1) {
                for (Item item : Backpack.getItems()) {
                    if (item != null && fruitName.equals(item.getName())) {
                        squeezeJuice(item.getSlot());
                        return;
                    }
                }
            }
        }
        Bank.loadLastPreset();
    }

    private int getItemCount(String name) {
        int count = 0;
        for (Item item : Backpack.getItems()) {
            if (item != null && name.equals(item.getName())) {
                count += item.getStackSize();
            }
        }
        return count;
    }

    private void squeezeJuice(int backpackSlot) {
        Backpack.interact(backpackSlot, "Squeeze");
        Execution.delay(script.getRandom().nextLong(300, 500));
    }
} 