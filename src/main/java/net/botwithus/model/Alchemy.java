package net.botwithus.model;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.cs2.ScriptBuilder;
import net.botwithus.rs3.game.cs2.layouts.Layout;
import net.botwithus.rs3.game.cs2.ReturnValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Alchemy {
    private final List<Pattern> alchemyPatterns = new ArrayList<>();
    private boolean alchemyEnabled = true;
    private CoaezUtility script;

    private static final ScriptBuilder cancelSelectionScript = ScriptBuilder.of(9666).returns(Layout.INT);

    public Alchemy(CoaezUtility script) {
        this.script = script;
    }

    public void setAlchemyEnabled(boolean enabled) {
        this.alchemyEnabled = enabled;
    }

    public boolean isAlchemyEnabled() {
        return alchemyEnabled;
    }

    public void addAlchemyItem(String pattern) {
        String processedPattern = pattern.toLowerCase()
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace(" ", "\\s+");
            
        Pattern itemPattern = Pattern.compile(processedPattern, Pattern.CASE_INSENSITIVE);
        if (!alchemyPatterns.contains(itemPattern)) {
            alchemyPatterns.add(itemPattern);
        }
    }

    public void removeAlchemyItem(String pattern) {
        alchemyPatterns.removeIf(p -> p.pattern().equals(pattern));
    }

    public List<String> getAlchemyItems() {
        return alchemyPatterns.stream()
                .map(Pattern::pattern)
                .collect(Collectors.toList());
    }

    private List<Item> fetchItemsToAlch() {
        List<Item> backpackItems = Backpack.getItems();
        ScriptConsole.println("[Alchemy] Total backpack items: " + backpackItems.size());
        ScriptConsole.println("[Alchemy] Configured patterns to match: " + getAlchemyItems());
        
        List<Item> matchingItems = backpackItems.stream()
                .filter(item -> {
                    String itemName = item.getName();
                    ScriptConsole.println("[Alchemy] Checking item: " + itemName);
                    boolean matched = alchemyPatterns.stream()
                            .anyMatch(pattern -> {
                                boolean matchResult = pattern.matcher(itemName).find();
                                ScriptConsole.println("[Alchemy] Pattern '" + pattern.pattern() + 
                                    "' match result for '" + itemName + "': " + matchResult);
                                return matchResult;
                            });
                    ScriptConsole.println("[Alchemy] Item matched: " + matched + " for " + itemName);
                    return matched;
                })
                .collect(Collectors.toList());
                
        ScriptConsole.println("[Alchemy] Found matching items: " + matchingItems.size());
        return matchingItems;
    }

    private boolean isAlchemySpellActive() {
        Component alchemyComponent = ComponentQuery.newQuery(218).text("High Level Alchemy").results().first();
        return alchemyComponent != null;
    }

    private boolean isCastingSpell() {
        return Client.getLocalPlayer().getAnimationId() != -1;
    }

    public void castAlchemy() {
        if (!isCastingSpell()) {
            if (!isAlchemySpellActive()) {
                selectAlchemySpell();
            }

            List<Item> itemsToAlch = fetchItemsToAlch();
            if (!itemsToAlch.isEmpty()) {
                Item itemToAlch = itemsToAlch.get(0);
                selectItemForAlchemy(itemToAlch);
            }
        }
    }

    private void selectAlchemySpell() {
        int alchemySpriteID = 14379;

        Component component = findComponentBySpriteId(alchemySpriteID);
        if (component != null) {
            boolean interacted = MiniMenu.interact(SelectableAction.SELECTABLE_COMPONENT.getType(), 0, -1,
                    (component.getInterfaceIndex() << 16) | component.getComponentIndex());
        }
    }

    private Component findComponentBySpriteId(int spriteID) {
        ResultSet<Component> components = ComponentQuery.newQuery(1219, 1430, 1670, 1671, 1672, 1673)
                .spriteId(spriteID)
                .option("Customise-keybind")
                .results();

        if (!components.isEmpty()) {
            return components.first();
        }
        return null;
    }

    private boolean clearSelection() {
        ScriptConsole.println("[Alchemy] Attempting to clear selection via ScriptBuilder invokeExact(9666)");
        List<ReturnValue> result = cancelSelectionScript.invokeExact();
        boolean success = !result.isEmpty() && result.get(0).asInt() == 1; 
        ScriptConsole.println("[Alchemy] Clear selection invoke success: " + success + " (Result size: " + result.size() + ")");
        if (success) {
            Execution.delay(script.getRandom().nextInt(100) + 50);
        }
        return success;
    }

    private void selectItemForAlchemy(Item item) {
        ScriptConsole.println("[Alchemy] Attempting to select item for alchemy: " + item.getName() + " (ID: " + item.getId() + ") in slot " + item.getSlot());
        boolean success = MiniMenu.interact(SelectableAction.SELECT_COMPONENT_ITEM.getType(), 0, item.getSlot(), 96534533);
        ScriptConsole.println("[Alchemy] Item selection success: " + success);
        if (success) {
            Execution.delay(script.getRandom().nextInt(150) + 100);
            clearSelection();
        }
    }

    public void clearAlchemyItems() {
        alchemyPatterns.clear();
    }

    public boolean hasItemsToAlchemize() {
        List<Item> backpackItems = Backpack.getItems();
        boolean hasItems = backpackItems.stream()
                .anyMatch(item -> {
                    String itemName = item.getName();
                    boolean matched = alchemyPatterns.stream()
                            .anyMatch(pattern -> pattern.matcher(itemName).find());
                    ScriptConsole.println("[Alchemy] Checking item: " + itemName + " - matched: " + matched);
                    return matched;
                });
        ScriptConsole.println("[Alchemy] Has items to alchemize: " + hasItems);
        return hasItems;
    }

    public void performAlchemyDuringCombat() {
        if (hasItemsToAlchemize()) {
            castAlchemy();
        }
    }

}
