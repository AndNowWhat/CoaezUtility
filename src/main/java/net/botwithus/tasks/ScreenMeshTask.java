package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;

public class ScreenMeshTask implements Task {
    private final CoaezUtility script;
    private final SiftSoilTask siftSoilHelper;
    
    public ScreenMeshTask(CoaezUtility script) {
        this.script = script;
        this.siftSoilHelper = new SiftSoilTask(script);
    }
    
    @Override
    public void execute() {
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (siftSoilHelper.backpackContainsSoil()) {
            SceneObject mesh = SceneObjectQuery.newQuery().name("Mesh").option("Screen").results().nearest();
            
            if (mesh != null && mesh.interact("Screen")) {
                Execution.delayUntil(15000, () -> Interfaces.isOpen(1370));
                
                Component screenOption = ComponentQuery.newQuery(1370)
                    .componentIndex(30)  
                    .results()
                    .first();
                
                if (screenOption != null && screenOption.interact(1)) {
                    Execution.delay(script.getRandom().nextLong(1000, 2000));
                } else {
                    System.out.println("Could not find screening interface option");
                }
            } else {
                System.out.println("Could not find mesh object");
            }
        } else {
            script.setWaitingForPreset(true);
            script.setPresetLoaded(false);
            Bank.loadLastPreset();
            Execution.delayUntil(script.getRandom().nextInt(5000) + 5000, () -> script.isPresetLoaded());
        }
    }
} 