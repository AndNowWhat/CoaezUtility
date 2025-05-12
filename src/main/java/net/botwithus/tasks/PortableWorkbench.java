package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 * Represents a portable workbench, extending the base Portable class.
 * It handles loading and managing construction flatpack products and their groups.
 */
public class PortableWorkbench extends Portable {

    private Product selectedProduct;

    // Stores products categorized by their sub-group Enum ID
    private final Map<Integer, List<Product>> groupEnumIdToProducts;
    // Stores the names of the sub-groups, mapped by their Enum ID
    private final Map<Integer, String> groupEnumIdToName;

    // Enum ID for the main group of construction flatpack sub-groups
    public static final int FLATPACK_GROUP_ENUM_ID = 11345;
    // Enum ID for the names of the construction flatpack sub-groups
    private static final int FLATPACK_GROUP_NAMES_ENUM_ID = 11346;

    /**
     * Constructs a new PortableWorkbench.
     * Initializes product and group maps and loads available flatpacks.
     * @param script The main script instance.
     */
    public PortableWorkbench(CoaezUtility script) {
        super(script, PortableType.WORKBENCH);
        this.groupEnumIdToProducts = new LinkedHashMap<>();
        this.groupEnumIdToName = new HashMap<>();
        try {
            loadAvailableProductsAndGroups();
            selectDefaultProduct();
        } catch (Exception e) {
            ScriptConsole.println("[PortableWorkbench] CRITICAL ERROR during constructor initialization: " + e.getMessage());
            e.printStackTrace();
            this.selectedProduct = null;
        }
    }

    /**
     * Selects the first available product from the loaded groups as the default.
     * Logs a warning if no products are found.
     */
    private void selectDefaultProduct() {
        if (groupEnumIdToProducts.isEmpty()) {
            script.println("[PortableWorkbench] Warning: No product groups loaded, cannot select default product.");
            this.selectedProduct = null;
            return;
        }

        for (int groupId : groupEnumIdToProducts.keySet()) {
            List<Product> productsInGroup = groupEnumIdToProducts.get(groupId);
            if (productsInGroup != null && !productsInGroup.isEmpty()) {
                this.selectedProduct = productsInGroup.get(0);
                return;
            }
        }
        this.selectedProduct = null;
        script.println("[PortableWorkbench] Warning: No valid products found in any group after checking all loaded groups.");
    }


