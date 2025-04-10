package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.model.Disassembly;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.List;
import java.util.stream.Collectors;

public class DisassemblyTask implements Task {
    private final CoaezUtility script;
    
    public DisassemblyTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        ScriptConsole.println("[DisassemblyTask] Starting with botState: " + script.getBotState());

        if(Interfaces.isOpen(1251)) {
            ScriptConsole.println("[DisassemblyTask] Interface 1251 open, waiting...");
            Execution.delayUntil(100000, () -> !Interfaces.isOpen(1251));
            return;
        }

        ScriptConsole.println("[DisassemblyTask] Items in disassembly list: " + script.getDisassembly().getDisassemblyItems());
        boolean hasItems = script.getDisassembly().hasItemsToDisassemble();
        ScriptConsole.println("[DisassemblyTask] Has items to disassemble: " + hasItems);

        List<Item> backpackItems = Backpack.getItems();
        ScriptConsole.println("[DisassemblyTask] Current backpack items: " + backpackItems.stream()
                .map(Item::getName)
                .collect(Collectors.joining(", ")));

        if (hasItems) {
            ScriptConsole.println("[DisassemblyTask] Casting disassembly");
            script.getDisassembly().castDisassembly();
            Execution.delay(script.getRandom().nextLong(600, 1200));
        } else {
            ScriptConsole.println("[DisassemblyTask] No items found, loading preset");
            Bank.loadLastPreset();
            Execution.delay(script.getRandom().nextLong(1200, 2000));
        }
    }
} 