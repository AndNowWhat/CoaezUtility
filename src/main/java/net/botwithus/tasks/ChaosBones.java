package net.botwithus.tasks;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;

public class ChaosBones implements Task {
    @Override
    public void execute() {
        if(Backpack.isEmpty()){
            Npc results = NpcQuery.newQuery().name("Simon").option("Talk to").results().nearest();
            if(results != null){
                results.interact("Load Last Preset from");
                Execution.delayUntil(15000, () -> !Backpack.isEmpty());
            }

        } else {
            SceneObject results = SceneObjectQuery.newQuery().name("Chaos altar").option("Pray at").results().nearest();
            if(results != null){
                results.interact("Offer");
                Execution.delayUntil(90000, Backpack::isEmpty);
            }

        }
    }
}
