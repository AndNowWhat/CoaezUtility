package net.botwithus.tasks;

import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.StructType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SmithingRecipeInspector is a utility class dedicated to inspecting and extracting
 * recipe information (primarily ingredients) for smithing products.
 * It identifies the recipe StructType associated with a product and parses it.
 */
public class SmithingRecipeInspector {

    // Common ItemType parameters that might hold the ID of the Recipe StructType.
    // These are checked in order.
    private static final int[] RECIPE_STRUCT_ID_PARAMS_ON_PRODUCT = {
            2675, // Primary common param for smithing recipe structs
            2645  // Secondary common param
            // Add more known params here if discovered
    };

    // Common StructType parameters for ingredients within a recipe StructType.
    // These arrays must be ordered: INGREDIENT_ITEM_ID_PARAMS[i] corresponds to INGREDIENT_QUANTITY_PARAMS[i].
    private static final int[] RECIPE_STRUCT_INGREDIENT_ID_KEYS = {
            2656, 2657, 2658, 2659, 2660, 2661, 2662, 2663
            // (e.g., Bar ID, Flux ID, etc.)
    };
    private static final int[] RECIPE_STRUCT_INGREDIENT_QUANTITY_KEYS = {
            2665, 2667, 2668, 2669, 2670, 2671, 2672, 2673
            // (e.g., Bar Quantity, Flux Quantity, etc.)
    };

    /**
     * Retrieves the list of ingredients required to craft the given product.
     *
     * @param productId The item ID of the smithing product.
     * @return A List of {@link Ingredient} objects required for the product.
     *         Returns an empty list if no recipe or ingredients are found.
     */
    public static List<Ingredient> getIngredientsForProduct(int productId) {
        List<Ingredient> ingredients = new ArrayList<>();

        int recipeStructId = getRecipeStructIdForItem(productId);

        if (recipeStructId <= 0) {
            // ScriptConsole.println("[SmithingRecipeInspector] No recipe StructType ID found for product ID: " + productId);
            return ingredients; // Return empty list
        }

        StructType recipeStruct = ConfigManager.getStructType(recipeStructId);
        if (recipeStruct == null) {
            ScriptConsole.println("[SmithingRecipeInspector] Failed to load StructType with ID: " + recipeStructId + " for product ID: " + productId);
            return ingredients; // Return empty list
        }

        // ScriptConsole.println("[SmithingRecipeInspector] Product ID: " + productId + " uses Recipe Struct ID: " + recipeStructId + ". Parsing...");
        return parseIngredientsFromRecipeStruct(recipeStruct);
    }

    /**
     * Finds the ID of the recipe StructType associated with a given item ID.
     * It checks known parameters on the item's ItemType.
     *
     * @param itemId The item ID of the product.
     * @return The ID of the recipe StructType, or -1 if not found.
     */
    private static int getRecipeStructIdForItem(int itemId) {
        ItemType itemType = ConfigManager.getItemType(itemId);
        if (itemType == null) {
            ScriptConsole.println("[SmithingRecipeInspector] getRecipeStructIdForItem: Could not find ItemType for ID: " + itemId);
            return -1;
        }

        for (int paramId : RECIPE_STRUCT_ID_PARAMS_ON_PRODUCT) {
            int recipeStructId = itemType.getIntParam(paramId);
            if (recipeStructId > 0) {
                return recipeStructId;
            }
        }
        // ScriptConsole.println("[SmithingRecipeInspector] No recipe struct ID found on ItemType " + itemId + " using common params.");
        return -1;
    }

