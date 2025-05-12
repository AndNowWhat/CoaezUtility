package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap; // Use LinkedHashMap to preserve insertion order for groups
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete implementation for the Portable Crafter.
 * Handles multiple interaction options and, for options like "Cut Gems",
 * manages internal categories (groups) and products similar to PortableWorkbench.
 */
public class PortableCrafter extends Portable {

    // --- Enum IDs ---
    private static final int GEM_GROUP_ENUM_ID = 6981; // recipe_products_group_crafting_gem
    private static final int GEM_CUTTING_ENUM_ID = 6983;
    private static final int GEM_CRUSHING_ENUM_ID = 6240;
    private static final int BOLT_TIPS_ENUM_ID = 6961;
    private static final int TAN_LEATHER_PRODUCTS_ENUM_ID = 2018; // Added for Tan Leather products

    // --- Interaction Options ---
    public static final String OPT_CUT_GEMS = "Cut Gems";
    public static final String OPT_CRAFT = "Craft"; // Generic
    public static final String OPT_CLAY = "Clay Crafting";
    public static final String OPT_TAN_LEATHER = "Tan Leather"; // Re-added

    public static final String[] CRAFTER_OPTIONS = {
            OPT_CUT_GEMS, OPT_CRAFT, OPT_CLAY, OPT_TAN_LEATHER // Re-added Tan Leather
    };

    // --- Interface Constants (Copied/Adapted from PortableTask) ---
    private static final int CRAFTER_MAKE_X_INTERFACE_ID = 1371; // Interface for product selection
    private static final int VARP_ID_SELECTED_ITEM = 1170; // VARP that tracks the selected item ID in 1371
    private static final int COMPONENT_INDEX_MAKE_BUTTON = 30; // Index of the 'Make' button in 1371
    private static final int MAKE_BUTTON_ACTION = 89784350; // Action ID for the 'Make' button
    private static final int ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX = 1; // Main component index for item list in 1371
    private static final int ITEM_SELECTION_INTERACTION_ACTION_ID = 89849878; // Action ID for selecting an item in the list

    // --- State ---
    private String selectedInteractionOption = OPT_CUT_GEMS; // Default to Cut Gems for testing structure
    private Product selectedProduct = null;
    private int selectedGroupId = -1; // Currently selected Category/Group Enum ID (e.g., 6983)

    // Group/Category related maps (used when selectedInteractionOption requires them, like "Cut Gems")
    private final Map<Integer, List<Product>> groupEnumIdToProducts;
    private final Map<Integer, String> groupEnumIdToName;
    private List<Integer> orderedGroupIds; // Maintains the order from the main group enum

    // Direct products list (used when an option doesn't have groups, or before Tan Leather fake group)
    private List<Product> directProducts;

