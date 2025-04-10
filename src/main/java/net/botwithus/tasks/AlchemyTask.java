package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.model.Alchemy;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class AlchemyTask implements Task {
    private final CoaezUtility script;
    
    public AlchemyTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        ScriptConsole.println("[AlchemyTask] Executing alchemy task");
        
        if (script.getAlchemy().hasItemsToAlchemize()) {
            ScriptConsole.println("[AlchemyTask] Casting alchemy");
            script.getAlchemy().castAlchemy();
            Execution.delay(script.getRandom().nextLong(300, 600));
        } else {
            ScriptConsole.println("[AlchemyTask] No items to alchemize, loading preset");
            Bank.loadLastPreset();
            Execution.delay(script.getRandom().nextLong(1200, 2000));
        }
    }
} 