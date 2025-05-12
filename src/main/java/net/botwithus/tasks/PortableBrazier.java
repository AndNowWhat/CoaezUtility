package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.js5.types.ItemType; // Use the corrected import
import net.botwithus.rs3.script.ScriptConsole;

import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;

import java.util.Collections;
import java.util.List;

/**
 * Concrete implementation for the Portable Brazier.
 * Overrides hasRequiredItems to check for specific item categories.
 */
public class PortableBrazier extends Portable {

    public PortableBrazier(CoaezUtility script) {
        super(script, PortableType.BRAZIER);
    }

    @Override
    public String getInteractionOption() {
        return "Add logs"; 
    }

    @Override
    public List<Ingredient> getRequiredItems() {
        return Collections.emptyList();
    }

    /**
     * Checks if the backpack contains any item of category 22 or 4255.
     * @return true if a required item is found, false otherwise.
     */
    @Override
    public boolean hasRequiredItems() {
        boolean found = Backpack.getItems().stream().anyMatch(item -> {
            if (item == null) return false;
            ItemType type = item.getConfigType();
            if (type == null) return false;
            int category = type.getCategory();
            return category == 22 || category == 4255;
        });

        return found;
    }

    /**
     * Overrides the default interact method to add a custom delay 
     * waiting for items to be consumed, instead of waiting for an interface.
     * @return true if interaction was initiated, false otherwise.
     */
    @Override
    public boolean interact() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
            .name(this.type.getName())
            .option(getInteractionOption()) 
            .results();
        
        SceneObject portableObject = results.first();
        if (portableObject != null) {
            ScriptConsole.println("[" + this.type.getName() + "] Found portable object, interacting with '" + getInteractionOption() + "'");
            if (portableObject.interact(getInteractionOption())) {
                ScriptConsole.println("[" + this.type.getName() + "] Interaction successful. Waiting for items to be consumed...");
                boolean finished = Execution.delayUntil(120000, () -> !this.hasRequiredItems());
                if (finished) {
                    ScriptConsole.println("[" + this.type.getName() + "] Finished consuming items.");
                } else {
                    ScriptConsole.println("[" + this.type.getName() + "] Timed out waiting for items to be consumed.");
                }
                return true; 
            } else {
                 ScriptConsole.println("[" + this.type.getName() + "] Interaction failed with option '" + getInteractionOption() + "'.");
                 return false; 
            }
        } else {
            ScriptConsole.println("[" + this.type.getName() + "] Portable object with option '" + getInteractionOption() + "' not found.");
            Execution.delay(1500);
            return false; 
        }
    }
} 