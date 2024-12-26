package net.botwithus;

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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Disassembly {
    private final List<Pattern> disassemblyPatterns = new ArrayList<>();
    private boolean disassemblyEnabled = true;

    public void setDisassemblyEnabled(boolean enabled) {
        ScriptConsole.println("[Disassembly] Setting enabled state to: " + enabled);
        this.disassemblyEnabled = enabled;
    }

    public boolean isDisassemblyEnabled() {
        return disassemblyEnabled;
    }

    public void addDisassemblyItem(String pattern) {
        // Convert the pattern to handle common cases
        String processedPattern = pattern.toLowerCase()
            // Escape special regex characters
            .replace(".", "\\.")
            .replace("*", ".*")
            // Add word boundaries for exact matches
            .replace(" ", "\\s+");
            
        // Create the pattern with case-insensitive flag
        Pattern itemPattern = Pattern.compile(processedPattern, Pattern.CASE_INSENSITIVE);
        ScriptConsole.println("[Disassembly] Adding pattern to list: " + processedPattern);
        disassemblyPatterns.add(itemPattern);
    }

    public void removeDisassemblyItem(String pattern) {
        ScriptConsole.println("[Disassembly] Removing pattern from list: " + pattern);
        disassemblyPatterns.removeIf(p -> p.pattern().equals(pattern));
    }

    public List<String> getDisassemblyItems() {
        return disassemblyPatterns.stream()
                .map(Pattern::pattern)
                .collect(Collectors.toList());
    }

    private List<Item> fetchItemsToDisassemble() {
        List<Item> backpackItems = Backpack.getItems();
        ScriptConsole.println("[Disassembly] Total backpack items: " + backpackItems.size());
        ScriptConsole.println("[Disassembly] Configured patterns to match: " + getDisassemblyItems());
        
        List<Item> matchingItems = backpackItems.stream()
                .filter(item -> {
                    String itemName = item.getName();
                    ScriptConsole.println("[Disassembly] Checking item: " + itemName);
                    boolean matched = disassemblyPatterns.stream()
                            .anyMatch(pattern -> pattern.matcher(itemName).find());
                    ScriptConsole.println("[Disassembly] Item matched: " + matched + " for " + itemName);
                    return matched;
                })
                .collect(Collectors.toList());
                
        ScriptConsole.println("[Disassembly] Found matching items: " + matchingItems.size());
        return matchingItems;
    }

    private boolean isDisassembleActive() {
        Component disassembleComponent = ComponentQuery.newQuery(218).text("Disassembly").results().first();
        ScriptConsole.println("[Disassembly] Disassemble active check: " + (disassembleComponent != null));
        return disassembleComponent != null;
    }

    private boolean isCastingAnimation() {
        boolean animating = Client.getLocalPlayer().getAnimationId() != -1;
        ScriptConsole.println("[Disassembly] Animation check: " + animating);
        return animating;
    }

    private CoaezUtility script;

    public Disassembly(CoaezUtility script) {
        this.script = script;
    }

    public void castDisassembly() {
        if (!disassemblyEnabled) {
            ScriptConsole.println("[Disassembly] Disassembly is disabled, skipping cast");
            return;
        }

        ScriptConsole.println("[Disassembly] Starting castDisassembly");
        
        // Check if we have items after loading a preset
        if (!hasItemsToDisassemble()) {
            ScriptConsole.println("[Disassembly] No matching items found in backpack after preset load, stopping script");
            script.setActive(false);
            return;
        }
        
        if (isCastingAnimation()) {
            ScriptConsole.println("[Disassembly] Animation in progress, skipping");
            return;
        }

        if (!isDisassembleActive()) {
            ScriptConsole.println("[Disassembly] Disassemble not active, selecting action");
            selectDisassembleAction();
            Execution.delay(1200);
        }

        List<Item> itemsToDisassemble = fetchItemsToDisassemble();
        ScriptConsole.println("[Disassembly] Found " + itemsToDisassemble.size() + " items to disassemble");
        
        if (!itemsToDisassemble.isEmpty()) {
            Item itemToDisassemble = itemsToDisassemble.get(0);
            ScriptConsole.println("[Disassembly] Selecting item: " + itemToDisassemble.getName() + " in slot " + itemToDisassemble.getSlot());
            selectItemForDisassembly(itemToDisassemble);
        }
    }

    private void selectDisassembleAction() {
        int disassembleSpriteID = 12510;
        Component component = findComponentBySpriteId(disassembleSpriteID);
        ScriptConsole.println("[Disassembly] Found disassemble component: " + (component != null));
        
        if (component != null) {
            int interfaceId = (component.getInterfaceIndex() << 16) | component.getComponentIndex();
            ScriptConsole.println("[Disassembly] Interacting with interface ID: " + interfaceId);
            boolean success = MiniMenu.interact(SelectableAction.SELECTABLE_COMPONENT.getType(), 0, -1, interfaceId);
            ScriptConsole.println("[Disassembly] Interaction success: " + success);
        }
    }

    private Component findComponentBySpriteId(int spriteID) {
        ResultSet<Component> components = ComponentQuery.newQuery(1219, 1430, 1670, 1671, 1672, 1673)
                .spriteId(spriteID)
                .option("Customise-keybind")
                .results();

        ScriptConsole.println("[Disassembly] Component search results size: " + components.size());
        return !components.isEmpty() ? components.first() : null;
    }

    private void selectItemForDisassembly(Item item) {
        boolean success = MiniMenu.interact(SelectableAction.SELECT_COMPONENT_ITEM.getType(), 0, item.getSlot(), 96534533);
        ScriptConsole.println("[Disassembly] Item selection success: " + success);
    }

    public void clearDisassemblyItems() {
        ScriptConsole.println("[Disassembly] Clearing all disassembly patterns");
        disassemblyPatterns.clear();
    }

    public boolean hasItemsToDisassemble() {
        List<Item> backpackItems = Backpack.getItems();
        boolean hasItems = backpackItems.stream()
                .anyMatch(item -> disassemblyPatterns.stream()
                        .anyMatch(pattern -> pattern.matcher(item.getName()).find()));
        ScriptConsole.println("[Disassembly] Has items to disassemble: " + hasItems);
        return hasItems;
    }
}