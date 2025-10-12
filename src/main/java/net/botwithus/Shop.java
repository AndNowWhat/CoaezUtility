package net.botwithus;

import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Shop {

    public static boolean isOpen() {
        return Interfaces.isOpen(1265);
    }

    public static ArrayList<Component> getItems(Predicate<Component> filter) {
        ArrayList<Component> items = new ArrayList<>();
        // Shop items are at component index 20 with subcomponents 0-12
        for (int i = 0; i <= 12; i++) {
            Component component = ComponentQuery.newQuery(1265)
                    .componentIndex(20)
                    .subComponentIndex(i)
                    .results()
                    .first();
            if (component != null) {
                items.add(component);
            }
        }
        return items.stream()
                .filter(filter)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<Component> getItems(Pattern pattern) {
        return new ArrayList<>(getItems(nameMatcher(pattern)));
    }

    public static ArrayList<Component> getItems() {
        return new ArrayList<>(getItems(component -> true));
    }

    public static boolean contains(Predicate<Component> filter) {
        return !getItems(filter).isEmpty();
    }

    public static boolean contains(Pattern pattern) {
        return !getItems(nameMatcher(pattern)).isEmpty();
    }

    public static int getAmount(Predicate<Component> filter) {
        return getItems(filter).stream()
                .mapToInt(component -> component.getItemAmount() > 0 ? component.getItemAmount() : 0)
                .sum();
    }

    public static int getAmount(Pattern pattern) {
        return getAmount(nameMatcher(pattern));
    }

    public static int getAmount() {
        return getAmount(component -> true);
    }

    public static boolean buyAll() {
        ArrayList<Component> items = new ArrayList<>();
        for (int i = 0; i <= 12; i++) {
            Component component = ComponentQuery.newQuery(1265)
                    .componentIndex(20)
                    .subComponentIndex(i)
                    .results()
                    .first();
            if (component != null) {
                items.add(component);
            }
        }
        
        if (items.isEmpty()) {
            return false;
        }

        boolean success = false;
        for (int i = 0; i < items.size(); i++) {
            if (Backpack.isFull()) {
                break;
            }
            Component component = items.get(i);
            if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), 7, i, 82903060)) {
                success = true;
            }
        }
        return success;
    }

    public static boolean close() {
        if (!isOpen()) {
            return false;
        }

        Component component = ComponentQuery.newQuery(1265)
                .componentIndex(20)
                .subComponentIndex(1)
                .results()
                .first();

        return component != null && component.interact(1);
    }

    private static Predicate<Component> nameMatcher(Pattern pattern) {
        return component -> {
            var type = ConfigManager.getItemType(component.getItemId());
            return type != null && type.getName().matches(pattern.pattern());
        };
    }

    private static boolean isFoodItem(String itemName) {
        return itemName.contains("cake") || 
               itemName.contains("pie") || 
               itemName.contains("stew") || 
               itemName.contains("soup") || 
               itemName.contains("bread") || 
               itemName.contains("batta") || 
               itemName.contains("crunchie") || 
               itemName.contains("worm") || 
               itemName.contains("fruit") || 
               itemName.contains("vegetable") || 
               itemName.contains("gnomebowl") || 
               itemName.contains("cocktail") ||
               itemName.contains("kebab");
    }
}