    public PortableCrafter(CoaezUtility script) {
        super(script, PortableType.CRAFTER);
        this.groupEnumIdToProducts = new LinkedHashMap<>();
        this.groupEnumIdToName = new LinkedHashMap<>(); // Keep names ordered too
        this.orderedGroupIds = new ArrayList<>();
        this.directProducts = new ArrayList<>(); // Initialize the direct products list
        try {
            // Load data for the default option
            loadDataForOption(this.selectedInteractionOption);
            selectDefaultGroupAndProduct();
        } catch (Exception e) {
            ScriptConsole.println("[PortableCrafter] CRITICAL ERROR during constructor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Core Logic ---

    /**
     * Loads group and product data based on the selected top-level interaction option.
     * If the option maps to a group enum (like "Cut Gems" -> 6981), it loads subgroups and their products.
     * Otherwise, it might load products directly (not implemented yet) or clear group data.
     * @param option The selected interaction option (e.g., "Cut Gems").
     */
    private void loadDataForOption(String option) {
        // Clear previous data
        groupEnumIdToProducts.clear();
        groupEnumIdToName.clear();
        orderedGroupIds.clear();
        selectedGroupId = -1;
        selectedProduct = null;

        if (OPT_CUT_GEMS.equals(option)) {
            ScriptConsole.println("[PortableCrafter] Loading group data for 'Cut Gems' using Group Enum: " + GEM_GROUP_ENUM_ID);
            EnumType mainGroupEnum = ConfigManager.getEnumType(GEM_GROUP_ENUM_ID);
            if (mainGroupEnum == null || mainGroupEnum.getOutputs() == null) {
                ScriptConsole.println("[PortableCrafter] Failed to load or no outputs for main group enum: " + GEM_GROUP_ENUM_ID);
                return;
            }

            List<Object> subGroupEnumIds = mainGroupEnum.getOutputs();
            for (Object subIdObj : subGroupEnumIds) {
                if (!(subIdObj instanceof Integer)) continue;
                int subGroupId = (Integer) subIdObj;

                // Manually assign names based on known IDs
                String subGroupName;
                if (subGroupId == GEM_CUTTING_ENUM_ID) subGroupName = "Gem Cutting";
                else if (subGroupId == GEM_CRUSHING_ENUM_ID) subGroupName = "Gem Crushing";
                else if (subGroupId == BOLT_TIPS_ENUM_ID) subGroupName = "Bolt Tips";
                else subGroupName = "Unknown Group (" + subGroupId + ")";

                orderedGroupIds.add(subGroupId);
                groupEnumIdToName.put(subGroupId, subGroupName);

                // Load products for this sub-group
                EnumType subGroupEnum = ConfigManager.getEnumType(subGroupId);
                List<Product> productsInGroup = new ArrayList<>();
                if (subGroupEnum != null && subGroupEnum.getOutputs() != null) {
                    for (Object prodIdObj : subGroupEnum.getOutputs()) {
                        if (prodIdObj instanceof Integer) {
                            int productId = (Integer) prodIdObj;
                            ItemType itemType = ConfigManager.getItemType(productId);
                            if (itemType != null) {
                                // Assumes Product(ItemType) constructor exists
                                Product product = new Product(itemType);
                                product.parseIngredients();
                                productsInGroup.add(product);
                            }
                        }
                    }
                    // Sort products within the group
                    productsInGroup.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                } else {
                    ScriptConsole.println("[PortableCrafter] Failed to load or no outputs for sub-group enum: " + subGroupId + " ('" + subGroupName + "')");
                }
                groupEnumIdToProducts.put(subGroupId, productsInGroup);
                 ScriptConsole.println("[PortableCrafter] Loaded " + productsInGroup.size() + " products for group: " + subGroupName);
            }
        } else if (OPT_TAN_LEATHER.equals(option)) {
            ScriptConsole.println("[PortableCrafter] Loading 'Tan Leather' products using Enum: " + TAN_LEATHER_PRODUCTS_ENUM_ID + " (faking group)");
            int fakeGroupId = TAN_LEATHER_PRODUCTS_ENUM_ID; // Use the product enum ID as the fake group ID
            String fakeGroupName = "Tan Leather";

            orderedGroupIds.add(fakeGroupId);
            groupEnumIdToName.put(fakeGroupId, fakeGroupName);

            EnumType leatherProductsEnum = ConfigManager.getEnumType(TAN_LEATHER_PRODUCTS_ENUM_ID);
            List<Product> productsInGroup = new ArrayList<>();
            if (leatherProductsEnum != null && leatherProductsEnum.getOutputs() != null) {
                for (Object prodIdObj : leatherProductsEnum.getOutputs()) {
                    if (prodIdObj instanceof Integer) {
                        int productId = (Integer) prodIdObj;
                        ItemType itemType = ConfigManager.getItemType(productId);
                        if (itemType != null) {
                            Product product = new Product(itemType);
                            product.parseIngredients(); // Ensure ingredients are parsed
                            productsInGroup.add(product);
                        }
                    }
                }
                // Sort products within the group
                productsInGroup.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
            } else {
                ScriptConsole.println("[PortableCrafter] Failed to load or no outputs for Tan Leather product enum: " + TAN_LEATHER_PRODUCTS_ENUM_ID);
            }
            groupEnumIdToProducts.put(fakeGroupId, productsInGroup);
            ScriptConsole.println("[PortableCrafter] Loaded " + productsInGroup.size() + " products for fake group: " + fakeGroupName);

        } else {
            // Handle other options like "Craft", "Clay Crafting"
            // If they have their own direct product enums, load them here into a general list.
            // For now, just log and ensure group data is clear.
            ScriptConsole.println("[PortableCrafter] Option '" + option + "' does not currently load grouped products.");
        }
    }

    /**
     * Selects the first available group and the first product within that group as default.
     */
    private void selectDefaultGroupAndProduct() {
        if (!orderedGroupIds.isEmpty()) {
            selectedGroupId = orderedGroupIds.get(0); // Select first group by default
            List<Product> products = groupEnumIdToProducts.get(selectedGroupId);
            if (products != null && !products.isEmpty()) {
                selectedProduct = products.get(0); // Select first product in that group
            } else {
                selectedProduct = null;
            }
        } else {
            // No groups loaded (e.g., for non-"Cut Gems" options)
            selectedGroupId = -1;
            selectedProduct = null;
        }
         ScriptConsole.println("[PortableCrafter] Default selection - GroupID: " + selectedGroupId + ", Product: " + (selectedProduct != null ? selectedProduct.getName() : "None"));
    }


    // --- Setters / Getters for GUI ---

    public void setSelectedInteractionOption(String option) {
        if (!isValidOption(option)) {
            ScriptConsole.println("[PortableCrafter] Attempted to set invalid option: " + option);
            return;
        }
        if (!Objects.equals(this.selectedInteractionOption, option)) {
            this.selectedInteractionOption = option;
            ScriptConsole.println("[PortableCrafter] Interaction option set to: " + option);
            loadDataForOption(option);
            selectDefaultGroupAndProduct(); // Reset group/product selection
        }
    }

    public List<Integer> getGroupEnumIds() {
        return Collections.unmodifiableList(orderedGroupIds);
    }

    public String getGroupName(int groupId) {
        return groupEnumIdToName.getOrDefault(groupId, "Unknown Group (" + groupId + ")");
    }

    public List<Product> getProductsForGroup(int groupId) {
        return Collections.unmodifiableList(groupEnumIdToProducts.getOrDefault(groupId, Collections.emptyList()));
    }

    public void setSelectedGroupId(int groupId) {
        if (groupEnumIdToProducts.containsKey(groupId)) {
            if (this.selectedGroupId != groupId) {
                this.selectedGroupId = groupId;
                ScriptConsole.println("[PortableCrafter] Selected group changed to: " + getGroupName(groupId) + " (ID: " + groupId + ")");
                // Select the first product of the new group by default
                List<Product> products = groupEnumIdToProducts.get(groupId);
                if (products != null && !products.isEmpty()) {
                    setSelectedProduct(products.get(0));
                } else {
                    setSelectedProduct(null);
                }
            }
        } else {
             ScriptConsole.println("[PortableCrafter] Attempted to set invalid group ID: " + groupId);
        }
    }

    public int getSelectedGroupId() {
        return selectedGroupId;
    }

    public void setSelectedProduct(Product product) {
        // Basic assignment, could add validation to ensure product belongs to selectedGroupId if needed
        this.selectedProduct = product;
        if (product != null) {
            ScriptConsole.println("[PortableCrafter|setSelectedProduct] Product set to: " + product.getName() + " (ID: " + product.getId() + ")");
            List<Ingredient> ingredients = product.getIngredients();
            ScriptConsole.println("[PortableCrafter|setSelectedProduct] Ingredients status for " + product.getName() + ": " + (ingredients == null ? "null" : "Size: " + ingredients.size()));
        } else {
            ScriptConsole.println("[PortableCrafter|setSelectedProduct] Selected product cleared (set to null).");
        }
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    // --- Portable Overrides ---

    @Override
    public String getInteractionOption() {
        // Always interact with the top-level option selected by the user
        return this.selectedInteractionOption;
    }

    @Override
    public List<Ingredient> getRequiredItems() {
        // Return ingredients of the specific selected product, if any
        if (selectedProduct != null && selectedProduct.getIngredients() != null) {
            return selectedProduct.getIngredients();
        }
        return Collections.emptyList();
    }

    // --- Interface Handling Logic (Adapted from PortableTask) ---

    /**
     * Handles selecting the specific product within the crafter's Make-X interface (likely 1371).
     * Assumes the interface is already open.
     * @return true if selection was attempted (even if it fails), false if the interface is not open or no product is selected.
     */
    public boolean handleMakeXSelection() {
        ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Attempting to handle Make-X selection...");

        // Ensure interface 1371 is open
        if (!Interfaces.isOpen(CRAFTER_MAKE_X_INTERFACE_ID)) {
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Make-X interface (" + CRAFTER_MAKE_X_INTERFACE_ID + ") is not open.");
            return false;
        }

        // Ensure a product is selected in the GUI
        if (this.selectedProduct == null) {
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] No product is selected in the crafter GUI. Cannot select.");
            return false;
        }

        int targetProductId = this.selectedProduct.getId();
        ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Target product for selection is: " + this.selectedProduct.getName() + " (ID: " + targetProductId + ").");

        // Check if the target product is already selected in the interface using VARP 1170
        int selectedItemIdInInterface = VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_ITEM);
        ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Interface selected item ID (VARP 1170): " + selectedItemIdInInterface);

        if (selectedItemIdInInterface == targetProductId) {
            // Product is already selected, proceed to click the Make button (Component 30)
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Correct item already selected. Clicking Make button (CompIdx: " + COMPONENT_INDEX_MAKE_BUTTON + ")...");
            // Find the Make button component - Interface 1371, Component 30
            Component makeButton = ComponentQuery.newQuery(CRAFTER_MAKE_X_INTERFACE_ID)
                                                .componentIndex(COMPONENT_INDEX_MAKE_BUTTON)
                                                .results().first();

            if (makeButton != null) {
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Interacting with Make button (ActionID: " + MAKE_BUTTON_ACTION + ")...");
                 if (MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, MAKE_BUTTON_ACTION)) {
                    Execution.delay(script.getRandom().nextInt(1000) + 800); // Delay after clicking
                    ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Clicked Make button.");
                    return true; // Indicate selection/action attempted
                 } else {
                    ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Failed to interact with Make button.");
                    return false; // Indicate failure
                 }
            } else {
                ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Could not find Make button component (CompIdx: " + COMPONENT_INDEX_MAKE_BUTTON + ") in interface " + CRAFTER_MAKE_X_INTERFACE_ID + ".");
                return false; // Indicate failure
            }
        } else {
            // Incorrect item selected or no item selected. Find and click the correct product component by index calculation.
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Incorrect item selected in interface. Attempting to find and select " + this.selectedProduct.getName() + " (ID: " + targetProductId + ") by index calculation.");

            // Determine which Enum holds the products for the currently selected interaction option.
            // This logic depends on the selectedInteractionOption state.
            int productEnumId;
            if (OPT_CUT_GEMS.equals(selectedInteractionOption)) {
                // For Cut Gems, we need the products from the *selected subgroup* (e.g., Gem Cutting, Gem Crushing).
                // The selectedGroupId holds the Enum ID for the products of the selected category.
                 if(selectedGroupId == -1) {
                      ScriptConsole.println("[PortableCrafter|handleMakeXSelection] ERROR: Cut Gems selected, but no sub-group selected.");
                      return false; // Cannot proceed if no sub-group is selected
                 }
                 productEnumId = selectedGroupId; // Use the selected group ID as the product enum ID
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Using selected group Enum ID for products: " + productEnumId);

            } else if (OPT_TAN_LEATHER.equals(selectedInteractionOption)) {
                // For Tan Leather, the product enum ID is fixed.
                productEnumId = TAN_LEATHER_PRODUCTS_ENUM_ID;
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Using Tan Leather product Enum ID: " + productEnumId);
            } else {
                 // Handle other options if they have a single product enum list directly
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Selection logic not implemented for option: " + selectedInteractionOption);
                 return false; // Cannot select product if logic is not defined
            }

            EnumType productEnum = ConfigManager.getEnumType(productEnumId);
            if (productEnum == null || productEnum.getOutputs() == null) {
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] CRITICAL: Could not get product enum data or its outputs for enum ID: " + productEnumId + " for option " + selectedInteractionOption);
                 return false;
            }
            List<Object> itemIdsInEnum = productEnum.getOutputs();

            // Find the index of the target product within its Enum.
            int productIndexInEnum = -1;
            for (int i = 0; i < itemIdsInEnum.size(); i++) {
                if (itemIdsInEnum.get(i) instanceof Integer && ((Integer)itemIdsInEnum.get(i)).intValue() == targetProductId) {
                    productIndexInEnum = i;
                    break;
                }
            }

            if (productIndexInEnum == -1) {
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] CRITICAL: Target product ID " + targetProductId + " ("+this.selectedProduct.getName()+") not found in the product enum list (ID: " + productEnumId + ") for option " + selectedInteractionOption + ". Contents: " + itemIdsInEnum.toString());
                return false; // Product not found in the expected enum list
            }
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Target product " + this.selectedProduct.getName() + " found at index " + productIndexInEnum + " in enum " + productEnumId + ".");


            // Calculate the sub-component index based on the product's index in the enum.
            // Based on PortableTask's handling of 1371, the pattern is (index * 4) + 1.
            int subComponentIndexToSelect = (productIndexInEnum * 4) + 1;
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Calculated target SubComponentIndex based on enum index: " + subComponentIndexToSelect);

            // Interact with the main item list component (index 1 in 1371) using the calculated sub-index.
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Attempting item selection interaction: Interface=" + CRAFTER_MAKE_X_INTERFACE_ID +
                                  ", TargetComponentIndex=" + ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX +
                                  ", SubComponentIndexToUse=" + subComponentIndexToSelect +
                                  ", ActionID=" + ITEM_SELECTION_INTERACTION_ACTION_ID);

