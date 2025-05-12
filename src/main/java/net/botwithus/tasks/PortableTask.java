package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;

import java.util.List;
import java.util.ArrayList;

public class PortableTask implements Task {
    private final CoaezUtility script;
    private Portable currentPortable;
    private Product currentProduct;

    private static final int MAKE_X_LIST_INTERFACE_ID = 1370;
    private static final int CONFIRM_MAKE_X_LIST_INTERFACE_ID = 1371;
    private static final int CRAFTING_IN_PROGRESS_INTERFACE_ID = 1251;
    private static final int VARP_ID_SELECTED_ITEM = 1170;
    private static final int VARP_ID_SELECTED_CATEGORY_ENUM = 1169;
    
    private static final int PRODUCT_LIST_CONTAINER_COMPONENT_INDEX = 22;
    private static final int COMPONENT_INDEX_GROUP_DROPDOWN_TRIGGER = 1;
    private static final int COMPONENT_INDEX_GROUP_DROPDOWN_LIST = 2;
    private static final int COMPONENT_INDEX_MAKE_BUTTON = 30;
    
    private static final int MAKE_BUTTON_ACTION = 89784350;
    private static final int GROUP_DROPDOWN_TRIGGER_ACTION = 89849884;
    private static final int GROUP_SELECT_ACTION = 96797586;

    private static final int ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX = 1;
    private static final int ITEM_SELECTION_INTERACTION_ACTION_ID = 89849878;

    /**
     * Constructs a new PortableTask.
     * @param script The main script instance.
     */
    public PortableTask(CoaezUtility script) {
        this.script = script;
        setActivePortable(new PortableWorkbench(script));
    }

    /**
     * Sets the currently active portable station for the task.
     * @param portable The portable station to set as active.
     */
    public void setActivePortable(Portable portable) {
        this.currentPortable = portable;
        if (portable != null) {
            ScriptConsole.println("[PortableTask] Active portable set to: " + portable.getType().getName());
            if (portable instanceof PortableWorkbench) {
                this.currentProduct = ((PortableWorkbench) portable).getSelectedProduct();
                if (this.currentProduct != null) {
                     ScriptConsole.println("[PortableTask] Default product for workbench: " + this.currentProduct.getName());
                }
            } else {
                this.currentProduct = null;
            }
        } else {
            ScriptConsole.println("[PortableTask] Active portable cleared.");
            this.currentProduct = null;
        }
    }

    /**
     * Gets the currently active portable station.
     * @return The active Portable object, or null if none is set.
     */
    public Portable getActivePortable() {
        return currentPortable;
    }

    /**
     * Sets the product to be created, primarily for PortableWorkbenches.
     * @param product The product to select.
     */
    public void setSelectedProduct(Product product) {
        this.currentProduct = product;
        if (currentPortable instanceof PortableWorkbench) {
            ((PortableWorkbench) currentPortable).setSelectedProduct(product);
            if (product != null) {
                 ScriptConsole.println("[PortableTask] Product for workbench set to: " + product.getName());
            }
        } else if (product != null) {
            ScriptConsole.println("[PortableTask] Warning: Tried to set product on a non-workbench portable: " + currentPortable.getType().getName());
        }
    }

    /**
     * Gets the currently selected product.
     * @return The selected Product object, or null if none is set.
     */
    public Product getSelectedProduct() {
        return currentProduct;
    }

    /**
     * Executes the main logic for the portable task, handling interface interactions and item requirements.
     */
    @Override
    public void execute() {
        if (currentPortable == null) {
            ScriptConsole.println("[PortableTask] No active portable selected. Stopping.");
            return;
        }

        ScriptConsole.println("[PortableTask] Executing for: " + currentPortable.getType().getName()
            + (currentProduct != null ? " (Product: " + currentProduct.getName() + ")" : ""));

        if (currentPortable instanceof PortableWorkbench && Interfaces.isOpen(MAKE_X_LIST_INTERFACE_ID)) {
            handleWorkbenchMakeXInterface((PortableWorkbench) currentPortable);
            return;
        }
        
        if (Interfaces.isOpen(CRAFTING_IN_PROGRESS_INTERFACE_ID)) {
            ScriptConsole.println("[" + currentPortable.getType().getName() + "] Crafting interface (" + CRAFTING_IN_PROGRESS_INTERFACE_ID + ") is open, waiting...");
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(CRAFTING_IN_PROGRESS_INTERFACE_ID));
            return;
        }
        
