package net.botwithus.tasks;

import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Product {
    private int id; // Item ID of the product
    private String name;
    private ItemType productItemType; // Store the ItemType for param access
    private List<Ingredient> ingredients = new ArrayList<>();
    private int outputQuantity; // How many items this product definition yields

    // CS2 Script Parameter IDs for ingredients (example, might vary per item)
    // Common for things like flatpacks
    private static final int CS2_INGREDIENT_ID_PARAM = 2655; 
    private static final int CS2_INGREDIENT_QUANTITY_PARAM = 2665;

    // General ItemType parameters (example from Mahogany Toy Box)
    private static final int PRIMARY_INGREDIENT_PARAM_ID = 211;
    private static final int PRIMARY_INGREDIENT_AMOUNT_PARAM_ID = 212;
    

    // Constructor taking an ItemType, typically for workbench products
    public Product(ItemType itemType) {
        if (itemType != null) {
            this.id = itemType.getId();
            this.name = itemType.getName();
            this.productItemType = itemType; // Store for param access
            this.outputQuantity = 1; // Default, can be overridden if product def implies otherwise
            // Do NOT call parseIngredients here, as it might be called selectively later
        } else {
            this.id = -1;
            this.name = "Invalid Product (null type)";
            this.outputQuantity = 0;
        }
    }

    // Constructor for specific item ID and output quantity (e.g., from Enum where quantity is known)
    public Product(int itemId, int outputQuantity) {
        this.id = itemId;
        this.outputQuantity = outputQuantity;
        this.productItemType = ConfigManager.getItemType(itemId);
        if (this.productItemType != null) {
            this.name = this.productItemType.getName();
        } else {
            this.name = "Unknown Item (ID: " + itemId + ")";
            ScriptConsole.println("[Product] Failed to get ItemType for ID: " + itemId);
        }
        // Do NOT call parseIngredients here
    }

    // Static factory method, preferred way to create if you only have item ID
    public static Product fromItemId(int itemId, int outputQuantity) {
        return new Product(itemId, outputQuantity);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getOutputQuantity() {
        return outputQuantity;
    }
    
    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    // Method to parse ingredients based on CS2 params (2655 for item ID, 2665 for quantity)
    // Made public to be callable from PortableCrafter after Product instantiation
    public void parseIngredients() { 
        if (this.productItemType == null) {
            ScriptConsole.println("[Product: " + (this.getName() != null ? this.getName() : "Unknown") + "] Cannot parse ingredients, productItemType is null.");
            return;
        }
        
        this.ingredients.clear(); // Clear any existing ingredients before parsing

        // Try parsing based on item params (like Mahogany toy box example)
        // Param 211 = Item ID, Param 212 = Amount
        int paramIngredientId = this.productItemType.getIntParam(PRIMARY_INGREDIENT_PARAM_ID); 
        int paramIngredientAmount = this.productItemType.getIntParam(PRIMARY_INGREDIENT_AMOUNT_PARAM_ID);
 
        boolean foundViaParams = false;

        
        if (paramIngredientId > 0 && paramIngredientAmount > 0) { 
            ItemType ingredientType = ConfigManager.getItemType(paramIngredientId);
            if (ingredientType != null) {
                ScriptConsole.println("[Product: " + this.getName() + "] Found ingredient via Item Params: " + ingredientType.getName() + " x" + paramIngredientAmount);
                this.ingredients.add(new Ingredient(ingredientType, paramIngredientAmount));
                foundViaParams = true;
            } else {
                ScriptConsole.println("[Product: " + this.getName() + "] Warning: Found ingredient ID (" + paramIngredientId + ") in param " + PRIMARY_INGREDIENT_PARAM_ID + " but failed to get ItemType.");
            }
        }
        
        // If not found via standard params, try CS2 params (used by flatpacks/tanning)
        if (!foundViaParams) {
            ScriptConsole.println("[Product: " + this.getName() + "] Attempting CS2 Param check (IDs " + CS2_INGREDIENT_ID_PARAM + ", " + CS2_INGREDIENT_QUANTITY_PARAM + ")");
            int cs2IngredientId = this.productItemType.getIntParam(CS2_INGREDIENT_ID_PARAM); // 2655
            int cs2IngredientAmount = this.productItemType.getIntParam(CS2_INGREDIENT_QUANTITY_PARAM); // 2665
            // ... logs results ...
            if (cs2IngredientId > 0 && cs2IngredientAmount > 0) {
                 ItemType ingredientType = ConfigManager.getItemType(cs2IngredientId); // Should get Uncut opal (1625)
                 if (ingredientType != null) {
                     this.ingredients.add(new Ingredient(ingredientType, cs2IngredientAmount)); // Add Uncut opal x 1
                 } // ...
             }
        }
        
        // Fallback: If NO ingredients found via any param method, for some items, the product itself might be considered the input (e.g. if it's a transformation)
        // This was a previous broad fallback, but for tanning, the CS2 params *should* work.
        // If ingredients list is still empty, it means parsing failed or params were absent/zero.
        if (this.ingredients.isEmpty()) {
             ScriptConsole.println("[Product: " + this.getName() + "] No specific ingredients found via param checks.");
        }
    }
} 