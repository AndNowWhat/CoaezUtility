package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.sorceressgarden.models.NPCDirection;

public class NPCLoggerTask implements Task {
    private final CoaezUtility script;
    private final int npcId = 5532;
    private static final Area WINTER_GARDEN_AREA = new Area.Rectangular(new Coordinate(2886, 5487, 0), new Coordinate(2903, 5464, 0));

    public NPCLoggerTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        ScriptConsole.println("[NPC Logger] Looking for Guardian " + npcId);

        Npc npc = NpcQuery.newQuery().byType(npcId).results().first();
        if( npc != null) {
            float direction1 = npc.getDirection1(); 
            float direction2 = npc.getDirection2();
            NPCDirection.Direction facing = NPCDirection.Direction.getFacingDirection(direction1, direction2);
            ScriptConsole.println("[NPC Logger] Found Guardian " + npcId + " at " + npc.getCoordinate() +
                " | direction1: " + direction1 + " | direction2: " + direction2 +
                " | angle(deg): " + Math.toDegrees(direction1) +
                " | facing: " + (facing != null ? facing.name() : "UNKNOWN"));
        } else {
        }
        Execution.delay(1000);
    }
} 