package net.botwithus.tasks;

import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Product {
    private final ItemType productItemType;
    private final List<Ingredient> ingredients;

    private static final int PRIMARY_INGREDIENT_PARAM_ID = 211;
    private static final int PRIMARY_INGREDIENT_AMOUNT_PARAM_ID = 212;
    private static final int LEVEL_REQUIREMENT_PARAM_ID = 23;
    
    // CS2 Script Parameter IDs for Crafting
    private static final int CS2_INGREDIENT_ID_PARAM = 2655;
    private static final int CS2_INGREDIENT_QUANTITY_PARAM = 2665;

    // Assuming category 206 is for flatpacks based on example
    public static final int FLATPACK_CATEGORY_ID = 206; 

    public Product(ItemType productItemType) {
        this.productItemType = productItemType;
        this.ingredients = parseIngredients();
    }

    private List<Ingredient> parseIngredients() {
        List<Ingredient> parsedIngredients = new ArrayList<>();
        if (productItemType == null) { 
            return parsedIngredients;
        }
        
        // Try parsing based on item params (like Mahogany toy box example)
        // Param 211 = Item ID, Param 212 = Amount
        // Use getIntParam and handle potential absence of the param
        int paramIngredientId = productItemType.getIntParam(PRIMARY_INGREDIENT_PARAM_ID); // Returns 0 if not present
        int paramIngredientAmount = productItemType.getIntParam(PRIMARY_INGREDIENT_AMOUNT_PARAM_ID); // Returns 0 if not present

        boolean foundViaParams = false;
        // Check if valid values were likely found (param might exist but be 0)
        // A more robust check might involve querying param *existence* if API allows
        if (paramIngredientId > 0 && paramIngredientAmount > 0) { 
            ItemType ingredientType = ConfigManager.getItemType(paramIngredientId);
            if (ingredientType != null) {
                ScriptConsole.println("[Product: " + getName() + "] Found ingredient via Item Params: " + ingredientType.getName() + " x" + paramIngredientAmount);
                parsedIngredients.add(new Ingredient(ingredientType, paramIngredientAmount));
                foundViaParams = true;
            } else {
                ScriptConsole.println("[Product: " + getName() + "] Warning: Found ingredient ID (" + paramIngredientId + ") in param " + PRIMARY_INGREDIENT_PARAM_ID + " but failed to get ItemType.");
            }
        }
        
        // If not found via standard params, try CS2 params (used by flatpacks)
        if (!foundViaParams) {
            // Try getting CS2 params using getIntParam, as they store integer values
            int cs2IngredientId = productItemType.getIntParam(CS2_INGREDIENT_ID_PARAM); // Returns 0 if not present
            int cs2IngredientAmount = productItemType.getIntParam(CS2_INGREDIENT_QUANTITY_PARAM); // Returns 0 if not present
            
            if (cs2IngredientId > 0 && cs2IngredientAmount > 0) {
                 ItemType ingredientType = ConfigManager.getItemType(cs2IngredientId);
                 if (ingredientType != null) {
                     ScriptConsole.println("[Product: " + getName() + "] Found ingredient via CS2 Params (getIntParam): " + ingredientType.getName() + " x" + cs2IngredientAmount);
                     parsedIngredients.add(new Ingredient(ingredientType, cs2IngredientAmount));
                 } else {
                     ScriptConsole.println("[Product: " + getName() + "] Warning: Found ingredient ID (" + cs2IngredientId + ") in CS2 param " + CS2_INGREDIENT_ID_PARAM + " but failed to get ItemType.");
                 }
            } else {
                // Only log this if neither method worked
                 ScriptConsole.println("[Product: " + getName() + "] No ingredient data found using Item Params or CS2 Params (getIntParam ID=" + cs2IngredientId +", Amount=" + cs2IngredientAmount + ").");
            }
        }
        
        // Add secondary/tertiary/etc ingredient parsing if necessary

        return parsedIngredients;
    }

    public String getName() {
        return productItemType != null ? productItemType.getName() : "Unknown Product";
    }

    public int getId() {
        return productItemType != null ? productItemType.getId() : -1;
    }

    public ItemType getProductItemType() {
        return productItemType;
    }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public int getLevelRequirement() {
        // Using param 23 based on example JSON
        // Returns 0 if not present, default to 1 in that case.
        int level = productItemType != null ? productItemType.getIntParam(LEVEL_REQUIREMENT_PARAM_ID) : 0;
        return level > 0 ? level : 1;
    }

    /* // Removing the dynamic discovery method again as there's no known API 
       // method to get all items or items by category.
       // The approach using a predefined list of product IDs in PortableWorkbench must be used.
    public static List<Product> getAllWorkbenchProducts() {
        // ... method content removed ...
    }
    */

} 