package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.script.ScriptConsole;

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

    // --- Interaction Options ---
    public static final String OPT_CUT_GEMS = "Cut Gems";
    public static final String OPT_CRAFT = "Craft"; // Generic
    public static final String OPT_CLAY = "Clay Crafting";
    // public static final String OPT_TAN_LEATHER = "Tan Leather"; // Temporarily removed

    public static final String[] CRAFTER_OPTIONS = {
            OPT_CUT_GEMS, OPT_CRAFT, OPT_CLAY // Temporarily removed Tan Leather
    };

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
             ScriptConsole.println("[PortableCrafter] Selected product set to: " + product.getName());
             // Automatically update selected group if product is set directly? Risky.
             // Better to rely on GUI setting group then product.
         } else {
             ScriptConsole.println("[PortableCrafter] Selected product cleared.");
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
            // TODO: Product needs to load ingredients correctly
            return selectedProduct.getIngredients();
        }
        return Collections.emptyList();
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
        // Ensure this.directProducts is initialized in the constructor if it might be null
        // For now, assuming it's initialized as new ArrayList<>()
        return Collections.unmodifiableList(directProducts != null ? directProducts : Collections.emptyList());
    }
} 