package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.Dialog;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import static net.botwithus.rs3.script.ScriptConsole.println;

public class InventionTask implements Task {
    private final CoaezUtility script;
    
    public InventionTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        
        if (Interfaces.isOpen(1251)) {
            println("Interface 1251 is open. Waiting for it to close.");
            Execution.delayUntil(2200, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Backpack.contains("Ancient weapon gizmo shell", 25) && !Backpack.isFull()) {
            println("Ancient weapon gizmo shell found. Adding materials.");
            Backpack.interact("Ancient weapon gizmo shell", "Add materials");
            Execution.delayUntil(1200, () -> Interfaces.isOpen(1712));

            if (Interfaces.isOpen(1712)) {
                println("Interface 1712 is open. Performing mini-menu interactions.");
                for (int i = 0; i < 9; i++) {
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 77, 112197638);
                    Execution.delay(300);
                }
                println("Confirming action.");
                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 1, -1, 112197656);
                Execution.delay(600);
                while (!Backpack.isFull() && script.isActive() && Interfaces.isOpen(1712)){
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 112197673);
                    Execution.delay(300);
                }
            }
        }
        
        if (Backpack.isFull()) {
            println("Backpack is full. Attempting to disassemble Ancient weapon gizmo.");
            if (Backpack.interact("Ancient weapon gizmo", "Disassemble")) {
                Execution.delayUntil(1200, () -> Interfaces.isOpen(1183));
            } else {
                println("Failed to disassemble Ancient weapon gizmo.");
            }
            if (Interfaces.isOpen(1183)) {
                println("Interface 1183 is open. Looking for 'All' button.");
                Component allButton = ComponentQuery.newQuery(1183).componentIndex(25).text("All").results().first();
                if (allButton != null) {
                    println("'All' button found. Interacting.");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77529095);
                    Execution.delayUntil(1800, () -> !Interfaces.isOpen(1183));
                    return;
                }
            }

        }

        if (Interfaces.isOpen(1370) || Interfaces.isOpen(1371)) {
                Component manufactureButton = ComponentQuery.newQuery(1370).componentIndex(30).results().first();
                if (manufactureButton != null) {
                    println("Manufacture button found. Interacting.");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
                    Execution.delayUntil(1200, () -> Interfaces.isOpen(1251));
                    return;
                } else {
                    println("Manufacture button not found.");
                }
            }

        println("Searching for Inventor's workbench.");
        EntityResultSet<SceneObject> workbenchs = SceneObjectQuery.newQuery().name("Inventor's workbench").option("Manufacture").results();
        SceneObject workbench = workbenchs.first();
        if (workbench != null) {
            println("Workbench found. Interacting to manufacture.");
            if (workbench.interact("Manufacture")) {
                Execution.delayUntil(1800, () -> Interfaces.isOpen(1370));
                if(Interfaces.isOpen(1370)) {
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 29, 89849878);
                }
            }
        }
        
    }
    
}