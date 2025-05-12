package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class FungalBowstrings implements Task {

    private final CoaezUtility script;

    private static final String TEMPERED_FUNGAL_SHAFTS = "Tempered fungal shaft";
    private static final int CRAFTING_INTERFACE_ID = 1251;
    private static final int CONFIRMATION_INTERFACE_ID = 1370;
    private static final int MAKE_X_INTERFACE_ID = 1371;
    private static final int CONFIRMATION_DIALOGUE_ACTION = 89784350;

    public FungalBowstrings(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        // 1. Check for Tempered fungal shafts
        if (!Backpack.contains(TEMPERED_FUNGAL_SHAFTS)) {
            ScriptConsole.println("Stopping: No " + TEMPERED_FUNGAL_SHAFTS);
            Execution.delay(600);
            return;
        }

        // 2. Check if already crafting
        if (Interfaces.isOpen(CRAFTING_INTERFACE_ID)) {
            Execution.delay(600);
            return;
        }

        // 3. Handle confirmation dialog
        if (Interfaces.isOpen(CONFIRMATION_INTERFACE_ID)) {
            boolean interacted = MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, CONFIRMATION_DIALOGUE_ACTION);
            if (interacted) {
                ScriptConsole.println("Confirmed crafting.");
                Execution.delayUntil(3000, () -> Interfaces.isOpen(MAKE_X_INTERFACE_ID) || !Interfaces.isOpen(CONFIRMATION_INTERFACE_ID));
                return;
            }
            return;
        }
        

        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Spinning wheel").option("Spin").results();
        SceneObject spinningWheel = results.first();

        if (spinningWheel != null) {
            ScriptConsole.println("Interacting with Spinning wheel.");
            if (spinningWheel.interact("Spin")) {
                Execution.delayUntil(5000, () -> Interfaces.isOpen(MAKE_X_INTERFACE_ID) || Interfaces.isOpen(CONFIRMATION_INTERFACE_ID));
            }
        } else {
            ScriptConsole.println("Spinning wheel not found.");
            Execution.delay(1500);
        }
    }
}
