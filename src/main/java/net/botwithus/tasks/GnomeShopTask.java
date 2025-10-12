package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.Shop;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.Random;

public class GnomeShopTask implements Task {
    private final CoaezUtility script;
    private int bankVisitCounter = 0;  // To track how many times we've gone to the bank
    Random random = new Random();
    int delayTime = 500000 + random.nextInt(100000);  // 200,000 + a random number from 0 to 99,999

    public GnomeShopTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        ScriptConsole.println("[GnomeShopTask] Executing gnome shop food buying task");

        if (Backpack.isFull()) {
            ScriptConsole.println("[GnomeShopTask] Backpack is full, loading last bank preset");
            Bank.loadLastPreset();
            Execution.delayUntil(15000L, () -> !Backpack.isFull());

            // Increment the bank visit counter
            bankVisitCounter++;

            // After 5 bank visits, wait for 5 minutes before continuing
            if (bankVisitCounter >= 4) {
                ScriptConsole.println("[GnomeShopTask] Visited bank 4 times, waiting for 3-5 minutes...");
                Execution.delay(delayTime);  // 5 minutes in milliseconds (300,000 ms)
                bankVisitCounter = 0;  // Reset counter after waiting
            }

            return;
        }

        if (!Shop.isOpen()) {
            ScriptConsole.println("[GnomeShopTask] Shop interface not open, finding Gnome Waiter");
            EntityResultSet<Npc> results = NpcQuery.newQuery().name("Gnome Waiter").option("Talk-to").results();
            if (!results.isEmpty()) {
                Npc gnomeWaiter = results.first();
                if (gnomeWaiter != null) {
                    ScriptConsole.println("[GnomeShopTask] Found Gnome Waiter, interacting with Trade option");
                    if (gnomeWaiter.interact("Trade")) {
                        Execution.delayUntil(10000L, Shop::isOpen);
                    }
                } else {
                    ScriptConsole.println("[GnomeShopTask] Gnome Waiter is null");
                }
            } else {
                ScriptConsole.println("[GnomeShopTask] No Gnome Waiter found with Talk-to option");
            }
            return;
        }

        if (Shop.isOpen()) {
            ScriptConsole.println("[GnomeShopTask] Shop is open, buying all available food");
            if (Shop.buyAll()) {
                ScriptConsole.println("[GnomeShopTask] Successfully bought food items");
                Execution.delay(1500L);
            } else {
                ScriptConsole.println("[GnomeShopTask] No food items available or failed to buy");
                Execution.delay(500L);
            }
        }
    }
}
