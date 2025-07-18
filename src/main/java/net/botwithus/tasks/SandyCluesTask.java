package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.minimenu.actions.NPCAction;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class SandyCluesTask implements Task {
    private final CoaezUtility script;

    // Item IDs
    private static final int CLUE_ID = 43349;
    private static final int SCROLL_BOX_ID = 43351;

    private static final int SARAH_ID = 21153;
    private static final int LIFEGUARD_ID = 21158;
    private static final int PALMER_ID = 21152;
    private static final int FOREMAN_ID = 21163;
    private static final int FLO_ID = 21148;
    private static final int SHELDON_ID = 21147;
    private static final int WELLINGTON_ID = 21150;
    private static final int REYNA = 21146;

    private static final int DUNG_HOLE_ID = 114121;
    private static final int COCONUTS_ID = 97332;
    private static final int FISH_TABLE_ID = 97277;
    private static final int PALM_TREE_ID = 117512;

    private static final Coordinate SCROLL_GIVER_COORD = new Coordinate(3180, 3241, 0);
    private static final Coordinate DUNG_HOLE_COORD = new Coordinate(3170, 3252, 0);
    private static final Coordinate SARAH_COORD = new Coordinate(3169, 3220, 0);
    private static final Coordinate LIFEGUARD_COORD = new Coordinate(3170, 3252, 0);
    private static final Coordinate PALMER_COORD = new Coordinate(3154, 3227, 0);
    private static final Coordinate FOREMAN_COORD = new Coordinate(3158, 3227, 0);
    private static final Coordinate FLO_COORD = new Coordinate(3164, 3215, 0);
    private static final Coordinate SHELDON_COORD = new Coordinate(3170, 3252, 0);
    private static final Coordinate WELLINGTON_COORD = new Coordinate(3180, 3241, 0);
    private static final Coordinate COCONUTS_COORD = new Coordinate(3169, 3220, 0);
    private static final Coordinate FISH_TABLE_COORD = new Coordinate(3180, 3241, 0);
    private static final Coordinate PALM_TREE_COORD = new Coordinate(3186, 3240, 0);

    private static final String SARAH_TEXT = "She can be trusted, she isn't shy and";
    private static final String LIFEGUARD_TEXT = "say he sits around watching beach";
    private static final String PALMER_TEXT = "palm of his hand. Others think he's just";
    private static final String FOREMAN_TEXT = "an endless stream of important";
    private static final String FLO_TEXT = "share common ground with the";
    private static final String SHELDON_TEXT = "He's got one hat, two hat, three hat,";
    private static final String WELLINGTON_TEXT = "He's named after a boot, and carrying";
    private static final String HOLE_TEXT = "Investigate a large hole that leads...";
    private static final String COCONUTS_TEXT = "Somewhere a dwarf looks after a pile";
    private static final String FISH_TABLE_TEXT = "Something smells fishy behind a dwarf";
    private static final String PALM_TREE_TEXT = "Pick some coconuts that are oh so";

    public SandyCluesTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        ScriptConsole.println("[SandyCluesTask] Executing sandy clues task");

        if (Backpack.contains(SCROLL_BOX_ID)) {
            ScriptConsole.println("[SandyCluesTask] Found scroll box, opening it");
            if (Backpack.interact("Sandy Scroll box", "Open")) {
                Execution.delay(600);
            }
        } else if (Backpack.contains(CLUE_ID)) {
            ScriptConsole.println("[SandyCluesTask] Found clue, reading it");
            if (Backpack.interact("Sandy Clue Scroll", "Read")) {
                Execution.delayUntil(5000, this::isClueDialogOpen);
                Execution.delay(600);
                solveClue();
            }
        } else {
            ScriptConsole.println("[SandyCluesTask] No clues found, getting new scroll");
            takeScroll();
        }

        Execution.delay(script.getRandom().nextLong(600, 1200));
    }

    private boolean isClueDialogOpen() {
        return Interfaces.isOpen(345);
    }

    private boolean isNPCDialogOpen() {
        return Interfaces.isOpen(1184);
    }

    private boolean isScrollBoxDialogOpen() {
        return Interfaces.isOpen(1189);
    }

    private boolean isShopInteractDialogOpen() {
        return Interfaces.isOpen(1186);
    }

    private void takeScroll() {
        moveToLocation(SCROLL_GIVER_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(REYNA).results();
        if (!npcs.isEmpty()) {
            Npc scrollGiver = npcs.nearest();
            ScriptConsole.println("[SandyCluesTask] Found scroll giver NPC, attempting to interact");
            if (scrollGiver.interact("Request Sandy Clue Scroll")) {
                ScriptConsole.println("[SandyCluesTask] Interacted with scroll giver, waiting for dialog");
                if (Execution.delayUntil(10000, this::isNPCDialogOpen)) {
                    ScriptConsole.println("[SandyCluesTask] NPC dialog opened");
                    Execution.delay(1200);
                    if (!Backpack.contains(CLUE_ID)) {
                        ScriptConsole.println("[SandyCluesTask] No more clues available, stopping");
                        script.setBotState(CoaezUtility.BotState.STOPPED);
                    }
                }
            }
        }
    }

    private void solveClue() {
        String clueText = getClueText();
        if (clueText == null) {
            ScriptConsole.println("[SandyCluesTask] Could not read clue text");
            return;
        }

        ScriptConsole.println("[SandyCluesTask] Clue text: " + clueText);

        if (clueText.contains(FLO_TEXT)) {
            flo();
        } else if (clueText.contains(FOREMAN_TEXT)) {
            foreman();
        } else if (clueText.contains(LIFEGUARD_TEXT)) {
            lifeguard();
        } else if (clueText.contains(PALMER_TEXT)) {
            palmer();
        } else if (clueText.contains(SARAH_TEXT)) {
            sarah();
        } else if (clueText.contains(SHELDON_TEXT)) {
            sheldon();
        } else if (clueText.contains(WELLINGTON_TEXT)) {
            wellington();
        } else if (clueText.contains(HOLE_TEXT)) {
            dungHole();
        } else if (clueText.contains(COCONUTS_TEXT)) {
            coconuts();
        } else if (clueText.contains(FISH_TABLE_TEXT)) {
            fishTable();
        } else if (clueText.contains(PALM_TREE_TEXT)) {
            palmTree();
        } else {
            ScriptConsole.println("[SandyCluesTask] Unknown clue text, stopping");
            script.setBotState(CoaezUtility.BotState.STOPPED);
        }
    }

    private String getClueText() {
        Component clueComponent = ComponentQuery.newQuery(345).componentIndex(4).results().first();
        if (clueComponent != null) {
            return clueComponent.getText();
        }
        return null;
    }

    private void moveToLocation(Coordinate coord) {
        if (Client.getLocalPlayer() != null) {
            Coordinate currentPos = Client.getLocalPlayer().getCoordinate();
            double distance = currentPos.distanceTo(coord);
            ScriptConsole.println("[SandyCluesTask] Current position: " + currentPos + ", Target: " + coord + ", Distance: " + distance);

            if (distance > 15) {
                int randomX = coord.getX() + script.getRandom().nextInt(-1, 2);
                int randomY = coord.getY() + script.getRandom().nextInt(-1, 2);
                Coordinate randomCoord = new Coordinate(randomX, randomY, coord.getZ());

                ScriptConsole.println("[SandyCluesTask] Attempting to walk to randomized coordinate: " + randomCoord);

                if (!bresenhamWalkTo(randomCoord, true, 10)) {
                    ScriptConsole.println("[SandyCluesTask] Failed to initiate walking to " + randomCoord);
                } else {
                    ScriptConsole.println("[SandyCluesTask] Successfully initiated walking to " + randomCoord);
                    Execution.delay(600);
                }
            } else {
                ScriptConsole.println("[SandyCluesTask] Already close enough to target (distance: " + distance + ")");
            }
        } else {
            ScriptConsole.println("[SandyCluesTask] Player is null, cannot move");
        }
    }

    /**
     * Walks to a coordinate using Bresenham line algorithm for pathfinding
     * @param coordinate The destination coordinate
     * @param minimap Whether to use minimap for walking (always true)
     * @param stepSize Maximum step size for each movement
     * @return true if walking was initiated successfully
     */
    private boolean bresenhamWalkTo(Coordinate coordinate, boolean minimap, int stepSize) {
        if (Client.getLocalPlayer() == null) {
            ScriptConsole.println("[SandyCluesTask] Player is null");
            return false;
        }

        Coordinate currentCoordinate = Client.getLocalPlayer().getCoordinate();
        if (currentCoordinate == null) {
            ScriptConsole.println("[SandyCluesTask] Current coordinate is null");
            return false;
        }

        int dx = coordinate.getX() - currentCoordinate.getX();
        int dy = coordinate.getY() - currentCoordinate.getY();
        int distance = (int)Math.hypot(dx, dy);

        ScriptConsole.println("[SandyCluesTask] bresenhamWalkTo - dx: " + dx + ", dy: " + dy + ", distance: " + distance + ", stepSize: " + stepSize);

        if (distance > stepSize) {
            ScriptConsole.println("[SandyCluesTask] Distance exceeds step size, taking step");
            int stepX = currentCoordinate.getX() + dx * stepSize / distance;
            int stepY = currentCoordinate.getY() + dy * stepSize / distance;
            Movement.walkTo(stepX, stepY, true);
            return true;
        } else {
            ScriptConsole.println("[SandyCluesTask] Walking directly to destination: " + coordinate.getX() + ", " + coordinate.getY());
            Movement.walkTo(coordinate.getX(), coordinate.getY(), true);
            return true;
        }
    }

    /**
     * Basic movement implementation using Movement API
     */
    private boolean moveToCoordinate(int x, int y, boolean useMap) {
        try {
            // Add randomization by 2 in x and y axis
            int randomX = x + script.getRandom().nextInt(-2, 3);
            int randomY = y + script.getRandom().nextInt(-2, 3);

            ScriptConsole.println("[SandyCluesTask] Attempting to walk to " + randomX + ", " + randomY + " (randomized from " + x + ", " + y + ")");

            if (Math.abs(randomX) > 10000 || Math.abs(randomY) > 10000) {
                ScriptConsole.println("[SandyCluesTask] ERROR: Invalid coordinates: " + randomX + ", " + randomY);
                return false;
            }

            if (Client.getLocalPlayer() != null && Client.getLocalPlayer().getCoordinate() != null) {
                Coordinate currentPos = Client.getLocalPlayer().getCoordinate();
                int currentX = currentPos.getX();
                int currentY = currentPos.getY();
                double distance = Math.hypot(randomX - currentX, randomY - currentY);

                if (distance < 2) {
                    ScriptConsole.println("[SandyCluesTask] Already close to target location, skipping walk");
                    return true;
                }
            }

            ScriptConsole.println("[SandyCluesTask] Calling Movement.walkTo(" + randomX + ", " + randomY + ", " + useMap + ")");
            ScriptConsole.println("[SandyCluesTask] Movement.walkTo called successfully");
            return true;
        } catch (Exception e) {
            ScriptConsole.println("[SandyCluesTask] ERROR: Exception while walking to " + x + ", " + y + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private void sarah() {
        ScriptConsole.println("[SandyCluesTask] Solving Sarah clue");
        moveToLocation(SARAH_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(SARAH_ID).results();
        if (!npcs.isEmpty()) {
            Npc sarah = npcs.nearest();
            if (sarah.interact("Talk to")) {
                if (Execution.delayUntil(10000, this::isNPCDialogOpen)) {
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77594639);
                    Execution.delay(600);
                }
            }
        }
    }

    private void lifeguard() {
        ScriptConsole.println("[SandyCluesTask] Solving Lifeguard clue");
        moveToLocation(LIFEGUARD_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(LIFEGUARD_ID).results();
        if (!npcs.isEmpty()) {
            Npc lifeguard = npcs.nearest();
            if (lifeguard.interact("Talk to")) {
                if (Execution.delayUntil(10000, this::isNPCDialogOpen)) {
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77594639);
                    Execution.delay(600);
                }
            }
        }
    }

    private void palmer() {
        ScriptConsole.println("[SandyCluesTask] Solving Palmer clue");
        moveToLocation(PALMER_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(PALMER_ID).results();
        if (!npcs.isEmpty()) {
            Npc palmer = npcs.nearest();
            if (palmer.interact("Talk to")) {
                if (Execution.delayUntil(10000, this::isNPCDialogOpen)) {
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77594639);
                    Execution.delay(600);
                }
            }
        }
    }

    private void foreman() {
        ScriptConsole.println("[SandyCluesTask] Solving Foreman clue");
        moveToLocation(FOREMAN_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(FOREMAN_ID).results();
        if (!npcs.isEmpty()) {
            Npc foreman = npcs.nearest();
            if (foreman.interact("Talk to")) {
                if (Execution.delayUntil(10000, this::isNPCDialogOpen)) {
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77594639);
                    Execution.delay(600);
                }
            }
        }
    }

    private void flo() {
        ScriptConsole.println("[SandyCluesTask] Solving Flo clue");
        moveToLocation(FLO_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(FLO_ID).results();
        if (!npcs.isEmpty()) {
            Npc flo = npcs.nearest();
            ScriptConsole.println("[SandyCluesTask] Found Flo NPC, attempting to interact with Shop");
            if (flo.interact("Open Store")) {
                ScriptConsole.println("[SandyCluesTask] Interacted with Flo shop, waiting for shop dialog");
                if (Execution.delayUntil(20000, this::isShopInteractDialogOpen)) {
                    ScriptConsole.println("[SandyCluesTask] Shop dialog opened, selecting option");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77725704);
                    Execution.delay(600);
                } else {
                    ScriptConsole.println("[SandyCluesTask] Failed to open shop dialog with Flo");
                }
            } else {
                ScriptConsole.println("[SandyCluesTask] Failed to interact with Flo shop");
            }
        } else {
            ScriptConsole.println("[SandyCluesTask] Could not find Flo NPC");
        }
    }

    private void sheldon() {
        ScriptConsole.println("[SandyCluesTask] Solving Sheldon clue");
        moveToLocation(SHELDON_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(SHELDON_ID).results();
        if (!npcs.isEmpty()) {
            Npc sheldon = npcs.nearest();
            ScriptConsole.println("[SandyCluesTask] Found Sheldon NPC, attempting to interact with Shop");
            if (sheldon.interact("Open Store")) {
                ScriptConsole.println("[SandyCluesTask] Interacted with Sheldon shop, waiting for shop dialog");
                if (Execution.delayUntil(10000, this::isShopInteractDialogOpen)) {
                    ScriptConsole.println("[SandyCluesTask] Shop dialog opened, selecting option");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77725704);
                    Execution.delay(600);
                } else {
                    ScriptConsole.println("[SandyCluesTask] Failed to open shop dialog with Sheldon");
                }
            } else {
                ScriptConsole.println("[SandyCluesTask] Failed to interact with Sheldon shop");
            }
        } else {
            ScriptConsole.println("[SandyCluesTask] Could not find Sheldon NPC");
        }
    }

    private void wellington() {
        ScriptConsole.println("[SandyCluesTask] Solving Wellington clue");
        moveToLocation(WELLINGTON_COORD);

        EntityResultSet<Npc> npcs = NpcQuery.newQuery().byType(WELLINGTON_ID).results();
        if (!npcs.isEmpty()) {
            Npc wellington = npcs.nearest();
            if (wellington.interact("Talk to")) {
                if (Execution.delayUntil(10000, this::isNPCDialogOpen)) {
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77594639);
                    Execution.delay(600);
                }
            }
        }
    }

    private void dungHole() {
        ScriptConsole.println("[SandyCluesTask] Solving Dung Hole clue");
        moveToLocation(DUNG_HOLE_COORD);

        EntityResultSet<SceneObject> objects = SceneObjectQuery.newQuery().ids(DUNG_HOLE_ID).results();
        if (!objects.isEmpty()) {
            SceneObject hole = objects.nearest();
            if (hole.interact("Dungeoneer")) {
                if (Execution.delayUntil(5000, this::isScrollBoxDialogOpen)) {
                    Execution.delay(600);
                }
            }
        }
    }

    private void coconuts() {
        ScriptConsole.println("[SandyCluesTask] Solving Coconuts clue");
        EntityResultSet<SceneObject> objects = SceneObjectQuery.newQuery().ids(COCONUTS_ID).hidden(false).results();
        if (!objects.isEmpty()) {
            SceneObject coconuts = objects.nearest();
            if (coconuts.interact("Deposit coconuts")) {
                if (Execution.delayUntil(20000, this::isScrollBoxDialogOpen)) {
                    Execution.delay(600);
                }
            }
        }
    }

    private void fishTable() {
        ScriptConsole.println("[SandyCluesTask] Solving Fish Table clue");
        moveToLocation(FISH_TABLE_COORD);

        EntityResultSet<SceneObject> objects = SceneObjectQuery.newQuery().interactId(FISH_TABLE_ID).results();
        if (!objects.isEmpty()) {
            SceneObject table = objects.nearest();
            if (table.interact("Deposit fish")) {
                if (Execution.delayUntil(20000, this::isScrollBoxDialogOpen)) {
                    Execution.delay(600);
                }
            }
        }
    }

    private void palmTree() {
        ScriptConsole.println("[SandyCluesTask] Solving Palm Tree clue");
        moveToLocation(PALM_TREE_COORD);
        EntityResultSet<SceneObject> objects = SceneObjectQuery.newQuery().id(PALM_TREE_ID).results();
        if (!objects.isEmpty()) {
            SceneObject tree = objects.nearest();
            if (tree.interact("Pick coconut")) {
                if (Execution.delayUntil(10000, this::isScrollBoxDialogOpen)) {
                    Execution.delay(600);
                }
            }
        }
    }
}