            if (MiniMenu.interact(ComponentAction.COMPONENT.getType(),
                                  ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX, // Main component index for the list
                                  subComponentIndexToSelect, // Calculated sub-component index for the item
                                  ITEM_SELECTION_INTERACTION_ACTION_ID)) { // Action ID for selecting an item
                ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Successfully initiated interaction to select item " + this.selectedProduct.getName());
                Execution.delay(script.getRandom().nextInt(600) + 400); // Delay after clicking
                // After selecting the item, the VARP (1170) should update on the next game tick.
                // The next execute cycle should see the correct item selected and click the Make button.
                return true; // Indicate action attempted
            } else {
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Failed to interact to select item using calculated SubComponentIndex. TargetCompIndex=" + ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX +
                                      ", CalculatedSubCompIndexToUse=" + subComponentIndexToSelect + ", ActionID=" + ITEM_SELECTION_INTERACTION_ACTION_ID);
                return false; // Indicate interaction failed
            }
        }
    }


    // --- Helper ---

    public boolean isValidOption(String option) {
        if (option == null) return false;
        for (String validOption : CRAFTER_OPTIONS) {
            if (option.equals(validOption)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the list of products for options that don't use groups (e.g., Tan Leather was originally direct).
     * This method is retained for potential future direct options, although Tan Leather currently uses a fake group.
     * @return An unmodifiable list of products, or an empty list if not applicable.
     */
    public List<Product> getDirectProducts() {
        // With Tan Leather faking a group, this might not be used unless a truly direct option is added.
        return Collections.unmodifiableList(directProducts != null ? directProducts : Collections.emptyList());
    }
} 