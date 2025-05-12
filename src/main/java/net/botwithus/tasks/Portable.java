package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;

import java.util.List;
import java.util.ArrayList;

public abstract class Portable {
    protected final CoaezUtility script;
    protected final PortableType type;

    // Common Interface IDs (can be overridden by subclasses if needed)
    protected int craftingInterfaceId = 1251;
    protected int confirmationInterfaceId = 1370; // General crafting confirm
    protected int makeXInterfaceId = 1371; // Make-X interface
    protected int confirmationDialogueAction = 89784350; // General confirm button action

    public Portable(CoaezUtility script, PortableType type) {
        this.script = script;
        this.type = type;
    }

    public PortableType getType() {
        return type;
    }

    // Abstract methods to be implemented by subclasses
    public abstract String getInteractionOption();
    public abstract List<Ingredient> getRequiredItems(); // Reads from structs/configs
    // Optional: Subclasses can override if they use different interfaces
    public int getCraftingInterfaceId() { return craftingInterfaceId; }
    public int getConfirmationInterfaceId() { return confirmationInterfaceId; }
    public int getMakeXInterfaceId() { return makeXInterfaceId; }
    public int getConfirmationDialogueAction() { return confirmationDialogueAction; }


    /**
     * Checks if the backpack contains the required items for this portable.
     * Handles non-stackable items by counting.
     */
    public boolean hasRequiredItems() {
        List<Ingredient> required = getRequiredItems();
        if (required.isEmpty()) {
            ScriptConsole.println("[" + type.getName() + "] No required items defined yet.");
            return false; // Or true? Depends on desired behavior if requirements aren't loaded
        }

        for (Ingredient req : required) {
            // TODO: Adapt this based on how Ingredient stores item info (ID vs Name) and stackability
            long count = Backpack.getItems().stream()
                .filter(item -> item.getName().equalsIgnoreCase(req.getDisplayName())) // Case-insensitive match
                // .mapToInt(item -> item.getStackability() == STACKABLE ? item.getStackSize() : 1) // Example if stackability is known
                .count(); // Simple count for non-stackable for now
            
            if (count < req.getAmount()) {
                ScriptConsole.println("[" + type.getName() + "] Missing required item: " + req.getDisplayName() + " (Need " + req.getAmount() + ", Have " + count + ")");
                return false;
            }
        }
        ScriptConsole.println("[" + type.getName() + "] All required items present.");
        return true;
    }

    /**
     * Finds and interacts with the portable scene object.
     * @return true if interaction was successful, false otherwise.
     */
    public boolean interact() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
            .name(type.getName())
            .option(getInteractionOption())
            .results();
        
        SceneObject portableObject = results.first();
        if (portableObject != null) {
            ScriptConsole.println("[" + type.getName() + "] Found portable object, interacting with '" + getInteractionOption() + "'");
            if (portableObject.interact(getInteractionOption())) {
                // Wait for the confirmation or make-x interface to appear
                Execution.delayUntil(5000L, () ->  // Ensure delay is long
                    Interfaces.isOpen(getConfirmationInterfaceId()) || Interfaces.isOpen(getMakeXInterfaceId())
                );
                return true;
            }
        } else {
            ScriptConsole.println("[" + type.getName() + "] Portable object not found.");
            Execution.delay(1500);
        }
        return false;
    }

    // Default confirmation handling (can be overridden)
    public void confirmAction() {
         ScriptConsole.println("[" + type.getName() + "] Confirmation interface open, confirming action");
         MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, getConfirmationDialogueAction());
         Execution.delay(script.getRandom().nextLong(1000, 2000));
    }
} 