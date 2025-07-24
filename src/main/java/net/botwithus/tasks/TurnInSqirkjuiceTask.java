package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class TurnInSqirkjuiceTask implements Task {
    private final CoaezUtility script;
    private static final String[] JUICE_NAMES = {
        "Winter sq'irkjuice",
        "Spring sq'irkjuice",
        "Autumn sq'irkjuice",
        "Summer sq'irkjuice"
    };

    private static final Coordinate OSMAN_COORDINATE = new Coordinate(3290, 3158, 0);
    private static final int DIST_THRESHOLD = 5;
    private static final Coordinate DOOR_COORDINATE = new Coordinate(3293, 3165, 0);
    private static final Area INSIDE_AREA = new Area.Rectangular(new Coordinate(3282, 3163, 0), new Coordinate(3303, 3150, 0));

    public TurnInSqirkjuiceTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        boolean found = false;
        for (String juice : JUICE_NAMES) {
            if (Backpack.contains(juice)) {
                ScriptConsole.println("Detected in backpack: " + juice);
                found = true;
                break;
            }
        }
        if (!found) {
            if (!found && INSIDE_AREA.contains(Client.getLocalPlayer().getCoordinate())) {
                ScriptConsole.println("Leaving Osman area to bank...");
                SceneObject door = SceneObjectQuery.newQuery().id(76499).hidden(false).results().nearest();
                if (door != null) {
                    door.interact("Pass-through");
                    Execution.delayUntil(5000, () -> !INSIDE_AREA.contains(Client.getLocalPlayer().getCoordinate()));
                }
            }
            ScriptConsole.println("No sq'irkjuice found in backpack, loading preset...");
            Bank.loadLastPreset();
            Execution.delayUntil(5000, () -> {
                for (String juice : JUICE_NAMES) if (Backpack.contains(juice)) return true;
                return false;
            });
           
            return;
        }

        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) return;
        Coordinate playerLoc = player.getCoordinate();
        if (!INSIDE_AREA.contains(playerLoc)) {
            // Not inside area, walk to door
            ScriptConsole.println("Walking to Osman doors at " + DOOR_COORDINATE);
            NavPath path = NavPath.resolve(DOOR_COORDINATE);
            if(path != null) {
                Movement.traverse(path);
            }
            // If at the door, log that we should open it (actual door opening logic can be added if needed)
            if (playerLoc.equals(DOOR_COORDINATE)) {
                ScriptConsole.println("At Osman doors, open the door if needed.");
                SceneObject door = SceneObjectQuery.newQuery().id(76499).hidden(false).results().nearest();
                if (door != null) {
                    door.interact("Pass-through");
                    Execution.delayUntil(5000, () -> INSIDE_AREA.contains(Client.getLocalPlayer().getCoordinate()));
                }
            }
            Execution.delay(script.getRandom().nextLong(800, 1200));
            return;
        }

        // Inside area, proceed as before
        for (String juice : JUICE_NAMES) {
            ResultSet<Component> juiceResults = ComponentQuery.newQuery(1473).componentIndex(5).itemName(juice).option("Drink").results();
            Component juiceComp = juiceResults.first();
            if (juiceComp != null) {
                ScriptConsole.println("Selecting juice component: " + juice);
                MiniMenu.interact(SelectableAction.SELECTABLE_COMPONENT.getType(), 0, juiceComp.getComponentIndex(),
                        (juiceComp.getInterfaceIndex() << 16) | juiceComp.getComponentIndex());
                Execution.delay(script.getRandom().nextLong(200, 400));
                break;
            }
        }

        Npc osman = NpcQuery.newQuery().name("Osman").option("Talk-to").results().nearest();
        if (osman != null) {
            ScriptConsole.println("Interacting with Osman (id=" + osman.getId() + ")");
            MiniMenu.interact(SelectableAction.SELECT_NPC.getType(), osman.getId(), 0, 0);
            Execution.delayUntil(5000, () -> !Backpack.isFull());
        } else {
            ScriptConsole.println("Could not find Osman nearby!");
        }
    }
}