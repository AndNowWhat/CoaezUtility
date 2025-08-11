package net.botwithus.tasks.clayurn;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

/**
 * Handles the pottery wheel interface and urn crafting logic
 */
public class UrnCraftingManager {
    private final CoaezUtility script;
    
    public UrnCraftingManager(CoaezUtility script) {
        this.script = script;
    }
    
    public void handlePotteryWheelInterface(UrnType selectedUrn) {
        if (selectedUrn == null) {
            ScriptConsole.println("[UrnCraftingManager] No urn selected for crafting");
            return;
        }
        
        int currentCategoryVar = VarManager.getVarValue(VarDomainType.PLAYER, 1169);
        int currentItemVar = VarManager.getVarValue(VarDomainType.PLAYER, 1170);
        
        ScriptConsole.println("[UrnCraftingManager] Current category var: " + currentCategoryVar + 
                ", current item var: " + currentItemVar);
        ScriptConsole.println("[UrnCraftingManager] Target urn ID: " + selectedUrn.getId());
        
        displayCurrentUrnInfo();
        
        if (currentCategoryVar != selectedUrn.getCategory().getEnumId()) {
            ScriptConsole.println("[UrnCraftingManager] Incorrect category selected, selecting correct category: " + 
                    selectedUrn.getCategory());
            selectUrnCategory(selectedUrn.getCategory());
            return;
        }
        
        if (currentItemVar == selectedUrn.getId()) {
            ScriptConsole.println("[UrnCraftingManager] Correct urn already selected: " + selectedUrn.getDisplayName());
            
            if (canCraftUrn()) {
                craftUrn();
            } else {
                ScriptConsole.println("[UrnCraftingManager] Cannot craft urn, no materials or requirements not met");
                Execution.delay(script.getRandom().nextInt(800, 1200));
            }
        } else {
            ScriptConsole.println("[UrnCraftingManager] Incorrect urn selected, selecting correct urn: " + 
                    selectedUrn.getDisplayName());
            selectUrnItem(selectedUrn);
        }
    }
    
    private void displayCurrentUrnInfo() {
        var currentUrnNameComponent = ComponentQuery.newQuery(1370).componentIndex(13).results().first();
        if (currentUrnNameComponent != null) {
            String currentUrnText = currentUrnNameComponent.getText();
            ScriptConsole.println("[UrnCraftingManager] Currently displayed urn: " + currentUrnText);
        }
    }
    
    private boolean canCraftUrn() {
        int canMakeVar = VarManager.getVarValue(VarDomainType.PLAYER, 8847);
        ScriptConsole.println("[UrnCraftingManager] Can make urn check (var 8847): " + canMakeVar);
        return canMakeVar > 0;
    }
    
    private void selectUrnCategory(UrnCategory category) {
        int dropdownComponentId = 89849884;
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, dropdownComponentId);
        Execution.delay(script.getRandom().nextInt(100, 200));
        
        int categoryIndex = category.getIndex();
        int componentIndex = (categoryIndex * 2) + 1;
        int categoryComponentId = 96797588;
        
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, componentIndex, categoryComponentId);
        Execution.delay(script.getRandom().nextInt(300, 800));
    }
    
    private void selectUrnItem(UrnType urn) {
        int currentCategoryVar = VarManager.getVarValue(VarDomainType.PLAYER, 1169);
        EnumType categoryEnum = ConfigManager.getEnumType(currentCategoryVar);
        
        if (categoryEnum == null || categoryEnum.getOutputs() == null) {
            ScriptConsole.println("[UrnCraftingManager] Error: Could not get category enum for var " + currentCategoryVar);
            return;
        }
        
        int urnEnumIndex = findUrnIndexInEnum(urn, categoryEnum);
        if (urnEnumIndex == -1) {
            ScriptConsole.println("[UrnCraftingManager] Error: Could not find urn " + urn.getId() + " in category enum");
            return;
        }
        
        ScriptConsole.println("[UrnCraftingManager] Found urn at enum index: " + urnEnumIndex);
        
        int componentIndex = (urnEnumIndex * 4) + 1;
        ScriptConsole.println("[UrnCraftingManager] Using component index: " + componentIndex + 
                " (enum index " + urnEnumIndex + " * 4 + 1) with component ID: 89849878");
        
        if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, componentIndex, 89849878)) {
            verifyUrnSelection(urn);
        } else {
            ScriptConsole.println("[UrnCraftingManager] Failed to select urn using MiniMenu.interact");
        }
        
        Execution.delay(script.getRandom().nextInt(300, 800));
    }
    
    private int findUrnIndexInEnum(UrnType urn, EnumType categoryEnum) {
        for (int i = 0; i < categoryEnum.getOutputs().size(); i++) {
            Object output = categoryEnum.getOutputs().get(i);
            if (output instanceof Integer && (Integer) output == urn.getId()) {
                return i;
            }
        }
        return -1;
    }
    
    private void verifyUrnSelection(UrnType urn) {
        Execution.delay(script.getRandom().nextInt(300, 600));
        
        int newCurrentItemVar = VarManager.getVarValue(VarDomainType.PLAYER, 1170);
        ScriptConsole.println("[UrnCraftingManager] After selection, current item var: " + newCurrentItemVar + 
                " (target: " + urn.getId() + ")");
        
        if (newCurrentItemVar == urn.getId()) {
            ScriptConsole.println("[UrnCraftingManager] Successfully selected urn: " + urn.getDisplayName());
        } else {
            ScriptConsole.println("[UrnCraftingManager] Selection failed - current item var did not change to target urn ID");
        }
    }
    
    private boolean craftUrn() {
        ScriptConsole.println("[UrnCraftingManager] Starting urn crafting using dialogue action (component ID: 89784350)");
        
        if (MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350)) {
            ScriptConsole.println("[UrnCraftingManager] Successfully started crafting urn");
            return true;
        } else {
            ScriptConsole.println("[UrnCraftingManager] Failed to start crafting using dialogue action");
            return false;
        }
    }
    
    public int getRequiredSoftClayForUrn(UrnType urn) {
        ItemType itemType = ConfigManager.getItemType(urn.getId());
        if (itemType == null) return 0;
        return itemType.getIntParam(2665); // craft_quantity_1
    }
}