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
import java.util.concurrent.Callable;

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
    private static final int CLAY_R_GROUP_ENUM_ID = 7008;   // For (r) items, "Fire Clay" path
    private static final int CLAY_UNF_GROUP_ENUM_ID = 7004; // For (unf) items, "Form Clay" path

    // --- Interaction Options ---
    public static final String OPT_CUT_GEMS = "Cut Gems";
    public static final String OPT_CRAFT = "Craft"; // Generic
    public static final String OPT_TAN_LEATHER = "Tan Leather";
    public static final String OPT_CLAY_FIRE = "Clay Crafting (Fire)"; // For (r) items
    public static final String OPT_CLAY_FORM = "Clay Crafting (Form)"; // For (unf) items

    public static final String[] CRAFTER_OPTIONS = {
            OPT_CUT_GEMS, OPT_CRAFT, OPT_TAN_LEATHER, OPT_CLAY_FIRE, OPT_CLAY_FORM
    };

    // --- Interface Constants (Copied/Adapted from PortableTask) ---
    public static final int CRAFTER_MAKE_X_INTERFACE_ID = 1371; // Interface for product selection
    private static final int VARP_ID_SELECTED_ITEM = 1170; // VARP that tracks the selected item ID in 1371
    private static final int COMPONENT_INDEX_MAKE_BUTTON = 30; // Index of the 'Make' button in 1371
    private static final int MAKE_BUTTON_ACTION = 89784350; // Action ID for the 'Make' button
    private static final int ITEM_SELECTION_INTERACTION_TARGET_COMPONENT_INDEX = 1; // Main component index for item list in 1371
    private static final int ITEM_SELECTION_INTERACTION_ACTION_ID = 89849878; // Action ID for selecting an item in the list

    // Category Selection Constants (from PortableTask)
    private static final int VARP_ID_SELECTED_CATEGORY_ENUM = 1169;
    private static final int COMPONENT_INDEX_GROUP_DROPDOWN_TRIGGER = 1;
    private static final int COMPONENT_INDEX_GROUP_DROPDOWN_LIST = 2;
    private static final int GROUP_DROPDOWN_TRIGGER_ACTION = 89849884;
    private static final int GROUP_SELECT_ACTION = 96797586;

    // --- State ---
    private String selectedInteractionOption = OPT_CUT_GEMS; // Default to Cut Gems for testing structure
    private Product selectedProduct = null;
    private int selectedGroupId = -1; // Currently selected Category/Group Enum ID (e.g., 6983)

    // Store the actual interaction option for the object if different from GUI choice (e.g. for Clay)
    private String objectInteractionOptionOverride = null; 

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

        } else if (OPT_CLAY_FIRE.equals(option)) {
            ScriptConsole.println("[PortableCrafter] Loading group data for 'Clay Crafting (Fire)' using Group Enum: " + CLAY_R_GROUP_ENUM_ID);
            EnumType mainGroupEnum = ConfigManager.getEnumType(CLAY_R_GROUP_ENUM_ID);
            if (mainGroupEnum == null || mainGroupEnum.getOutputs() == null) {
                ScriptConsole.println("[PortableCrafter] Failed to load or no outputs for main clay (fire) group enum: " + CLAY_R_GROUP_ENUM_ID);
                return;
            }

            List<Object> subGroupEnumIds = mainGroupEnum.getOutputs();
            for (Object subIdObj : subGroupEnumIds) {
                if (!(subIdObj instanceof Integer)) continue;
                int subGroupId = (Integer) subIdObj;

                EnumType subGroupEnum = ConfigManager.getEnumType(subGroupId);
                if (subGroupEnum == null) {
                     ScriptConsole.println("[PortableCrafter] Failed to load sub-group enum: " + subGroupId + " for Clay Crafting (Fire).");
                     continue;
                }

                String subGroupName;
                switch (subGroupId) { // Names for (r) items - SAME AS (unf) NAMES
                    case 7018: subGroupName = "Cooking Urns"; break;
                    case 12910: subGroupName = "Divination Urns"; break;
                    case 12913: subGroupName = "Farming Urns"; break;
                    case 7021: subGroupName = "Fishing Urns"; break;
                    case 12916: subGroupName = "Hunter Urns"; break;
                    case 7024: subGroupName = "Mining Urns"; break;
                    case 7027: subGroupName = "Prayer Urns"; break;
                    case 12919: subGroupName = "Runecrafting Urns"; break;
                    case 7030: subGroupName = "Smelting Urns"; break;
                    case 7033: subGroupName = "Woodcutting Urns"; break;
                    default: subGroupName = "Unknown Urn Group (" + subGroupId + ")"; break; // Remove (R)
                }

                orderedGroupIds.add(subGroupId);
                groupEnumIdToName.put(subGroupId, subGroupName);

                List<Product> productsInGroup = new ArrayList<>();
                if (subGroupEnum.getOutputs() != null) {
                    for (Object prodIdObj : subGroupEnum.getOutputs()) {
                        if (prodIdObj instanceof Integer) {
                            int productId = (Integer) prodIdObj;
                            ItemType itemType = ConfigManager.getItemType(productId);
                            if (itemType != null) {
                                Product product = new Product(itemType);
                                product.parseIngredients();
                                productsInGroup.add(product);
                            }
                        }
                    }
                    productsInGroup.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                } else {
                    ScriptConsole.println("[PortableCrafter] No outputs for Clay (Fire) sub-group: " + subGroupId);
                }
                groupEnumIdToProducts.put(subGroupId, productsInGroup);
                ScriptConsole.println("[PortableCrafter] Loaded " + productsInGroup.size() + " products for clay (fire) group: " + subGroupName);
            }
        } else if (OPT_CLAY_FORM.equals(option)) {
            ScriptConsole.println("[PortableCrafter] Loading group data for 'Clay Crafting (Form)' using Group Enum: " + CLAY_UNF_GROUP_ENUM_ID);
            EnumType mainGroupEnum = ConfigManager.getEnumType(CLAY_UNF_GROUP_ENUM_ID);
            if (mainGroupEnum == null || mainGroupEnum.getOutputs() == null) {
                ScriptConsole.println("[PortableCrafter] Failed to load or no outputs for main clay (form) group enum: " + CLAY_UNF_GROUP_ENUM_ID);
                return;
            }

            List<Object> subGroupEnumIds = mainGroupEnum.getOutputs();
            for (Object subIdObj : subGroupEnumIds) {
                if (!(subIdObj instanceof Integer)) continue;
                int subGroupId = (Integer) subIdObj;

                EnumType subGroupEnum = ConfigManager.getEnumType(subGroupId);
                if (subGroupEnum == null) {
                     ScriptConsole.println("[PortableCrafter] Failed to load sub-group enum: " + subGroupId + " for Clay Crafting (Form).");
                     continue;
                }
                
                String subGroupName;
                // Manual name mapping for (unf) groups from enum 7004 - SAME AS (r) NAMES
                switch (subGroupId) {
                    case 7014: subGroupName = "Pottery"; break; // Changed from Pottery Items (unf)
                    case 7016: subGroupName = "Cooking Urns"; break;
                    case 12908: subGroupName = "Divination Urns"; break;
                    case 12911: subGroupName = "Farming Urns"; break;
                    case 7019: subGroupName = "Fishing Urns"; break;
                    case 12914: subGroupName = "Hunter Urns"; break;
                    case 7022: subGroupName = "Mining Urns"; break;
                    case 7025: subGroupName = "Prayer Urns"; break;
                    case 12917: subGroupName = "Runecrafting Urns"; break;
                    case 7028: subGroupName = "Smelting Urns"; break;
                    case 7031: subGroupName = "Woodcutting Urns"; break;
                    default: subGroupName = "Unknown Urn Group (" + subGroupId + ")"; break; // Remove (UNF)
                }

                orderedGroupIds.add(subGroupId);
                groupEnumIdToName.put(subGroupId, subGroupName);
                List<Product> productsInGroup = new ArrayList<>();
                if (subGroupEnum.getOutputs() != null) {
                    for (Object prodIdObj : subGroupEnum.getOutputs()) {
                        if (prodIdObj instanceof Integer) {
                            int productId = (Integer) prodIdObj;
                            ItemType itemType = ConfigManager.getItemType(productId);
                            if (itemType != null) {
                                Product product = new Product(itemType);
                                product.parseIngredients();
                                productsInGroup.add(product);
                            }
                        }
                    }
                    productsInGroup.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                } else {
                     ScriptConsole.println("[PortableCrafter] No outputs for Clay (Form) sub-group: " + subGroupId);
                }
                groupEnumIdToProducts.put(subGroupId, productsInGroup);
                ScriptConsole.println("[PortableCrafter] Loaded " + productsInGroup.size() + " products for clay (form) group: " + subGroupName);
            }
        } else {
            // Handle other options like "Craft"
            // If they have their own direct product enums, load them here into a general list.
            // For now, just log and ensure group data is clear.
            ScriptConsole.println("[PortableCrafter] Option '" + option + "' does not currently load grouped products.");
        }

        // Set the object interaction override if a clay option is chosen
        if (OPT_CLAY_FORM.equals(option) || OPT_CLAY_FIRE.equals(option)) {
            this.objectInteractionOptionOverride = "Clay Crafting"; // Set to the actual initial interaction option
        } else {
            this.objectInteractionOptionOverride = null; // Clear override for other options
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
        // If an override is set (e.g., for Clay's initial generic interaction),
        // return that. Otherwise, return the specific option selected in the GUI.
        if (this.objectInteractionOptionOverride != null && 
            (OPT_CLAY_FORM.equals(this.selectedInteractionOption) || OPT_CLAY_FIRE.equals(this.selectedInteractionOption))) {
            return this.objectInteractionOptionOverride;
        }
        return this.selectedInteractionOption; // For Cut Gems, Tan Leather, etc.
    }

    /**
     * Gets the specific interaction option that was selected in the GUI,
     * which might be more granular than the initial object interaction option.
     * Useful for deciding sub-dialog choices, like "Form Clay" vs "Fire Clay".
     * @return The GUI-selected interaction option string.
     */
    public String getSelectedGuiInteractionOption() {
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

        // --- Category Selection Logic --- START ---
        int currentCategoryEnumId = VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_CATEGORY_ENUM);
        int targetCategoryEnumId = this.selectedGroupId; // The group ID we *want* selected

        ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Category Check: Target Group ID = " + targetCategoryEnumId + ", Interface Group ID = " + currentCategoryEnumId);

        if (currentCategoryEnumId != targetCategoryEnumId) {
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Incorrect category selected. Finding and selecting target category: " + getGroupName(targetCategoryEnumId));

            int mainGroupEnumId = -1; // Initialize to a non-valid ID
            if (OPT_CUT_GEMS.equals(selectedInteractionOption)) {
                mainGroupEnumId = GEM_GROUP_ENUM_ID;
            } else if (OPT_CLAY_FIRE.equals(selectedInteractionOption)) {
                mainGroupEnumId = CLAY_R_GROUP_ENUM_ID;
            } else if (OPT_CLAY_FORM.equals(selectedInteractionOption)) {
                mainGroupEnumId = CLAY_UNF_GROUP_ENUM_ID;
            } else if (OPT_TAN_LEATHER.equals(selectedInteractionOption)) {
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Tan Leather selected, skipping category selection logic as it uses a single fake group.");
                 // For Tan Leather, the category is implicitly correct, so we act as if it matched.
                 // No need to proceed with mainGroupEnumId logic for category switching.
            } else {
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Category selection logic not implemented for option: " + selectedInteractionOption);
                 return false; 
            }

            // Only proceed with category switching if a valid mainGroupEnumId was set 
            // AND the category is actually different (this second check handles Tan Leather implicitly)
            if (mainGroupEnumId != -1 && currentCategoryEnumId != targetCategoryEnumId) { 
                 EnumType mainGroupEnum = ConfigManager.getEnumType(mainGroupEnumId);
                 if (mainGroupEnum == null || mainGroupEnum.getOutputs() == null) {
                      ScriptConsole.println("[PortableCrafter|handleMakeXSelection] CRITICAL: Could not load main group enum (ID: " + mainGroupEnumId + ") to find category index.");
                      return false;
                 }
                 List<Object> subGroupEnumIds = mainGroupEnum.getOutputs();

                 int targetCategoryIndex = -1;
                 for (int i = 0; i < subGroupEnumIds.size(); i++) {
                     if (subGroupEnumIds.get(i) instanceof Integer && ((Integer) subGroupEnumIds.get(i)).intValue() == targetCategoryEnumId) {
                         targetCategoryIndex = i;
                         break;
                     }
                 }

                 if (targetCategoryIndex != -1) {
                     ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Found target category at index " + targetCategoryIndex + " in main group enum " + mainGroupEnumId + ". Opening dropdown...");
                     if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), COMPONENT_INDEX_GROUP_DROPDOWN_TRIGGER, -1, GROUP_DROPDOWN_TRIGGER_ACTION)) {
                         Execution.delay(script.getRandom().nextInt(400) + 300); 
                         int categoryListSubIndex = (targetCategoryIndex * 2) + 1;
                         ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Selecting category from list (CompIdx: " + COMPONENT_INDEX_GROUP_DROPDOWN_LIST + ", SubIndex: " + categoryListSubIndex + ")...");
                         if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), COMPONENT_INDEX_GROUP_DROPDOWN_LIST, categoryListSubIndex, GROUP_SELECT_ACTION)) {
                             Execution.delay(script.getRandom().nextInt(600) + 400); // Initial delay after click
                            
                             // Wait for the category VARP to update
                             ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Waiting for category VARP (" + VARP_ID_SELECTED_CATEGORY_ENUM + ") to update to target ID: " + targetCategoryEnumId);
                             Callable<Boolean> categoryUpdated = () -> VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_CATEGORY_ENUM) == targetCategoryEnumId;
                             if (Execution.delayUntil(3000, categoryUpdated)) {
                                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Category VARP updated successfully. Ending cycle.");
                                 return true; // VARP updated, let next cycle handle item selection
                             } else {
                                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Timeout waiting for category VARP to update to " + targetCategoryEnumId + ". Current value: " + VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_CATEGORY_ENUM));
                                 return false; // Indicate failure
                             }
                         } else {
                             ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Failed to interact with category selection list (Action: " + GROUP_SELECT_ACTION + ").");
                             return false;
                         }
                     } else {
                          ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Failed to interact with category dropdown trigger (Action: " + GROUP_DROPDOWN_TRIGGER_ACTION + ").");
                         return false;
                     }
                 } else {
                     ScriptConsole.println("[PortableCrafter|handleMakeXSelection] CRITICAL: Could not find the target category enum ID " + targetCategoryEnumId + " within the main group enum " + mainGroupEnumId + ".");
                     return false;
                 }
             }
        } // --- Category Selection Logic --- END ---

        // If we reach here, the category *should* be correct.
        ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Correct category assumed/selected. Proceeding with item check/selection.");

        // Check if the target product is already selected in the interface using VARP 1170
        int selectedItemIdInInterface = VarManager.getVarValue(VarDomainType.PLAYER, VARP_ID_SELECTED_ITEM);
        ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Interface selected item ID (VARP 1170): " + selectedItemIdInInterface);

        if (selectedItemIdInInterface == targetProductId) {
            // Product is already selected, proceed to click the Make button (Component 30)
            ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Correct item already selected. Clicking Make button (CompIdx: " + COMPONENT_INDEX_MAKE_BUTTON + ")...");
            // Find the Make button component - Interface 1371, Component 30
            Component makeButton = ComponentQuery.newQuery(1370)
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
            } else if (OPT_CLAY_FIRE.equals(selectedInteractionOption) || OPT_CLAY_FORM.equals(selectedInteractionOption)) {
                 // For Clay Crafting (Fire or Form), use the selected subgroup Enum ID.
                 if(selectedGroupId == -1) {
                      ScriptConsole.println("[PortableCrafter|handleMakeXSelection] ERROR: Clay Crafting selected, but no sub-group selected.");
                      return false; 
                 }
                 productEnumId = selectedGroupId; 
                 ScriptConsole.println("[PortableCrafter|handleMakeXSelection] Using selected Clay Crafting (" + selectedInteractionOption + ") group Enum ID for products: " + productEnumId);
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
                                  ITEM_SELECTION_INTERACTION_ACTION_ID)) {
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

    /**
     * Helper method to infer a user-friendly group name from an enum name string.
     * Example: "recipe_products_cooking_urns_3" -> "Cooking Urns"
     * @param enumName The raw enum name.
     * @return A formatted group name or the original name if formatting fails.
     */
    // Removed inferGroupNameFromEnumName as it's not currently used due to lack of EnumType.getName()
    /*
    private String inferGroupNameFromEnumName(String enumName) {
        if (enumName == null || !enumName.startsWith("recipe_products_")) {
            return enumName != null ? enumName : "Unknown Enum Name"; // Return original or default
        }
        try {
            // Remove prefix and potential suffix (_#)
            String baseName = enumName.substring("recipe_products_".length());
            baseName = baseName.replaceAll("_\\d+$", ""); // Remove trailing _number if present

            // Split by underscore, capitalize words, join with spaces
            String[] parts = baseName.split("_");
            StringBuilder formattedName = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) continue;
                formattedName.append(Character.toUpperCase(part.charAt(0)))
                             .append(part.substring(1).toLowerCase())
                             .append(" ");
            }
            return formattedName.toString().trim(); // Return the formatted string
        } catch (Exception e) {
            ScriptConsole.println("[PortableCrafter] Error inferring group name from '" + enumName + "': " + e.getMessage());
            return enumName; // Fallback to original name on error
        }
    }
    */
} 