    /**
     * Loads available flatpack products and their group names from EnumTypes.
     * Populates the groupEnumIdToProducts and groupEnumIdToName maps.
     * Logs warnings for missing enums, names, or products.
     */
    private void loadAvailableProductsAndGroups() {
        if (FLATPACK_GROUP_ENUM_ID == -1 || FLATPACK_GROUP_NAMES_ENUM_ID == -1) {
            ScriptConsole.println("[PortableWorkbench] Error: FLATPACK_GROUP_ENUM_ID or FLATPACK_GROUP_NAMES_ENUM_ID is not properly set.");
            return;
        }

        EnumType groupEnum = ConfigManager.getEnumType(FLATPACK_GROUP_ENUM_ID);
        if (groupEnum == null) {
            ScriptConsole.println("[PortableWorkbench] Error: Failed to load Group EnumType with ID: " + FLATPACK_GROUP_ENUM_ID);
            return;
        }

        EnumType groupNamesEnum = ConfigManager.getEnumType(FLATPACK_GROUP_NAMES_ENUM_ID);
        if (groupNamesEnum == null) {
            ScriptConsole.println("[PortableWorkbench] Warning: Failed to load Group Names EnumType with ID: " + FLATPACK_GROUP_NAMES_ENUM_ID + ". Proceeding without explicit group names.");
        }

        List<Object> subGroupEnumIds = groupEnum.getOutputs();
        List<Object> groupNameOutputs = (groupNamesEnum != null) ? groupNamesEnum.getOutputs() : null;

        if (subGroupEnumIds == null || subGroupEnumIds.isEmpty()) {
            ScriptConsole.println("[PortableWorkbench] Warning: Group EnumType ID " + FLATPACK_GROUP_ENUM_ID + " has no output IDs (sub-group enum IDs). No products loaded.");
            return;
        }

        for (int i = 0; i < subGroupEnumIds.size(); i++) {
            Object subIdObj = subGroupEnumIds.get(i);

            if (!(subIdObj instanceof Integer)) {
                ScriptConsole.println("[PortableWorkbench] Warning: Skipping non-Integer value in Group Enum outputs at index " + i + ": " + (subIdObj != null ? subIdObj.getClass().getName() : "null"));
                continue;
            }
            int subGroupEnumId = (Integer) subIdObj;

            String subGroupName = "Group " + subGroupEnumId;
            if (groupNameOutputs != null && i < groupNameOutputs.size()) {
                Object nameObj = groupNameOutputs.get(i);
                if (nameObj instanceof String) {
                    subGroupName = (String) nameObj;
                } else {
                    ScriptConsole.println("[PortableWorkbench] Warning: Name at index " + i + " in Group Names Enum (" + FLATPACK_GROUP_NAMES_ENUM_ID + ") is not a String: " + (nameObj != null ? nameObj.getClass().getName() : "null"));
                }
            } else if (groupNameOutputs != null) {
                 ScriptConsole.println("[PortableWorkbench] Warning: Group Names Enum (" + FLATPACK_GROUP_NAMES_ENUM_ID + ") has fewer names than Group Enum has IDs. Mismatch for sub-group ID " + subGroupEnumId + " at index " + i);
            } 

            groupEnumIdToName.put(subGroupEnumId, subGroupName);

            EnumType subGroupEnum = ConfigManager.getEnumType(subGroupEnumId);
            if (subGroupEnum == null) {
                ScriptConsole.println("[PortableWorkbench] Warning: Failed to load Sub-Group EnumType with ID: " + subGroupEnumId + " (Name: " + subGroupName + "). Skipping products for this group.");
                groupEnumIdToProducts.put(subGroupEnumId, new ArrayList<>());
                continue;
            }

            List<Object> productIds = subGroupEnum.getOutputs();
            if (productIds == null || productIds.isEmpty()) {
                groupEnumIdToProducts.put(subGroupEnumId, new ArrayList<>());
                continue;
            }

            List<Product> productsInGroup = new ArrayList<>();
            for (Object obj : productIds) {
                if (obj instanceof Integer) {
                    int productId = (Integer) obj;
                    ItemType itemType = ConfigManager.getItemType(productId);
                    if (itemType != null) {
                        Product product = new Product(itemType);
                        product.parseIngredients();
                        productsInGroup.add(product);
                    } else {
                        ScriptConsole.println("[PortableWorkbench] Warning: Could not load ItemType for product ID from sub-enum " + subGroupEnumId + " (Name: " + subGroupName + "): " + productId);
                    }
                } else {
                    ScriptConsole.println("[PortableWorkbench] Warning: Skipping non-Integer value in Sub-Group Enum outputs (ID " + subGroupEnumId + ", Name: " + subGroupName + "): " + (obj != null ? obj.getClass().getName() : "null"));
                }
            }

            productsInGroup.sort(Comparator.comparing(Product::getName));
            groupEnumIdToProducts.put(subGroupEnumId, productsInGroup);
        }
    }

    // --- Methods for GUI Interaction ---

    /**
     * Gets the ordered list of Sub-Group Enum IDs that were processed.
     * The order reflects the sequence in the main Group Enum (FLATPACK_GROUP_ENUM_ID).
     * @return A new List containing the Integer Enum IDs.
     */
    public List<Integer> getGroupEnumIds() {
        return new ArrayList<>(this.groupEnumIdToProducts.keySet());
    }

