package net.botwithus.tasks;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.script.Execution;

import java.util.function.Predicate;

import net.botwithus.rs3.game.js5.types.ItemType;

public class PortableBrazierTask {

    private final String BRAZIER_NAME = PortableType.BRAZIER.getName();

    private Predicate<Item> isRequiredItem() {
        return item -> {
            if (item == null) return false;
            ItemType itemType = item.getConfigType();
            if (itemType == null) return false;
            int category = itemType.getCategory();
            return category == 4255 || category == 22;
        };
    }

    public void execute() {
        if (Backpack.getItems().stream().anyMatch(isRequiredItem())) {
            SceneObject brazier = SceneObjectQuery.newQuery().name(BRAZIER_NAME).results().nearest();

            if (brazier != null) {
                System.out.println("Found brazier: " + brazier.getName() + " at " + brazier.getCoordinate());
                if (brazier.interact(0)) {
                    System.out.println("Interacted with brazier (action 0).");
                    Execution.delayUntil(60000, () -> !Backpack.getItems().stream().anyMatch(isRequiredItem()));
                     if (!Backpack.getItems().stream().anyMatch(isRequiredItem())) {
                        System.out.println("Finished using items on brazier.");
                    } else {
                        System.out.println("Timed out waiting for items to be used.");
                    }
                } else {
                    System.out.println("Failed to interact with brazier (action 0).");
                }
            } else {
                System.out.println("Portable brazier not found nearby.");
            }
        } else {
             System.out.println("No required items (category 4255 or 22) found in backpack.");
             Execution.delay(1000);
        }
    }
} 