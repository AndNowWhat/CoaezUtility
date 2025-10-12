package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class ShopDisassemblyTask implements Task {
    private final CoaezUtility script;

    public ShopDisassemblyTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        ScriptConsole.println("[ShopDisassemblyTask] Executing shop disassembly task");
             // Check if backpack contains "Magic stone"
        boolean hasMarbleBlock = Backpack.contains("Magic stone");
        ScriptConsole.println("[ShopDisassemblyTask] Has Magic stone: " + hasMarbleBlock);

        if (hasMarbleBlock) {
            ScriptConsole.println("[ShopDisassemblyTask] Magic stone found, starting disassembly");

            // Use disassembly minimenu [Original]: DoAction(SELECTABLE_COMPONENT, 0, -1, 109445165)
            // onto the item slot [Original]: DoAction(SELECT_COMPONENT_ITEM, 0, 27, 96534533)
            Item marbleBlock = Backpack.getItem("Magic stone");
            if (marbleBlock != null) {
                ScriptConsole.println("[ShopDisassemblyTask] Found Magic stone in slot: " + marbleBlock.getSlot());

                // First interact with the disassembly action
                boolean disassemblySuccess = MiniMenu.interact(SelectableAction.SELECTABLE_COMPONENT.getType(), 0, -1, 109445165);
                ScriptConsole.println("[ShopDisassemblyTask] Disassembly action success: " + disassemblySuccess);

                if (disassemblySuccess) {
                    Execution.delay(script.getRandom().nextLong(600, 1200));

                    // Then interact with the Magic stone item slot
                    boolean itemSuccess = MiniMenu.interact(SelectableAction.SELECT_COMPONENT_ITEM.getType(), 0, marbleBlock.getSlot(), 96534533);
                    ScriptConsole.println("[ShopDisassemblyTask] Item selection success: " + itemSuccess);

                    if (itemSuccess) {
                        Execution.delayUntil(10000, () -> Interfaces.isOpen(847));
                        if(Interfaces.isOpen(847)) {
                            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 55509014);
                            Execution.delayUntil(10000, () -> !Interfaces.isOpen(847));
                        }
                        ScriptConsole.println("[ShopDisassemblyTask] Waiting for interface 1251 to open...");

                        // Wait until interface 1251 is open
                        Execution.delayUntil(1200, () -> Interfaces.isOpen(1251));

                        if (Interfaces.isOpen(1251)) {
                            ScriptConsole.println("[ShopDisassemblyTask] Interface 1251 is open, waiting for it to close...");

                            // Wait until interface 1251 is no longer open
                            Execution.delayUntil(30000, () -> !Interfaces.isOpen(1251));

                            ScriptConsole.println("[ShopDisassemblyTask] Interface 1251 closed, disassembly complete");
                        }
                    }
                }
            }
        } else {
            ScriptConsole.println("[ShopDisassemblyTask] No Magic stone found, checking shop interface");

            // Check if interface 1265 is open
            if (Interfaces.isOpen(1265)) {
                ScriptConsole.println("[ShopDisassemblyTask] Shop interface 1265 is open, buying all items");

                // Interact with the buy all option (component 7, 6)
                boolean buySuccess = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 7, 8, 82903060);
                ScriptConsole.println("[ShopDisassemblyTask] Buy all success: " + buySuccess);
                boolean buySuccess2 = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 7, 8, 82903060);
                ScriptConsole.println("[ShopDisassemblyTask] Buy all success: " + buySuccess2);

                if (buySuccess && buySuccess2) {
                    // Wait until backpack is full
                    Execution.delayUntil(600, () -> Backpack.isFull());
                    ScriptConsole.println("[ShopDisassemblyTask] Backpack is now full");
                }
            } else {
                ScriptConsole.println("[ShopDisassemblyTask] Shop interface not open, finding Sawmill operator");

                // Trade with Sawmill operator NPC
                EntityResultSet<Npc> results = NpcQuery.newQuery().name("Sawmill operator").option("Trade").results();
                if (!results.isEmpty()) {
                    Npc sawmillOperator = results.first();
                    if (sawmillOperator != null) {
                        ScriptConsole.println("[ShopDisassemblyTask] Found Sawmill operator, interacting with Trade option");
                        boolean tradeSuccess = sawmillOperator.interact("Trade");
                        ScriptConsole.println("[ShopDisassemblyTask] Trade interaction success: " + tradeSuccess);

                        if (tradeSuccess) {
                            // Wait for shop interface to open
                            Execution.delayUntil(10000, () -> Interfaces.isOpen(1265));
                            ScriptConsole.println("[ShopDisassemblyTask] Shop interface should now be open");
                        }
                    } else {
                        ScriptConsole.println("[ShopDisassemblyTask] Sawmill operator is null");
                    }
                } else {
                    ScriptConsole.println("[ShopDisassemblyTask] No Sawmill operator found with Trade option");
                }
            }
        }
    }
}
