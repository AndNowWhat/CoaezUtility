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
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.ItemType.Stackability;

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

    public abstract String getInteractionOption();
    public abstract List<Ingredient> getRequiredItems(); 
    public int getCraftingInterfaceId() { return craftingInterfaceId; }
    public int getConfirmationInterfaceId() { return confirmationInterfaceId; }
    public int getMakeXInterfaceId() { return makeXInterfaceId; }
    public int getConfirmationDialogueAction() { return confirmationDialogueAction; }


    /**
     * Checks if the backpack contains the required items for this portable.
     * Handles stackable items correctly using ItemType.Stackability.
     */
    public boolean hasRequiredItems() {
        List<Ingredient> required = getRequiredItems();
        if (required == null || required.isEmpty()) { 

             ScriptConsole.println("[" + type.getName() + "] No required items defined or list is null.");
            return false; 
        }

        for (Ingredient req : required) {
            if (req == null) continue;

            int totalQuantity = Backpack.getItems().stream()
                .filter(item -> item != null && item.getName().equalsIgnoreCase(req.getDisplayName()))
                .mapToInt(item -> {
                    ItemType type = item.getConfigType(); 
                    if (type == null) return 0; 
                    
                    Stackability stackability = type.getStackability(); 
                    boolean isGenerallyStackable = stackability == Stackability.ALWAYS || stackability == Stackability.SOMETIMES;
                    boolean isCoin = item.getId() == 995;

                    return (isGenerallyStackable || isCoin) ? item.getStackSize() : 1;
                })
                .sum();
            
            if (totalQuantity < req.getAmount()) {
                ScriptConsole.println("[" + type.getName() + "] Missing required item: " + req.getDisplayName() + " (Need " + req.getAmount() + ", Have " + totalQuantity + ")");
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

    public void confirmAction() {
         ScriptConsole.println("[" + type.getName() + "] Confirmation interface open, confirming action");
         MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, getConfirmationDialogueAction());
         Execution.delay(script.getRandom().nextLong(1000, 2000));
    }
} 