        if (currentPortable.hasRequiredItems()) {
            ScriptConsole.println("[" + currentPortable.getType().getName() + "] Found required items, interacting...");
            currentPortable.interact();
        } else {
            ScriptConsole.println("[" + currentPortable.getType().getName() + "] No/Not enough required items found for " + (currentProduct != null ? currentProduct.getName() : "selected portable") + ", loading preset.");
            script.setWaitingForPreset(true);
            script.setPresetLoaded(false);
            Bank.loadLastPreset();
            Execution.delayUntil(script.getRandom().nextInt(5000) + 5000L, () -> script.isPresetLoaded());
        }
    }

    /**
     * Handles category and item selection within the Make-X List interface (1371) for Portable Workbenches.
     * This includes selecting the correct category via dropdown and then selecting the target item from the list.
     * @param workbench The PortableWorkbench instance being used.
     */
    private void handleWorkbenchMakeXInterface(PortableWorkbench workbench) {
        Product targetProduct = workbench.getSelectedProduct();
        if (targetProduct == null) {
            ScriptConsole.println("[PortableTask] Make-X Interface (1371) open, but no target product set. Cannot proceed.");
            return;
        }

        int targetProductId = targetProduct.getId();
        int currentCategoryEnumId = VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_CATEGORY_ENUM);
        ScriptConsole.println("[PortableTask] Checking category in 1371. Current Category Enum ID: " + currentCategoryEnumId);

        EnumType currentCategoryEnum = ConfigManager.getEnumType(currentCategoryEnumId);
        boolean isProductInCurrentCategory = false;
        if (currentCategoryEnum != null && currentCategoryEnum.getOutputs() != null) {
            isProductInCurrentCategory = currentCategoryEnum.getOutputs().contains(targetProductId);
        }

        if (!isProductInCurrentCategory) {
            ScriptConsole.println("[PortableTask] Target product " + targetProduct.getName() + " not in current category (" + currentCategoryEnumId + "). Finding and selecting correct category...");
            
            int targetCategoryEnumId = -1;
            int targetCategoryIndex = -1;
            EnumType mainGroupEnum = ConfigManager.getEnumType(PortableWorkbench.FLATPACK_GROUP_ENUM_ID);
            if (mainGroupEnum != null && mainGroupEnum.getOutputs() != null) {
                List<Object> subGroupEnumIds = mainGroupEnum.getOutputs();
                for (int i = 0; i < subGroupEnumIds.size(); i++) {
                    if (subGroupEnumIds.get(i) instanceof Integer) {
                        int subGroupId = (Integer) subGroupEnumIds.get(i);
                        EnumType subGroupEnum = ConfigManager.getEnumType(subGroupId);
                        if (subGroupEnum != null && subGroupEnum.getOutputs() != null && subGroupEnum.getOutputs().contains(targetProductId)) {
                            targetCategoryEnumId = subGroupId;
                            targetCategoryIndex = i;
                            ScriptConsole.println("[PortableTask] Found target product in category Enum ID: " + targetCategoryEnumId + " at index " + targetCategoryIndex);
                            break;
                        }
                    }
                }
            }

            if (targetCategoryIndex != -1) {
                ScriptConsole.println("[PortableTask] Interacting to open category dropdown (CompIdx: " + COMPONENT_INDEX_GROUP_DROPDOWN_TRIGGER + ")...");
                if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), COMPONENT_INDEX_GROUP_DROPDOWN_TRIGGER, -1, GROUP_DROPDOWN_TRIGGER_ACTION)) {
                    Execution.delay(script.getRandom().nextInt(400) + 300); 
                    
                    int categoryListSubIndex = (targetCategoryIndex * 2) + 1; 
                    ScriptConsole.println("[PortableTask] Interacting to select category (CompIdx: " + COMPONENT_INDEX_GROUP_DROPDOWN_LIST + ", SubIndex: " + categoryListSubIndex + ")...");
                    if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), COMPONENT_INDEX_GROUP_DROPDOWN_LIST, categoryListSubIndex, GROUP_SELECT_ACTION)) {
                        Execution.delay(script.getRandom().nextInt(600) + 400); 
                    } else {
                        ScriptConsole.println("[PortableTask] Failed to interact with category selection list (Action: " + GROUP_SELECT_ACTION + ").");
                    }
                } else {
                     ScriptConsole.println("[PortableTask] Failed to interact with category dropdown trigger (Action: " + GROUP_DROPDOWN_TRIGGER_ACTION + ").");
                }
            } else {
                 ScriptConsole.println("[PortableTask] CRITICAL: Could not find the correct category enum containing the target product ID " + targetProductId + ".");
            }
            return; 
        } else {
            ScriptConsole.println("[PortableTask] Correct category selected (" + currentCategoryEnumId + "). Checking selected item...");
            int selectedItemIdInInterface = VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_ITEM);
            ScriptConsole.println("[PortableTask] Item Check in 1371: Target=" + targetProductId + ", Interface Selection=" + selectedItemIdInInterface);

            if (selectedItemIdInInterface == targetProductId) {
                ScriptConsole.println("[PortableTask] Correct item selected. Clicking Make button (CompIdx: "+ COMPONENT_INDEX_MAKE_BUTTON +")...");
                Component makeButton = ComponentQuery.newQuery(MAKE_X_LIST_INTERFACE_ID)
                        .componentIndex(COMPONENT_INDEX_MAKE_BUTTON)
                        .results().first();
                        
                if (makeButton != null) {
                    ScriptConsole.println("[PortableTask] Interacting with Make button using DIALOGUE action (ActionID: " + MAKE_BUTTON_ACTION + ")...");
                    if (MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, MAKE_BUTTON_ACTION)) {
                       Execution.delay(script.getRandom().nextInt(1000) + 800); 
                    } else {
                       ScriptConsole.println("[PortableTask] Failed to interact with Make button using DIALOGUE action (ActionID: " + MAKE_BUTTON_ACTION + ").");
                    }
                } else {
                    ScriptConsole.println("[PortableTask] Could not find Make button component (CompIdx: " + COMPONENT_INDEX_MAKE_BUTTON + "). Check Component Index.");
                }
                return; 
            } else {
                ScriptConsole.println("[PortableTask] Incorrect item selected. Attempting to select " + targetProduct.getName() + " (ID: " + targetProductId + ") using revised logic.");

                EnumType currentCategoryData = ConfigManager.getEnumType(currentCategoryEnumId);
                if (currentCategoryData == null || currentCategoryData.getOutputs() == null) {
                    ScriptConsole.println("[PortableTask] CRITICAL: Could not get current category enum data or its outputs for enum ID: " + currentCategoryEnumId);
                    return; 
                }
                List<Object> itemIdsInCurrentCategoryEnum = currentCategoryData.getOutputs();
                
                int productIndexInEnum = -1;
                for (int i = 0; i < itemIdsInCurrentCategoryEnum.size(); i++) {
                    if (itemIdsInCurrentCategoryEnum.get(i) instanceof Integer && ((Integer)itemIdsInCurrentCategoryEnum.get(i)).intValue() == targetProductId) {
                        productIndexInEnum = i;
                        break;
                    }
                }

                if (productIndexInEnum == -1) {
                    ScriptConsole.println("[PortableTask] CRITICAL: Target product ID " + targetProductId + " ("+targetProduct.getName()+") not found in the current category enum list (ID: " + currentCategoryEnumId + "). Contents: " + itemIdsInCurrentCategoryEnum.toString());
                    return; 
                }
                ScriptConsole.println("[PortableTask] Target product " + targetProduct.getName() + " found at index " + productIndexInEnum + " in category enum " + currentCategoryEnumId + ".");

                int subComponentIndexToSelect = (productIndexInEnum * 4) + 1;
                ScriptConsole.println("[PortableTask] Calculated target SubComponentIndex based on enum index: " + subComponentIndexToSelect);

                ScriptConsole.println("[PortableTask] Attempting item selection interaction: Interface=" + CONFIRM_MAKE_X_LIST_INTERFACE_ID + 
                                      ", TargetComponentIndex=" + ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX + 
                                      ", SubComponentIndexToUse=" + subComponentIndexToSelect + 
                                      ", ActionID=" + ITEM_SELECTION_INTERACTION_ACTION_ID);

                if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), 
                                      ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX, 
                                      subComponentIndexToSelect, 
                                      ITEM_SELECTION_INTERACTION_ACTION_ID)) {
                    ScriptConsole.println("[PortableTask] Successfully initiated interaction using calculated SubComponentIndex to select item " + targetProduct.getName());
                    Execution.delay(script.getRandom().nextInt(600) + 400); 
                } else {
                    ScriptConsole.println("[PortableTask] Failed to interact to select item using calculated SubComponentIndex. TargetCompIndex=" + ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX + 
                                          ", CalculatedSubCompIndexToUse=" + subComponentIndexToSelect + ", ActionID=" + ITEM_SELECTION_INTERACTION_ACTION_ID);
                }

                return; 
            }
        }
    }
}