    /**
     * Gets the display name for a given Sub-Group Enum ID.
     * @param groupId The Enum ID of the sub-group.
     * @return The name (e.g., "Parlour Flatpacks") or a default string if not found or if the name map is invalid.
     */
    public String getGroupName(int groupId) {
        if (this.groupEnumIdToName == null) {
            return "Error: Name map null";
        }
        return this.groupEnumIdToName.getOrDefault(groupId, "Unknown Group (" + groupId + ")");
    }

    /**
     * Gets an unmodifiable list of products associated with a specific Sub-Group Enum ID.
     * The list is sorted alphabetically by product name.
     * @param groupId The Enum ID of the sub-group.
     * @return An unmodifiable list of Product objects, or an empty list if the group ID is invalid, has no products, or the product map is invalid.
     */
    public List<Product> getProductsForGroup(int groupId) {
        if (this.groupEnumIdToProducts == null) {
             return Collections.emptyList();
        }
        List<Product> products = this.groupEnumIdToProducts.getOrDefault(groupId, Collections.emptyList());
        return Collections.unmodifiableList(products);
    }

    // --- Product Selection --- 

    /**
     * Sets the product the workbench should currently be configured to make.
     * Ensures the provided product is valid and exists within the loaded groups.
     * Logs a warning if an invalid product is attempted.
     * @param product The Product to select, or null to clear selection.
     */
    public void setSelectedProduct(Product product) {
        String productName = (product != null && product.getName() != null) ? product.getName() : "null";
        int productId = (product != null) ? product.getId() : -1;

        boolean productIsValid = false;
        if (product == null) { // Clearing selection is valid
            productIsValid = true;
        } else if (this.groupEnumIdToProducts != null) { // Check if product exists in any group
            for (List<Product> groupProducts : groupEnumIdToProducts.values()) {
                if (groupProducts != null && groupProducts.stream().anyMatch(p -> p != null && p.getId() == product.getId())) {
                    productIsValid = true;
                    break;
                }
            }
        }

        if (productIsValid) {
             this.selectedProduct = product;
        } else {
             ScriptConsole.println("[PortableWorkbench] Warning: Attempted to set an invalid or unloaded product: " + productName + " (ID: " + productId + ")");
        }
    }

    /**
     * Gets the currently selected product for the workbench.
     * @return The selected Product, or null if none is selected.
     */
    public Product getSelectedProduct() {
        return selectedProduct;
    }

    /**
     * Gets an unmodifiable list of all available products across all groups, sorted alphabetically.
     * @return An unmodifiable list of all Product objects.
     */
    public List<Product> getAvailableProducts() {
        List<Product> allProducts = new ArrayList<>();
        if (this.groupEnumIdToProducts == null) return Collections.unmodifiableList(allProducts);

        for(List<Product> groupList : groupEnumIdToProducts.values()) {
            if (groupList != null) {
                 allProducts.addAll(groupList);
            }
        }
        // Sort considering null products or names just in case, though unlikely with current loading logic
        allProducts.sort(Comparator.comparing(p -> (p != null && p.getName() != null) ? p.getName() : ""));
        return Collections.unmodifiableList(allProducts);
    }

    // --- Portable Overrides --- 

    /**
     * Returns the specific interaction option for the Portable Workbench.
     * @return The interaction string "Make Flatpacks".
     */
    @Override
    public String getInteractionOption() {
        return "Make Flatpacks";
    }

    /**
     * Gets the required ingredients for the currently selected product.
     * @return A list of Ingredient objects for the selected product, or an empty list if no product is selected.
     */
    @Override
    public List<Ingredient> getRequiredItems() {
        if (selectedProduct != null) {
            return selectedProduct.getIngredients(); // Assumes Product class handles ingredient loading
        } else {
             //ScriptConsole.println("[PortableWorkbench] No product selected, cannot determine required items.");
            return Collections.emptyList();
        }
    }
} 