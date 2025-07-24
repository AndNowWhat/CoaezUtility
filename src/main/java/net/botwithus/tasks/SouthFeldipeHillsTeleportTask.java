package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.queries.builders.characters.PlayerQuery;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;

import java.util.Random;

import static net.botwithus.rs3.script.ScriptConsole.println;

public class SouthFeldipeHillsTeleportTask implements Task {
    private final CoaezUtility script;
    private final Random random;
    private static final String TELEPORT_NAME = "Camelot Teleport";

    public SouthFeldipeHillsTeleportTask(CoaezUtility script) {
        this.script = script;
        this.random = new Random();
    }

    @Override
    public void execute() {
        // Check if the teleport ability is on the action bar and not on cooldown
        if (ActionBar.containsAbility(TELEPORT_NAME) && ActionBar.getCooldown(TELEPORT_NAME) <= 0) {
            println("Using " + TELEPORT_NAME);
            
            // Use the teleport ability
            if (ActionBar.useAbility(TELEPORT_NAME)) {
                println("Successfully used " + TELEPORT_NAME);

                Execution.delayUntil(random.nextInt(1800) + 200, () -> LocalPlayer.LOCAL_PLAYER.getAnimationId() == -1);
            } else {
                println("Failed to use " + TELEPORT_NAME);
            }
        }
    }
}
