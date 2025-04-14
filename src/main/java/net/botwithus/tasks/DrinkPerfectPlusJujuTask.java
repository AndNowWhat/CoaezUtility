package net.botwithus.tasks;

import java.util.regex.Pattern;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.util.Regex;

public class DrinkPerfectPlusJujuTask implements Task {
    private final CoaezUtility script;
    private static final int PERFECT_PLUS_JUJU_VARBIT = 26026;
    private static final Pattern PERFECT_PLUS_JUJU_PATTERN = Regex.getPatternForContainingOneOf("Perfect plus potion");
    public DrinkPerfectPlusJujuTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        script.println("Executing DrinkPerfectPlusJujuTask");
        int perfectPlusJujuValue = VarManager.getVarbitValue(PERFECT_PLUS_JUJU_VARBIT);
        if (perfectPlusJujuValue == 0 && hasPerfectPlusJuju()) {
            script.println("Found perfect plus juju potion, attempting to drink");
            boolean result = drinkPerfectPlusJuju();
            script.println("Perfect juju plus drink attempt result: " + result);
        }
    }

    private boolean hasPerfectPlusJuju() {
        boolean hasPerfectPlusJuju = Backpack.contains(PERFECT_PLUS_JUJU_PATTERN);
        script.println("Has perfect plus juju: " + hasPerfectPlusJuju);
        return hasPerfectPlusJuju;
    }

    private boolean drinkPerfectPlusJuju() {
        if (Backpack.contains(PERFECT_PLUS_JUJU_PATTERN)) {
            script.println("Found perfect plus juju potion, attempting to drink");
            boolean result = Backpack.interact(PERFECT_PLUS_JUJU_PATTERN, "Drink");
            return result;
        }
        return false;
    }
} 