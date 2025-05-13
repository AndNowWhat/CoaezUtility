package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.input.GameInput;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;

import java.util.Collections;
import java.util.List;

public class PortableSawmill extends Portable {

    private SawmillPlank selectedPlank;

    private static final int PLANK_SELECTION_INTERFACE_ID = 720;
    private static final int COST_CONFIRM_INTERFACE_ID = 1418;
    private static final int ENTER_AMOUNT_INTERFACE_ID = 1469;
    private static final int CONFIRMATION_INTERFACE_ID = 1188;

    private static final int BASE_PLANK_ACTION_ID = 47185937;
    private static final int MORE_OPTIONS_ACTION_ID = 47185958;
    private static final int PLANK_ACTION_ID_MULTIPLIER = 3;

    public PortableSawmill(CoaezUtility script) {
        super(script, PortableType.SAWMILL);
        this.setSelectedPlank(SawmillPlank.NORMAL_PLANKS);
    }

    public void setSelectedPlank(SawmillPlank plank) {
        this.selectedPlank = plank;
        ScriptConsole.println("[PortableSawmill] Selected plank set to: " + (plank != null ? plank.getDisplayName() : "null"));
    }

    public SawmillPlank getSelectedPlank() {
        return selectedPlank;
    }

    @Override
    public String getInteractionOption() {
        return "Make Planks";
    }

    @Override
    public List<Ingredient> getRequiredItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasRequiredItems() {
        return Backpack.getItems().stream().anyMatch(item -> {
            if (item == null) return false;
            ItemType type = item.getConfigType();
            if (type == null) return false;
            int category = type.getCategory();
            return category == 22 || category == 4255;
        });
    }

    private boolean selectPlankFromFirstPage() {
        int plankActionId = BASE_PLANK_ACTION_ID + (selectedPlank.getInterfaceOptionIndex() * PLANK_ACTION_ID_MULTIPLIER);
        ScriptConsole.println("[PortableSawmill] Selecting plank (Page 1): " + selectedPlank.getDisplayName() + " using Action ID: " + plankActionId);
        if (!MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, plankActionId)) {
            ScriptConsole.println("[PortableSawmill] Failed to select plank (Page 1): " + selectedPlank.getDisplayName() + " with Action ID " + plankActionId);
            return false;
        }
        return true;
    }

    private boolean selectPlankFromSecondPage() {
        ScriptConsole.println("[PortableSawmill] Target plank is on second page. Clicking 'More options' (Action ID: " + MORE_OPTIONS_ACTION_ID + ").");
        if (!MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, MORE_OPTIONS_ACTION_ID)) {
            ScriptConsole.println("[PortableSawmill] Failed to click 'More options'.");
            return false;
        }
        Execution.delay(script.getRandom().nextInt(300) + 700);

        int plankActionId = BASE_PLANK_ACTION_ID + (selectedPlank.getInterfaceOptionIndex() * PLANK_ACTION_ID_MULTIPLIER);
        ScriptConsole.println("[PortableSawmill] Selecting plank (Page 2): " + selectedPlank.getDisplayName() + " using Action ID: " + plankActionId);
        if (!MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, plankActionId)) {
            ScriptConsole.println("[PortableSawmill] Failed to select plank (Page 2): " + selectedPlank.getDisplayName() + " with Action ID " + plankActionId);
            return false;
        }
        return true;
    }

    @Override
    public boolean interact() {
        if (selectedPlank == null) {
            ScriptConsole.println("[PortableSawmill] No plank selected. Please set a plank first.");
            return false;
        }

        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name(this.type.getName())
                .option(getInteractionOption())
                .results();

        SceneObject sawmillObject = results.first();
        if (sawmillObject == null) {
            ScriptConsole.println("[PortableSawmill] Sawmill object not found.");
            Execution.delay(script.getRandom().nextInt(300) + 1200);
            return false;
        }

        if (!sawmillObject.interact(getInteractionOption())) {
            ScriptConsole.println("[PortableSawmill] Failed to interact with sawmill object.");
            return false;
        }
        
        if (!Execution.delayUntil(5000, () -> Interfaces.isOpen(PLANK_SELECTION_INTERFACE_ID))) {
            ScriptConsole.println("[PortableSawmill] Plank selection interface (" + PLANK_SELECTION_INTERFACE_ID + ") did not open.");
            return false;
        }
        ScriptConsole.println("[PortableSawmill] Plank selection interface (" + PLANK_SELECTION_INTERFACE_ID + ") open.");
        Execution.delay(script.getRandom().nextInt(200) + 300);

        boolean plankSelectedSuccessfully;
        if (selectedPlank.isSecondPage()) {
            plankSelectedSuccessfully = selectPlankFromSecondPage();
        } else {
            plankSelectedSuccessfully = selectPlankFromFirstPage();
        }

        if (!plankSelectedSuccessfully) {
            return false;
        }
        
        if (!Execution.delayUntil(5000, () -> Interfaces.isOpen(ENTER_AMOUNT_INTERFACE_ID))) {
            ScriptConsole.println("[PortableSawmill] Enter amount interface (" + ENTER_AMOUNT_INTERFACE_ID + ") did not open.");
            Execution.delay(script.getRandom().nextInt(200) + 300);
            return false;
        }
        Execution.delay(script.getRandom().nextInt(600) + 300);

        if(Interfaces.isOpen(ENTER_AMOUNT_INTERFACE_ID)) {
            ScriptConsole.println("[PortableSawmill] Enter amount interface (" + ENTER_AMOUNT_INTERFACE_ID + ") open. Sending keys '28'.");
            GameInput.setIntInput(28);
            Execution.delay(script.getRandom().nextInt(200) + 300);
        }

        Execution.delay(script.getRandom().nextInt(600) + 300);

        if (!Execution.delayUntil(5000, () -> Interfaces.isOpen(CONFIRMATION_INTERFACE_ID) && Dialog.isOpen())) {
            ScriptConsole.println("[PortableSawmill] Final confirmation interface (" + CONFIRMATION_INTERFACE_ID + ") or dialog did not open.");
            return false;
        }

        ScriptConsole.println("[PortableSawmill] Final confirmation interface (" + CONFIRMATION_INTERFACE_ID + ") open. Selecting 'Ok'.");
        if (!Dialog.interact(0)) {
            ScriptConsole.println("[PortableSawmill] Failed to select 'Ok' (Option 0) in final confirmation.");
            return false;
        }
        
        ScriptConsole.println("[PortableSawmill] Plank making initiated. Waiting for logs to be consumed (up to 10 seconds)...");
        boolean finished = Execution.delayUntil(10000, () -> !this.hasRequiredItems());
        
        if (finished && !this.hasRequiredItems()) {
            ScriptConsole.println("[PortableSawmill] Finished consuming logs.");
        } else if (Bank.isOpen()){
            ScriptConsole.println("[PortableSawmill] Bank opened, activity likely concluded or inventory is full. Loading preset.");
            script.setWaitingForPreset(true);
            script.setPresetLoaded(false);
            Bank.loadLastPreset();
            Execution.delayUntil(script.getRandom().nextInt(5000) + 5000L, () -> script.isPresetLoaded());
        } else if (!Interfaces.isOpen(CONFIRMATION_INTERFACE_ID)) {
             ScriptConsole.println("[PortableSawmill] Confirmation interface closed, crafting may have finished or been interrupted.");
        }
        else {
            ScriptConsole.println("[PortableSawmill] Timed out waiting for logs to be consumed, or crafting was interrupted.");
        }
        return true; 
    }
} 