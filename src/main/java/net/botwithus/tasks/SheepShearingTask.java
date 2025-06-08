package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.api.game.world.Traverse;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class SheepShearingTask implements Task {

    private final CoaezUtility script;
    private final Area sheepArea = new Area.Rectangular(new Coordinate(2897, 3491, 0), new Coordinate(2903, 3486, 0));
    private final Coordinate bankCoordinate = new Coordinate(2888, 3536, 0);
    private final Area bankArea = new Area.Circular(bankCoordinate, 5);
    public SheepShearingTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return;
        }

        if (Backpack.isFull()) {
            handleBanking(player);
        } else {
            handleShearing(player);
        }
    }

    private void handleBanking(LocalPlayer player) {
        if (!bankArea.contains(player.getCoordinate())) {
            NavPath path = NavPath.resolve(bankArea.getRandomWalkableCoordinate());
            Movement.traverse(path);
        } else {
            Bank.loadLastPreset();
            Execution.delayUntil(8000, () -> !Backpack.isFull());
        }
    }

    private void handleShearing(LocalPlayer player) {
        if (!sheepArea.contains(player.getCoordinate())) {
            NavPath path = NavPath.resolve(sheepArea.getRandomWalkableCoordinate());
            Movement.traverse(path);
            return;
        }

        if (player.getAnimationId() != -1) {
            Execution.delay(script.getRandom().nextInt(300, 600));
            return;
        }

        EntityResultSet<Npc> sheepResults = NpcQuery.newQuery().name("Sheep").option("Shear").results();
        Npc nearestSheep = sheepResults.nearestTo(player);

        if (nearestSheep != null) {
            nearestSheep.interact("Shear");
            boolean startedAnimating = Execution.delayUntil(5000, () -> player.getAnimationId() != -1);
            if (startedAnimating) {
                Execution.delayUntil(5000, () -> player.getAnimationId() == -1);
            }
        } else {
            ScriptConsole.println("No sheep to shear found in the area.");
            Execution.delay(script.getRandom().nextInt(1000, 3000));
        }
    }
} 