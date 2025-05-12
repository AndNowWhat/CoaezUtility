package net.botwithus.tasks;

import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Ingredient {
    // Stores potential ItemTypes. Usually one, but supports optional ingredients.
    private final List<ItemType> itemTypeList;
    private final int amount;

    // Constructor for a single required item type
    public Ingredient(ItemType itemType, int amount) {
        this.itemTypeList = new ArrayList<>();
        this.itemTypeList.add(itemType);
        this.amount = amount;
    }

    // Constructor for multiple (optional) item types
    public Ingredient(List<ItemType> itemTypes, int amount) {
        this.itemTypeList = new ArrayList<>(itemTypes);
        this.amount = amount;
    }

    public List<ItemType> getItemTypes() {
        return itemTypeList;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isOptional() {
        return itemTypeList.size() > 1;
    }

    // Gets the representative name (first item's name or joined list for optionals)
    public String getDisplayName() {
        if (itemTypeList.isEmpty()) {
            return "Unknown Ingredient";
        }
        if (itemTypeList.size() == 1 || itemTypeList.stream().allMatch(it -> it.getName().equals(itemTypeList.get(0).getName()))) {
            return itemTypeList.get(0).getName() != null ? itemTypeList.get(0).getName() : "Unknown";
        } else {
            return itemTypeList.stream()
                               .map(it -> it.getName() != null ? it.getName() : "Unknown")
                               .collect(Collectors.joining(" / "));
        }
    }

    // Helper to check if a given backpack item matches this ingredient (any of the types if optional)
    public boolean matches(ItemType backpackItemType) {
         if (backpackItemType == null) return false;
         return itemTypeList.stream().anyMatch(reqType -> reqType.getId() == backpackItemType.getId());
    }
} 