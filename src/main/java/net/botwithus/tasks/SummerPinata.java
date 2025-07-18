package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.PathingEntity;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.List;
import java.util.regex.Pattern;

public class SummerPinata implements Task {
    private final CoaezUtility script;

    public SummerPinata(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[SummerPinataTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }

        if(!Backpack.contains(53329)){
            ScriptConsole.println("[SummerPinataTask] Summer piñata not found in backpack, stopping...");
            script.setActive(false);
            return;
        }


        PathingEntity<?> currentTarget = player.getTarget();
        if (currentTarget != null) {
            ScriptConsole.println("[SummerPinataTask] Player already has target: " + currentTarget.getName());
            Execution.delay(1000);
            return;
        }

        EntityResultSet<Npc> results = NpcQuery.newQuery()
            .byType(29225)
            .option("Attack")
            .results();
        if (!results.isEmpty()) {
            Npc targetNpc = results.first();
            ScriptConsole.println("[SummerPinataTask] Found NPC to attack: " + targetNpc.getName());

            if (targetNpc.interact("Attack")) {
                ScriptConsole.println("[SummerPinataTask] Successfully attacked NPC");
                Execution.delay(script.getRandom().nextLong(1500, 3000));
            } else {
                ScriptConsole.println("[SummerPinataTask] Failed to attack NPC");
                Execution.delay(1000);
            }
        } else {
            if(Backpack.contains(53329)){
                    if(MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 96534533)){
                        ScriptConsole.println("[SummerPinataTask] Successfully deployed Summer piñata");
                        Execution.delayUntil(1200, () -> {
                            EntityResultSet<Npc> deployedResults = NpcQuery.newQuery()
                                    .name("Summer piñata")
                                    .results();
                            return !deployedResults.isEmpty();
                        });
                    } else {
                        ScriptConsole.println("[SummerPinataTask] Failed to deploy Summer piñata");
                    }

                }

            }

        }
    }
