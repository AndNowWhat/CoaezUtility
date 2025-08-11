package net.botwithus.tasks.clayurn;

import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages loading and caching of urn data from game configuration
 */
public class UrnDataManager {
    private static final int POTTERY_GROUP_ENUM_ID = 7004;
    
    private List<UrnCategory> availableCategories;
    private List<UrnType> availableUrns;
    private boolean dataLoaded = false;
    
    public void loadUrnData() {
        try {
            availableCategories = new ArrayList<>();
            availableUrns = new ArrayList<>();
            
            ScriptConsole.println("[UrnDataManager] Attempting to load urn data from enum " + POTTERY_GROUP_ENUM_ID);
            
            EnumType potteryGroupEnum = ConfigManager.getEnumType(POTTERY_GROUP_ENUM_ID);
            if (potteryGroupEnum == null || potteryGroupEnum.getOutputs() == null) {
                ScriptConsole.println("[UrnDataManager] Error: Could not load pottery group enum");
                return;
            }
            
            ScriptConsole.println("[UrnDataManager] Successfully loaded pottery group enum with " + 
                    potteryGroupEnum.getOutputs().size() + " outputs");
            
            processCategories(potteryGroupEnum);
            
            dataLoaded = !availableCategories.isEmpty() && !availableUrns.isEmpty();
            
            ScriptConsole.println("[UrnDataManager] Successfully loaded " + availableCategories.size() + 
                    " categories and " + availableUrns.size() + " urn types");
            
        } catch (Exception e) {
            ScriptConsole.println("[UrnDataManager] Error loading urn data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processCategories(EnumType potteryGroupEnum) {
        for (int i = 0; i < potteryGroupEnum.getOutputs().size(); i++) {
            Object categoryEnumIdObj = potteryGroupEnum.getOutputs().get(i);
            
            if (categoryEnumIdObj instanceof Integer) {
                int categoryEnumId = (Integer) categoryEnumIdObj;
                processCategory(i, categoryEnumId);
            }
        }
    }
    
    private void processCategory(int index, int categoryEnumId) {
        EnumType categoryEnum = ConfigManager.getEnumType(categoryEnumId);
        if (categoryEnum == null || categoryEnum.getOutputs() == null) {
            return;
        }
        
        UrnCategory category = new UrnCategory(index, getCategoryDisplayName(index), categoryEnumId);
        availableCategories.add(category);
        
        ScriptConsole.println("[UrnDataManager] Category " + index + ": " + category.getDisplayName() + 
                " (enum " + categoryEnumId + ") with " + categoryEnum.getOutputs().size() + " products");
        
        processUrnsInCategory(category, categoryEnum);
    }
    
    private void processUrnsInCategory(UrnCategory category, EnumType categoryEnum) {
        for (Object productIdObj : categoryEnum.getOutputs()) {
            if (productIdObj instanceof Integer) {
                int productId = (Integer) productIdObj;
                
                ItemType itemType = ConfigManager.getItemType(productId);
                if (itemType != null && itemType.getName() != null) {
                    String itemName = itemType.getName();
                    if (itemName.contains("urn") && itemName.contains("unfired")) {
                        UrnType urn = new UrnType(productId, itemName, category);
                        availableUrns.add(urn);
                        ScriptConsole.println("[UrnDataManager]   - " + itemName + " (ID: " + productId + ")");
                    }
                }
            }
        }
    }
    
    private String getCategoryDisplayName(int index) {
        switch (index) {
            case 0: return "Pottery";
            case 1: return "Cooking Urns";
            case 2: return "Divination Urns";
            case 3: return "Farming Urns";
            case 4: return "Fishing Urns";
            case 5: return "Hunter Urns";
            case 6: return "Mining Urns";
            case 7: return "Prayer Urns";
            case 8: return "Runecrafting Urns";
            case 9: return "Smelting Urns";
            case 10: return "Woodcutting Urns";
            default: return "Category " + index;
        }
    }
    
    public List<UrnCategory> getAvailableCategories() {
        return availableCategories != null ? new ArrayList<>(availableCategories) : new ArrayList<>();
    }
    
    public List<UrnType> getAvailableUrns() {
        return availableUrns != null ? new ArrayList<>(availableUrns) : new ArrayList<>();
    }
    
    public List<UrnType> getUrnsInCategory(UrnCategory category) {
        if (availableUrns == null || category == null) {
            return new ArrayList<>();
        }
        return availableUrns.stream()
                .filter(urn -> urn.getCategory().equals(category))
                .toList();
    }
    
    public UrnType getUrnById(int id) {
        if (availableUrns == null) {
            return null;
        }
        return availableUrns.stream()
                .filter(urn -> urn.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    public boolean isDataLoaded() {
        return dataLoaded;
    }
    
    public void printAvailableUrns() {
        if (!isDataLoaded()) {
            ScriptConsole.println("[UrnDataManager] No urn data available");
            return;
        }
        
        ScriptConsole.println("[UrnDataManager] Available urn categories:");
        for (UrnCategory category : availableCategories) {
            ScriptConsole.println("  " + category.getIndex() + ": " + category.getDisplayName() + 
                    " (enum " + category.getEnumId() + ")");
        }
        
        ScriptConsole.println("[UrnDataManager] Available urn types:");
        for (UrnType urn : availableUrns) {
            ScriptConsole.println("  " + urn.getId() + ": " + urn.getDisplayName() + 
                    " (" + urn.getCategory().getDisplayName() + ")");
        }
    }
}