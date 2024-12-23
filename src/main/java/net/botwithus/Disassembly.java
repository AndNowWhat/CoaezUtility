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
    private final List<String> disassemblyItems = new ArrayList<>();
    private boolean disassemblyEnabled = true;

    public void setDisassemblyEnabled(boolean enabled) {
        ScriptConsole.println("[Disassembly] Setting enabled state to: " + enabled);
        this.disassemblyEnabled = enabled;
    }

    public boolean isDisassemblyEnabled() {
        return disassemblyEnabled;
    }

    public void addDisassemblyItem(String itemName) {
        if (!disassemblyItems.contains(itemName)) {
            ScriptConsole.println("[Disassembly] Adding item to list: " + itemName);
            disassemblyItems.add(itemName);
        }
    }

    public void removeDisassemblyItem(String itemName) {
        ScriptConsole.println("[Disassembly] Removing item from list: " + itemName);
        disassemblyItems.remove(itemName);
    }

    public List<String> getDisassemblyItems() {
        return new ArrayList<>(disassemblyItems);
    }

    private List<Item> fetchItemsToDisassemble() {
        List<Item> backpackItems = Backpack.getItems();
        ScriptConsole.println("[Disassembly] Total backpack items: " + backpackItems.size());
        ScriptConsole.println("[Disassembly] Configured items to disassemble: " + disassemblyItems);
        
        List<Item> matchingItems = backpackItems.stream()
                .filter(item -> {
                    String itemName = item.getName();
                    ScriptConsole.println("[Disassembly] Checking item: " + itemName);
                    boolean matched = disassemblyItems.stream()
                            .anyMatch(disItem -> itemName.toLowerCase().contains(disItem.toLowerCase()));
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

    public void castDisassembly() {
        if (!disassemblyEnabled) {
            ScriptConsole.println("[Disassembly] Disassembly is disabled, skipping cast");
            return;
        }

        ScriptConsole.println("[Disassembly] Starting castDisassembly");
        
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
        ScriptConsole.println("[Disassembly] Clearing all disassembly items");
        disassemblyItems.clear();
    }

    public boolean hasItemsToDisassemble() {
        List<String> disassemblyItems = getDisassemblyItems();
        List<Item> backpackItems = Backpack.getItems();
        boolean hasItems = backpackItems.stream()
                .anyMatch(item -> disassemblyItems.stream()
                        .anyMatch(name -> item.getName().equalsIgnoreCase(name)));
        ScriptConsole.println("[Disassembly] Has items to disassemble: " + hasItems);
        return hasItems;
    }
}