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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Alchemy {
    private final List<String> alchemyItems = new ArrayList<>();
    private boolean alchemyEnabled = true;

    public void setAlchemyEnabled(boolean enabled) {
        this.alchemyEnabled = enabled;
    }

    public boolean isAlchemyEnabled() {
        return alchemyEnabled;
    }

    public void addAlchemyItem(String itemName) {
        if (!alchemyItems.contains(itemName)) {
            alchemyItems.add(itemName);
        }
    }

    public void removeAlchemyItem(String itemName) {
        alchemyItems.remove(itemName);
    }

    public List<String> getAlchemyItems() {
        return new ArrayList<>(alchemyItems);
    }

    private List<Item> fetchItemsToAlch() {
        return Backpack.getItems().stream()
                .filter(item -> alchemyItems.stream().anyMatch(alchItem -> Pattern.compile(Pattern.quote(alchItem), Pattern.CASE_INSENSITIVE).matcher(item.getName()).matches()))
                .collect(Collectors.toList());
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

    private void selectItemForAlchemy(Item item) {
        MiniMenu.interact(SelectableAction.SELECT_COMPONENT_ITEM.getType(), 0, item.getSlot(), 96534533);
    }

    public void clearAlchemyItems() {
        alchemyItems.clear();
    }

    public boolean hasItemsToAlchemize() {
        List<String> alchemyItems = getAlchemyItems();
        List<Item> backpackItems = Backpack.getItems();
        for (String itemName : alchemyItems) {
            boolean found = backpackItems.stream().anyMatch(item -> item.getName().equalsIgnoreCase(itemName));
            if (found) {
                return true;
            }
        }

        return false;
    }

    public void performAlchemyDuringCombat() {
        if (hasItemsToAlchemize()) {
            castAlchemy();
            Execution.delay(1200);

        }
    }

}
