package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class InventionGizmoTask implements Task {
    private final CoaezUtility script;

    public InventionGizmoTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        ScriptConsole.println("[InventionGizmoTask] Executing invention gizmo crafting task");

        // Check if backpack contains Adamant bar
        boolean hasBucketOfWater = Backpack.contains("Adamant bar");
        ScriptConsole.println("[InventionGizmoTask] Has Adamant bar: " + hasBucketOfWater);

        if (!hasBucketOfWater) {
            ScriptConsole.println("[InventionGizmoTask] No Adamant bar found, loading last bank preset");
            Execution.delay(script.getRandom().nextLong(600, 1200));
            Bank.loadLastPreset();
            Execution.delayUntil(15000, () -> Backpack.contains("Adamant bar"));
            if(!Backpack.contains("Adamant bar")){
                ScriptConsole.println("No bars found after preset loading, stopping");
                script.setActive(false);
            }
            ScriptConsole.println("[InventionGizmoTask] Bank preset loaded, checking for buckets again...");
            return;
        }

        // If interface 1251 is open, wait up to 3 minutes until it's closed
        if (Interfaces.isOpen(1251)) {
            ScriptConsole.println("[InventionGizmoTask] Interface 1251 is open, waiting for it to close (up to 3 minutes)...");

            // Wait up to 3 minutes (180000ms) for interface 1251 to close
            boolean closed = Execution.delayUntil(180000, () -> !Interfaces.isOpen(1251));

            if (closed) {
                ScriptConsole.println("[InventionGizmoTask] Interface 1251 has closed successfully");
            } else {
                ScriptConsole.println("[InventionGizmoTask] Interface 1251 did not close within 3 minutes");
            }
            return;
        }

        // If interface 1370 is open, confirm with specific DoAction
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[InventionGizmoTask] Interface 1370 is open, confirming with dialogue action");

            // Confirm with [Original]: DoAction(DIALOGUE, 0, -1, 89784350)
            boolean confirmSuccess = MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            ScriptConsole.println("[InventionGizmoTask] Confirmation success: " + confirmSuccess);

            if (confirmSuccess) {
                Execution.delay(script.getRandom().nextLong(600, 1200));
            }
            return;
        }

        // Otherwise, open the Inventor's workbench
        ScriptConsole.println("[InventionGizmoTask] Interfaces 1251 and 1370 not open, finding Inventor's workbench");

        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Inventor's workbench").option("Manufacture").results();

        if (!results.isEmpty()) {
            SceneObject workbench = results.first();
            if (workbench != null) {
                ScriptConsole.println("[InventionGizmoTask] Found Inventor's workbench, interacting with Manufacture option");
                boolean manufactureSuccess = workbench.interact("Manufacture");
                ScriptConsole.println("[InventionGizmoTask] Manufacture interaction success: " + manufactureSuccess);

                if (manufactureSuccess) {
                    // Wait for interface to open
                    Execution.delayUntil(10000, () -> Interfaces.isOpen(1251) || Interfaces.isOpen(1370));
                    ScriptConsole.println("[InventionGizmoTask] Interface should now be open");
                }
            } else {
                ScriptConsole.println("[InventionGizmoTask] Inventor's workbench is null");
            }
        } else {
            ScriptConsole.println("[InventionGizmoTask] No Inventor's workbench found with Manufacture option");
        }
    }
}