    /**
     * Parses a given recipe StructType to extract its ingredient list.
     *
     * @param recipeStruct The StructType representing the smithing recipe.
     * @return A List of {@link Ingredient} objects.
     */
    private static List<Ingredient> parseIngredientsFromRecipeStruct(StructType recipeStruct) {
        List<Ingredient> ingredients = new ArrayList<>();
        if (recipeStruct == null) { // Should have been checked before, but good for safety
            ScriptConsole.println("[SmithingRecipeInspector] parseIngredientsFromRecipeStruct: recipeStruct is null.");
            return ingredients;
        }

        Map<Integer, Serializable> params = recipeStruct.getParams();
        if (params == null) {
            ScriptConsole.println("[SmithingRecipeInspector] parseIngredientsFromRecipeStruct: Struct " + recipeStruct.getId() + " has no params.");
            return ingredients;
        }

        for (int i = 0; i < RECIPE_STRUCT_INGREDIENT_ID_KEYS.length; i++) {
            int ingredientIdParamKey = RECIPE_STRUCT_INGREDIENT_ID_KEYS[i];
            int quantityParamKey = RECIPE_STRUCT_INGREDIENT_QUANTITY_KEYS[i];

            Object idValRaw = params.get(ingredientIdParamKey);
            Object qtyValRaw = params.get(quantityParamKey);

            if (idValRaw instanceof Integer && qtyValRaw instanceof Integer) {
                int ingredientItemId = (Integer) idValRaw;
                int ingredientQuantity = (Integer) qtyValRaw;

                if (ingredientItemId > 0 && ingredientQuantity > 0) {
                    ItemType ingredientItemType = ConfigManager.getItemType(ingredientItemId);
                    if (ingredientItemType != null) {
                        ingredients.add(new Ingredient(ingredientItemType, ingredientQuantity));
                        // ScriptConsole.println("  [RecipeInspector] Added: " + ingredientItemType.getName() + " x" + ingredientQuantity + " (from Struct: " + recipeStruct.getId() + ")");
                    } else {
                        ScriptConsole.println("[SmithingRecipeInspector] parseIngredientsFromRecipeStruct: Failed to get ItemType for ingredient ID: " + ingredientItemId + " from struct " + recipeStruct.getId());
                    }
                } else {
                    // If we find a valid param pair but ID/qty is not positive,
                    // it typically means the end of this recipe's ingredient list.
                    break;
                }
            } else {
                // If the very first pair of expected ingredient parameters (e.g., 2656/2665) isn't present
                // or not of the correct type, assume this struct doesn't define ingredients in the expected way,
                // or we've reached the end of defined ingredients.
                if (i == 0) {
                    // ScriptConsole.println("[SmithingRecipeInspector] Primary ingredient params (" + ingredientIdParamKey + "/" + quantityParamKey + ") missing or not integers in recipe struct " + recipeStruct.getId());
                }
                // For subsequent ingredients, if a pair of params is missing, it means end of ingredients.
                break;
            }
        }
        if (ingredients.isEmpty()){
             // ScriptConsole.println("[SmithingRecipeInspector] No ingredients parsed from Struct ID: " + recipeStruct.getId());
        }
        return ingredients;
    }

    // Example Usage (can be removed or kept for testing)
    public static void main(String[] args) {
        // This is a placeholder for how you might test it if you had a mock ConfigManager setup.
        // In a real script, you'd call SmithingRecipeInspector.getIngredientsForProduct(someProductId);

        // Example: (Assuming item ID 4151 is a whip, which is not smithed, but for param checking demo)
        // You would replace 4151 with an actual smithable product ID like a bronze dagger.
        int testProductId = 2349; // Bronze Bar - not a final product with a recipe struct, but ItemType exists
                                   // A better test would be like a Bronze Dagger (ID: 1205)

        // ScriptConsole.println("Attempting to get ingredients for product ID: " + testProductId);
        // List<Ingredient> ingredients = getIngredientsForProduct(testProductId);

        // if (ingredients.isEmpty()) {
        //     ScriptConsole.println("No ingredients found for product ID: " + testProductId);
        // } else {
        //     ScriptConsole.println("Ingredients for " + ConfigManager.getItemType(testProductId).getName() + ":");
        //     for (Ingredient ingredient : ingredients) {
        //         ScriptConsole.println("- " + ingredient.getAmount() + " x " + ingredient.getDisplayName());
        //     }
        // }
    }
} 