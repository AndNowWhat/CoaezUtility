package net.botwithus.tasks.clayurn;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.DepositBox;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Helper class for inventory-related operations
 */
public class InventoryHelper {
    
    public static boolean hasRegularClay() {
        return Backpack.contains("Clay");
    }
    
    public static boolean hasSoftClay() {
        return Backpack.contains("Soft clay");
    }
    
    public static int getSoftClayCount() {
        return Backpack.getItems().stream()
                .filter(item -> item != null && "Soft clay".equals(item.getName()))
                .mapToInt(Item::getStackSize)
                .sum();
    }
    
    public static boolean hasUnfiredUrns() {
        return Backpack.getItems().stream()
                .anyMatch(item -> item != null && item.getName() != null && 
                        item.getName().toLowerCase().contains("unfired") && 
                        item.getName().toLowerCase().contains("urn"));
    }
    
    public static boolean hasFiredUrnsNeedingRunes() {
        return Backpack.getItems().stream()
                .anyMatch(item -> item != null && item.getName() != null &&
                        item.getName().toLowerCase().contains("no rune"));
    }
    
    public static Item getFirstUrnNeedingRunes() {
        return Backpack.getItems().stream()
                .filter(item -> item != null && item.getId() != -1 &&
                        item.getName().toLowerCase().contains("no rune"))
                .findFirst()
                .orElse(null);
    }
    
    public static boolean addRunesToUrn(Item urnItem) {
        if (urnItem == null) {
            return false;
        }
        
        List<String> options = Objects.requireNonNull(urnItem.getConfigType()).getBackpackOptions();
        if (options == null || options.isEmpty()) {
            ScriptConsole.println("[InventoryHelper] No options found for urn item");
            return false;
        }
        
        String option = options.get(0);
        ScriptConsole.println("[InventoryHelper] Interacting with urn: " + urnItem.getName() + " using option: " + option);
        
        return Backpack.interact(urnItem.getName(), option);
    }
    
    public static void depositAllItems() {
        if (DepositBox.isOpen()) {
            DepositBox.depositAll();
            Execution.delay(850L);
        }
    }
    
    public static boolean depositSpecificUrns(UrnQueue urnQueue) {
        if (!DepositBox.isOpen()) {
            return false;
        }
        
        Pattern urnPattern = Pattern.compile("urn.*\\(empty\\)", Pattern.CASE_INSENSITIVE);
        List<Item> items = Backpack.getItems();
        List<Integer> urnSlots = new ArrayList<>();
        
        // Find slots containing urns from the queue
        for (UrnType urnType : urnQueue.getQueue().keySet()) {
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                if (item != null && item.getName() != null && 
                    urnPattern.matcher(item.getName()).find() && item.getId() == urnType.getId()) {
                    urnSlots.add(i);
                }
            }
        }
        
        boolean allDeposited = true;
        for (int slotNum : urnSlots) {
            if (net.botwithus.rs3.game.inventories.Backpack.getSlot(slotNum) != null) {
                boolean interacted = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 4, slotNum, 720915);
                if (!interacted) {
                    ScriptConsole.println("[InventoryHelper] Failed to interact with deposit box for slot " + slotNum);
                    allDeposited = false;
                    continue;
                }
                
                boolean removed = Execution.delayUntil(1000L, () -> 
                        net.botwithus.rs3.game.inventories.Backpack.getSlot(slotNum) == null);
                if (!removed) {
                    ScriptConsole.println("[InventoryHelper] Urn in slot " + slotNum + " was not removed after deposit");
                    allDeposited = false;
                }
            }
        }
        
        return allDeposited;
    }
    
    public static boolean hasRemainingUrns() {
        return Backpack.getItems().stream()
                .anyMatch(item -> item != null && item.getName() != null && 
                        item.getName().toLowerCase().contains("urn") && 
                        item.getName().toLowerCase().contains("(empty)"));
    }
    
    public static List<String> getRemainingUrnNames() {
        return Backpack.getItems().stream()
                .filter(item -> item != null && item.getName() != null && 
                        item.getName().toLowerCase().contains("urn") && 
                        item.getName().toLowerCase().contains("(empty)"))
                .map(Item::getName)
                .toList();
    }
}