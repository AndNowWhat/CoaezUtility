package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class FlaxPickerTask implements Task {

    private final CoaezUtility script;

    // Areas and coordinates
    private final Area flaxArea = new Area.Rectangular(new Coordinate(2882, 3474, 0), new Coordinate(2889, 3470, 0));
    private final Coordinate spinningWheelCoord = new Coordinate(2888, 3495, 0);
    private final Area spinningWheelArea = new Area.Circular(spinningWheelCoord, 5);
    private final Coordinate burthorpeBankCoord = new Coordinate(2888, 3536, 0);
    private final Area burthorpeBankArea = new Area.Circular(burthorpeBankCoord, 5);

    // Interface IDs
    private static final int CRAFTING_INTERFACE_ID = 1251;
    private static final int CONFIRMATION_INTERFACE_ID = 1370;
    private static final int STRING_INTERFACE_ID = 1473;
    private static final int CONFIRMATION_DIALOGUE_ACTION = 89784350;

    // Item names
    private static final String FLAX = "Flax";
    private static final String BOWSTRING = "Bowstring";

    private TaskState currentState = TaskState.PICK_FLAX;

    private enum TaskState {
        PICK_FLAX,
        GO_TO_SPINNING_WHEEL,
        SPIN_FLAX,
        STRING_BOWSTRING,
        GO_TO_BANK,
        BANK_ITEMS
    }

    public FlaxPickerTask(CoaezUtility script) {
        this.script = script;
    }

    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return;
        }

        ScriptConsole.println("[FlaxPicker] Current state: " + currentState);

        switch (currentState) {
            case PICK_FLAX -> handlePickFlax(player);
            case GO_TO_SPINNING_WHEEL -> handleGoToSpinningWheel(player);
            case SPIN_FLAX -> handleSpinFlax(player);
            case STRING_BOWSTRING -> handleStringBowstring(player);
            case GO_TO_BANK -> handleGoToBank(player);
            case BANK_ITEMS -> handleBanking(player);
        }
    }

    private void handlePickFlax(LocalPlayer player) {
        if (Backpack.isFull()) {
            ScriptConsole.println("[FlaxPicker] Backpack full, moving to spinning wheel");
            currentState = TaskState.GO_TO_SPINNING_WHEEL;
            return;
        }

        if (!flaxArea.contains(player.getCoordinate())) {
            ScriptConsole.println("[FlaxPicker] Moving to flax area");
            NavPath path = NavPath.resolve(flaxArea.getRandomWalkableCoordinate());
            Movement.traverse(path);
            Execution.delay(script.getRandom().nextInt(600, 1200));
            return;
        }

        EntityResultSet<SceneObject> flaxResults = SceneObjectQuery.newQuery()
                .name(FLAX)
                .hidden(false)
                .option("Pick")
                .results();
        SceneObject nearestFlax = flaxResults.nearestTo(player);

        if (nearestFlax != null) {
            nearestFlax.interact("Pick");
            Execution.delay(script.getRandom().nextInt(100, 400));
        } else {
            ScriptConsole.println("[FlaxPicker] No flax found nearby");
            Execution.delay(script.getRandom().nextInt(600, 1000));
        }
    }

    private void handleGoToSpinningWheel(LocalPlayer player) {
        if (!spinningWheelArea.contains(player.getCoordinate())) {
            ScriptConsole.println("[FlaxPicker] Walking to spinning wheel");
            NavPath path = NavPath.resolve(spinningWheelCoord);
            Movement.traverse(path);
            Execution.delay(script.getRandom().nextInt(600, 1200));
        } else {
            ScriptConsole.println("[FlaxPicker] Arrived at spinning wheel");
            currentState = TaskState.SPIN_FLAX;
        }
    }

    private void handleSpinFlax(LocalPlayer player) {
        if (!Backpack.contains(FLAX)) {
            if (Backpack.contains(BOWSTRING)) {
                ScriptConsole.println("[FlaxPicker] Flax spun, checking for stringing");
                currentState = TaskState.STRING_BOWSTRING;
            } else {
                ScriptConsole.println("[FlaxPicker] No flax to spin, going to bank");
                currentState = TaskState.GO_TO_BANK;
            }
            return;
        }

        if (Interfaces.isOpen(CRAFTING_INTERFACE_ID)) {
            ScriptConsole.println("[FlaxPicker] Spinning in progress");
            Execution.delayUntil(60000, () -> !Interfaces.isOpen(CRAFTING_INTERFACE_ID));
            return;
        }

        if (Interfaces.isOpen(CONFIRMATION_INTERFACE_ID)) {
            ScriptConsole.println("[FlaxPicker] Confirming spinning");
            boolean interacted = MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, CONFIRMATION_DIALOGUE_ACTION);
            if (interacted) {
                Execution.delayUntil(3000, () -> Interfaces.isOpen(CRAFTING_INTERFACE_ID) || !Interfaces.isOpen(CONFIRMATION_INTERFACE_ID));
            }
            return;
        }

        EntityResultSet<SceneObject> wheelResults = SceneObjectQuery.newQuery()
                .name("Spinning wheel")
                .option("Spin")
                .hidden(false)
                .results();
        SceneObject spinningWheel = wheelResults.nearest();

        if (spinningWheel != null) {
            ScriptConsole.println("[FlaxPicker] Interacting with spinning wheel");
            if (spinningWheel.interact("Spin")) {
                Execution.delayUntil(5000, () -> Interfaces.isOpen(CONFIRMATION_INTERFACE_ID) || Interfaces.isOpen(CRAFTING_INTERFACE_ID));
            }
        } else {
            ScriptConsole.println("[FlaxPicker] Spinning wheel not found");
            Execution.delay(1500);
        }
    }

    private void handleStringBowstring(LocalPlayer player) {
        if (Backpack.isFull()) {
            var results = ComponentQuery.newQuery(STRING_INTERFACE_ID)
                    .componentIndex(5)
                    .itemName(BOWSTRING)
                    .option("String")
                    .results();

            if (!results.isEmpty()) {
                ScriptConsole.println("[FlaxPicker] Backpack full of bowstrings, going to bank");
                currentState = TaskState.GO_TO_BANK;
            } else {
                ScriptConsole.println("[FlaxPicker] Backpack full but not all bowstrings, going to bank");
                currentState = TaskState.GO_TO_BANK;
            }
            return;
        }

        if (!Backpack.contains(BOWSTRING)) {
            ScriptConsole.println("[FlaxPicker] No more bowstrings, going to bank");
            currentState = TaskState.GO_TO_BANK;
            return;
        }

        ScriptConsole.println("[FlaxPicker] Bowstrings ready, going to bank");
        currentState = TaskState.GO_TO_BANK;
    }

    private void handleGoToBank(LocalPlayer player) {
        if (!burthorpeBankArea.contains(player.getCoordinate())) {
            ScriptConsole.println("[FlaxPicker] Walking to Burthorpe bank");
            NavPath path = NavPath.resolve(burthorpeBankCoord);
            Movement.traverse(path);
            Execution.delay(script.getRandom().nextInt(600, 1200));
        } else {
            ScriptConsole.println("[FlaxPicker] Arrived at bank");
            currentState = TaskState.BANK_ITEMS;
        }
    }

    private void handleBanking(LocalPlayer player) {
        ScriptConsole.println("[FlaxPicker] Loading last preset");
        Bank.loadLastPreset();

        boolean emptied = Execution.delayUntil(8000, () -> !Backpack.isFull() || Backpack.isEmpty());

        if (emptied) {
            ScriptConsole.println("[FlaxPicker] Bank preset loaded, returning to flax picking");
            currentState = TaskState.PICK_FLAX;
        } else {
            ScriptConsole.println("[FlaxPicker] Waiting for banking to complete");
            Execution.delay(script.getRandom().nextInt(600, 1200));
        }
    }
}
