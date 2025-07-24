package net.botwithus.tasks;

import java.util.ArrayList;
import java.util.List;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class NPCLoggerTask implements Task {
    private final CoaezUtility script;
    private final int npcId = 5539;

    public NPCLoggerTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[NPC Logger] Local player not found.");
            Execution.delay(1000);
            return;
        }
        Coordinate playerPos = player.getCoordinate();
        if (playerPos == null) {
            ScriptConsole.println("[NPC Logger] Local player position not found.");
            Execution.delay(1000);
            return;
        }
        ScriptConsole.println("[NPC Logger] Scanning for NPCs within 30 tiles of player at " + playerPos);

        List<Npc> npcs = new ArrayList<>();
        for (Npc npc : NpcQuery.newQuery().results()) {
            npcs.add(npc);
        }
        for (Npc npc : npcs) {
            Coordinate npcPos = npc.getCoordinate();
            if (npcPos != null && playerPos.distanceTo(npcPos) <= 30) {
                ScriptConsole.println("[NPC Logger] NPC id: " + npc.getId() +
                    " | name: " + npc.getName() +
                    " | pos: " + npcPos +
                    " | direction1: " + npc.getDirection1() +
                    " | direction2: " + npc.getDirection2());
            }
        }
        Execution.delay(1000);
    }
} 