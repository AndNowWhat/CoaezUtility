package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class BuyBucketsWaterTask implements Task {
    private final CoaezUtility script;

    public BuyBucketsWaterTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        ScriptConsole.println("[BuyBucketsWaterTask] Executing buy buckets of water task");

        // Check if backpack is full
        if (Backpack.isFull()) {
            ScriptConsole.println("[BuyBucketsWaterTask] Backpack is full, loading last bank preset");
            Execution.delay(script.getRandom().nextLong(600, 1200));
            Bank.loadLastPreset();
            Execution.delayUntil(15000, () -> !Backpack.isFull());
            return;
        }

        // If backpack is not full, find Shantay NPC and trade
        if (!Interfaces.isOpen(1265)) {
            ScriptConsole.println("[BuyBucketsWaterTask] Shop interface not open, finding Shantay");
            EntityResultSet<Npc> results = NpcQuery.newQuery().name("Shantay").option("Trade").results();

            if (!results.isEmpty()) {
                Npc shantay = results.first();
                if (shantay != null) {
                    ScriptConsole.println("[BuyBucketsWaterTask] Found Shantay, interacting with Trade option");
                    boolean tradeSuccess = shantay.interact("Trade");
                    ScriptConsole.println("[BuyBucketsWaterTask] Trade interaction success: " + tradeSuccess);

                    if (tradeSuccess) {
                        // Wait for shop interface to open
                        Execution.delayUntil(10000, () -> Interfaces.isOpen(1265));
                        ScriptConsole.println("[BuyBucketsWaterTask] Shop interface should now be open");
                    }
                } else {
                    ScriptConsole.println("[BuyBucketsWaterTask] Shantay is null");
                }
            } else {
                ScriptConsole.println("[BuyBucketsWaterTask] No Shantay found with Trade option");
            }
            return;
        }

        // If shop interface is open, buy buckets of water until backpack is full
        if (Interfaces.isOpen(1265)) {
            ScriptConsole.println("[BuyBucketsWaterTask] Shop interface is open, buying buckets of water");

            // Buy buckets of water until backpack is full
            int attempts = 0;
            while (!Backpack.isFull()) {
                attempts++;
                ScriptConsole.println("[BuyBucketsWaterTask] Attempting to buy buckets of water (attempt " + attempts + ")");

                // Add humanlike delay before buying
                Execution.delay(script.getRandom().nextLong(600, 1200));

                // Use the specified DoAction: [Original]: DoAction(N/A, 7, 5, 82903060)
                boolean buySuccess = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 7, 5, 82903060);
                ScriptConsole.println("[BuyBucketsWaterTask] Buy attempt " + attempts + " success: " + buySuccess);

                if (buySuccess) {
                    Execution.delay(script.getRandom().nextLong(600, 800));
                } else {
                    ScriptConsole.println("[BuyBucketsWaterTask] Buy attempt " + attempts + " failed, stopping");
                    break;
                }
            }

            ScriptConsole.println("[BuyBucketsWaterTask] Finished buying attempts after " + attempts + " purchases, current backpack status: " + (Backpack.isFull() ? "full" : "not full"));
        }
    }
}
