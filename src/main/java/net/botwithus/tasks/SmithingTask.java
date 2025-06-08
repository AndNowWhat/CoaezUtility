package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.StructType;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.scene.entities.characters.Headbar;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.script.Execution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

public class SmithingTask implements Task {
    private CoaezUtility script;
    private Random random = new Random();

    // Enum for Smithing States from user's code
    public enum SmithingState {
        IDLE,
        SELECTING_ITEM,
        SMITHING,
        HEATING
    }

    private SmithingState currentSmithingState = SmithingState.IDLE;
    
    // Fields from user's code / implied by getters & setters
    private int selectedBarIndex = 0; // Index for barNames and barLevels
    // TODO: User needs to provide data for barNames and barLevels
    private String[] barNames = {"Bronze", "Iron", "Steel"}; // Placeholder
    private int[] barLevels = {1, 15, 30}; // Placeholder

    private int selectedItemIndex = 0; // How is this selected after choosing a bar?
    private boolean goByLevel = false;
    private boolean checkRecipe = false; // For debug utility
    private int currentRecipeStructId = 0; // For debug utility
    
    private int itemsCrafted = 0;
    private int startingSmithingXp = 0;


    // Existing enum and fields (may need to reconcile with new logic)
    public enum SmithingCategoryEnum {
        FURNACE_MASTERWORK("Furnace: Masterwork", 2411),
        ANVIL_MASTERWORK("Anvil: Masterwork", 2533),
        MASTERWORK_MATERIALS("Masterwork Materials", 15069),
        MASTERWORK_PIECES("Masterwork Pieces", 15070),
        MASTERWORK_ARMOUR("Masterwork Armour", 15071),
        MASTERWORK_WEAPONS("Masterwork Weapons", 15912),
        TRIMMED_MASTERWORK_ARMOUR("Trimmed Masterwork Armour", 15072);
        
        private final String displayName;
        private final int enumIdKey;

        SmithingCategoryEnum(String displayName, int enumIdKey) {
            this.displayName = displayName;
            this.enumIdKey = enumIdKey;
        }

        public String getDisplayName() { return displayName; }
        public int getEnumIdKey() { return enumIdKey; }

        @Override
        public String toString() { return displayName; }
    }

    private Map<String, List<Product>> productCache;
    private SmithingCategoryEnum selectedCategoryEnum; // GUI currently sets this
    private Product selectedProduct;                 // GUI currently sets this

    public SmithingTask(CoaezUtility script) {
        this.script = script;
        this.productCache = new LinkedHashMap<>();
        ScriptConsole.println("[SmithingTask] Initialized.");
        LocalPlayer localPlayer = Client.getLocalPlayer(); // Get player once
        if (localPlayer != null && Skills.SMITHING.getActualLevel() > 0) { // Ensure level is positive before getting XP at level
            startingSmithingXp = Skills.SMITHING.getExperienceAt(Skills.SMITHING.getActualLevel()); 
        }
    }

    // Getter/Setters from our previous design (GUI uses these)
    public List<SmithingCategoryEnum> getCategories() {
        return List.of(SmithingCategoryEnum.values());
    }

    public SmithingCategoryEnum getSelectedCategory() {
        return selectedCategoryEnum;
    }

    public void setSelectedCategory(SmithingCategoryEnum category) {
        if (this.selectedCategoryEnum != category) {
            this.selectedCategoryEnum = category;
            // this.selectedProduct = null; // Reset product when category changes
            // ScriptConsole.println("[SmithingTask] GUI Selected Category: " + (category != null ? category.getDisplayName() : "None"));
            // TODO: How does this connect to selectedBarIndex?
        }
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product product) {
        this.selectedProduct = product;
        // ScriptConsole.println("[SmithingTask] GUI Selected Product: " + (product != null ? product.getName() + " (ID: " + product.getId() + ")" : "None"));
        // TODO: How does this connect to selectedItemIndex?
    }

    // Helper method to get the Recipe Struct ID from an ItemType
    private int getRecipeStructIdForSmithingItem(int itemId) {
        ItemType itemType = ConfigManager.getItemType(itemId);
        if (itemType == null) {
            ScriptConsole.println("[SmithingTask] getRecipeStructIdForSmithingItem: Could not find ItemType for ID: " + itemId);
            return -1;
        }
        // Check common params for recipe struct ID. Prioritize 2675, then 2645, etc.
        // These are typical parameters on the *product ItemType* that point to a StructType defining the recipe.
        int recipeStructId = itemType.getIntParam(2675); 
        if (recipeStructId <= 0) {
            recipeStructId = itemType.getIntParam(2645); 
        }
        // Add more checks for other known params if necessary
        if (recipeStructId <= 0) {
            // ScriptConsole.println("[SmithingTask] No recipe struct ID found on ItemType " + itemId + " using common params (e.g., 2675, 2645).");
        }
        return recipeStructId > 0 ? recipeStructId : -1;
    }

    // Helper method to parse ingredients from a given Recipe StructType
    private List<Ingredient> parseIngredientsFromStruct(StructType recipeStruct) {
        List<Ingredient> ingredients = new ArrayList<>();
        if (recipeStruct == null) {
            // ScriptConsole.println("[SmithingTask] parseIngredientsFromStruct: recipeStruct is null.");
            return ingredients;
        }

        Map<Integer, Serializable> params = recipeStruct.getParams();
        if (params == null) {
            ScriptConsole.println("[SmithingTask] parseIngredientsFromStruct: Struct " + recipeStruct.getId() + " has no params.");
            return ingredients;
        }

        // Define known parameter IDs for ingredient items and their quantities within a recipe StructType.
        // These arrays should be ordered: ITEM_ID_PARAMS[i] corresponds to QUANTITY_PARAMS[i].
        final int[] INGREDIENT_ITEM_ID_PARAMS = {2656, 2657, 2658, 2659, 2660, 2661, 2662, 2663}; // Example: Bar ID, Flux ID etc.
        final int[] INGREDIENT_QUANTITY_PARAMS = {2665, 2667, 2668, 2669, 2670, 2671, 2672, 2673}; // Example: Bar Quantity, Flux Quantity etc.

        for (int i = 0; i < INGREDIENT_ITEM_ID_PARAMS.length; i++) {
            int ingredientIdParamKey = INGREDIENT_ITEM_ID_PARAMS[i];
            int quantityParamKey = INGREDIENT_QUANTITY_PARAMS[i];

            Object idValRaw = params.get(ingredientIdParamKey);
            Object qtyValRaw = params.get(quantityParamKey);

            if (idValRaw instanceof Integer && qtyValRaw instanceof Integer) {
                int ingredientItemId = (Integer) idValRaw;
                int ingredientQuantity = (Integer) qtyValRaw;

                if (ingredientItemId > 0 && ingredientQuantity > 0) {
                    ItemType ingredientItemType = ConfigManager.getItemType(ingredientItemId);
                    if (ingredientItemType != null) {
                        ingredients.add(new Ingredient(ingredientItemType, ingredientQuantity));
                        // ScriptConsole.println("  [StructParse] Added: " + ingredientItemType.getName() + " x" + ingredientQuantity);
                    } else {
                        ScriptConsole.println("[SmithingTask] parseIngredientsFromStruct: Failed to get ItemType for ingredient ID: " + ingredientItemId + " from struct " + recipeStruct.getId());
                    }
                } else {
                    // Stop if we find a valid pair of params but the item ID or quantity is zero/negative,
                    // as it might indicate the end of the ingredient list for this recipe.
                    break; 
                }
            } else {
                // If the very first pair of expected ingredient parameters (e.g., 2656/2665) isn't present
                // or not of the correct type, assume this struct doesn't define ingredients in the expected way.
                if (i == 0) {
                    // ScriptConsole.println("[SmithingTask] Primary ingredient params (" + ingredientIdParamKey + "/" + quantityParamKey + ") missing or not integers in recipe struct " + recipeStruct.getId());
                }
                // For subsequent ingredients, if a pair of params is missing, it means end of ingredients.
                break;
            }
        }
        return ingredients;
    }

    // getProductsForCategory from our previous design
    public List<Product> getProductsForCategory(SmithingCategoryEnum category) {
        if (category == null) {
            // ScriptConsole.println("[SmithingTask] Category is null, cannot get products.");
            return new ArrayList<>();
        }
        String cacheKey = category.name(); 
        // Consider cache invalidation or selective caching if product data can change
        // For now, simple cache check:
        // if (productCache.containsKey(cacheKey)) {
        //     return new ArrayList<>(productCache.get(cacheKey));
        // }

        List<Product> products = new ArrayList<>();
        int gameEnumId = category.getEnumIdKey(); // This is the Enum ID that lists the product Item IDs
        EnumType productListEnum = null;
        try {
            productListEnum = ConfigManager.getEnumType(gameEnumId);
        } catch (Exception e) {
            ScriptConsole.println("[SmithingTask] Error fetching EnumType for ID: " + gameEnumId + " for category " + category.getDisplayName() + ". Error: " + e.getMessage());
            return products; // Return empty list on error
        }

        if (productListEnum != null && productListEnum.getOutputs() != null) {
            // ScriptConsole.println("[SmithingTask] Processing " + productListEnum.getOutputs().size() + " potential products from Enum " + gameEnumId + " for category " + category.getDisplayName());
            for (Object output : productListEnum.getOutputs()) {
                if (output instanceof Integer) {
                    int productId = (Integer) output;
                    ItemType itemType = ConfigManager.getItemType(productId);
                    if (itemType != null) {
                        // Create Product instance. Its constructor might attempt general parsing.
                        Product product = new Product(itemType); 
                        
                        // Now, SmithingTask uses its specialized logic to get and set ingredients.
                        int recipeStructId = getRecipeStructIdForSmithingItem(productId);
                        List<Ingredient> specificIngredients = new ArrayList<>();

                        if (recipeStructId > 0) {
                            StructType recipeStruct = ConfigManager.getStructType(recipeStructId);
                            if (recipeStruct != null) {
                                // ScriptConsole.println("  [Product: " + itemType.getName() + "] Found recipe Struct ID: " + recipeStructId + ". Parsing for ingredients...");
                                specificIngredients = parseIngredientsFromStruct(recipeStruct);
                            } else {
                                // ScriptConsole.println("  [Product: " + itemType.getName() + "] Found recipe Struct ID: " + recipeStructId + " but failed to load StructType.");
                            }
                        } else {
                            // ScriptConsole.println("  [Product: " + itemType.getName() + "] No specific recipe Struct ID found. Ingredients might rely on Product's internal parsing or be empty.");
                        }
                        
                        // Set the ingredients determined by SmithingTask's logic.
                        // This overrides anything Product.parseIngredients() might have found.
                        product.setIngredients(specificIngredients);

                        if (specificIngredients.isEmpty() && product.getIngredients().isEmpty()) {
                             // ScriptConsole.println("  [Product: " + itemType.getName() + "] No ingredients found by SmithingTask specific logic or by Product internal parsing.");
                        } else if (specificIngredients.isEmpty() && !product.getIngredients().isEmpty()){
                        }

                        products.add(product);
                    } else {
                        // ScriptConsole.println("[SmithingTask] Failed to get ItemType for product ID: " + productId + " in category " + category.getDisplayName());
                    }
                }
            }
        } else {
            if (productListEnum == null) {
                ScriptConsole.println("[SmithingTask] Could not find EnumType with ID: " + gameEnumId + " for category " + category.getDisplayName());
            } else {
                ScriptConsole.println("[SmithingTask] Product Enum " + gameEnumId + " for category " + category.getDisplayName() + " has null or empty outputs.");
            }
        }

        if (products.isEmpty()) {
             // ScriptConsole.println("[SmithingTask] No products loaded for category: " + category.getDisplayName() + " (GameEnumID: " + gameEnumId + ").");
        } else {
             // ScriptConsole.println("[SmithingTask] Loaded " + products.size() + " products for " + category.getDisplayName() + " with SmithingTask-specific ingredient parsing.");
        }
        
        // Update cache with the newly processed list
        // productCache.put(cacheKey, products); // Uncomment if caching is desired
        return new ArrayList<>(products); // Return a copy
    }

    // --- Start of User Provided Smithing Logic ---

    private long handleSmithing(LocalPlayer player) {
        // Process smithing actions based on the current smithing state
        // Note: SmithingState.values()[currentSmithingState_as_int] was in original, changed to use enum directly
        switch (currentSmithingState) {
            case SELECTING_ITEM:
                return handleItemInterfaceSelection();
            case SMITHING:
                return handleSmithingProcess(player);
            case HEATING:
                return handleHeatingProcess(player);
            default: // IDLE or any other state
                // What should happen in IDLE? Maybe transition to SELECTING_ITEM if product is chosen?
                // For now, just a delay.
                return random.nextLong(600, 800);
        }
    }
    
    private long handleItemInterfaceSelection() {
        // IMPORTANT: Interface 37 is the OLD pre-rework smithing interface.
        // Modern smithing uses interface 1472. This logic might need significant updates.
        if (!Interfaces.isOpen(37)) {
            ScriptConsole.println("[SmithingTask] Interface 37 (Old Smithing) not open. Attempting to open via Anvil...");
            SceneObject anvilObj = SceneObjectQuery.newQuery().name("Anvil").option("Smith").results().nearest();
            if (anvilObj != null) {
                anvilObj.interact("Smith");
                Execution.delayUntil(3000L, new Callable<Boolean>() { @Override public Boolean call() { return Interfaces.isOpen(37); } }); 
                if (Interfaces.isOpen(37)) {
                    ScriptConsole.println("[SmithingTask] Interface 37 opened.");
                } else {
                    ScriptConsole.println("[SmithingTask] Failed to open Interface 37.");
                    // Consider changing state back to IDLE or an error state.
                    currentSmithingState = SmithingState.IDLE;
                    return random.nextLong(300,500);
                }
            } else {
                 ScriptConsole.println("[SmithingTask] No Anvil found to open smithing interface.");
                 currentSmithingState = SmithingState.IDLE;
                 return random.nextLong(300,500);
            }
            return random.nextLong(300, 500);
        }
        
        // This component 37:40 was for the selected item display in the OLD interface.
        Component selectedItemComp = ComponentQuery.newQuery(37).componentIndex(40).results().first(); // subComponentIndex(-1) typically not needed for .first()
        if (selectedItemComp == null) { // This might mean the interface isn't fully loaded or structure is unexpected
            ScriptConsole.println("[SmithingTask] Selected Item Component (37:40) is Null. Interface might not be ready or structure mismatch.");
            // It's often better to wait a short moment or re-verify interface state than to immediately close/reopen.
            return random.nextLong(300, 500);
        }
        
        // Select the bar type from component 37:52 (list of bars in OLD interface)
        // Ensure selectedBarIndex is valid
        if (selectedBarIndex < 0 || selectedBarIndex >= barNames.length) {
            ScriptConsole.println("[SmithingTask] Invalid selectedBarIndex: " + selectedBarIndex + ". Resetting to IDLE.");
            currentSmithingState = SmithingState.IDLE;
            return random.nextLong(300,500);
        }
        String desiredBarName = barNames[selectedBarIndex];
        // The subcomponent index calculation (selectedBarIndex * 2 + 1) is specific to the old interface's list structure.
        Component barComponent = ComponentQuery.newQuery(37).componentIndex(52).subComponentIndex(selectedBarIndex * 2 + 1).results().first();
        
        if (barComponent != null) {
            ScriptConsole.println("[SmithingTask] Attempting to select bar: " + desiredBarName + " at index " + selectedBarIndex);
            // The action string "Select " + desiredBarName might be too specific or locale-dependent.
            // Using a more generic action ID if available or just "Interact" might be more robust.
            // For now, using the provided logic.
            if (barComponent.interact("Select " + desiredBarName)) { // Or simply barComponent.interact(); if action is default
                 ScriptConsole.println("[SmithingTask] Selected bar: " + desiredBarName + ". Next state should be selecting specific item to smith.");
                 // PROBLEM: This method only selects the BAR. It doesn't select the ITEM to make from the bar.
                 // After selecting a bar, the next step is to choose (e.g.) a dagger, sword, etc.
                 // The current logic needs to be extended or currentSmithingState needs to transition to an ITEM_SELECTION state.
                 // For now, assuming after bar selection, we might go to SMITHING state, but this is incomplete.
                 // currentSmithingState = SmithingState.SMITHING; // This is likely wrong, need item selection first.
                 ScriptConsole.println("[SmithingTask] WARNING: Item selection logic after bar selection is missing.");
                 // Perhaps transition to a new state like SELECTING_PRODUCT_FROM_BAR
            } else {
                 ScriptConsole.println("[SmithingTask] Failed to interact with bar component for: " + desiredBarName);
            }
            return random.nextLong(800, 1200);
        } else {
            ScriptConsole.println("[SmithingTask] Bar component not found for: " + desiredBarName + " at index " + selectedBarIndex);
        }
        
        return random.nextLong(300, 500);
    }
    
    private long handleSmithingProcess(LocalPlayer player) {
        if (player == null) return random.nextLong(600,1000);

        SceneObject anvil = SceneObjectQuery.newQuery().name("Anvil").option("Smith").results().nearest();
        if (anvil == null) {
            ScriptConsole.println("[SmithingTask] No anvil found nearby.");
            currentSmithingState = SmithingState.IDLE; // Or some error/recovery state
            return random.nextLong(600, 1000);
        }
        
        // Check for "Unfinished smithing item" might be specific to a very particular smithing recipe/method.
        // Modern smithing usually consumes bars and produces items directly or via progress.
        if (player.getAnimationId() == -1 && Backpack.contains("Unfinished smithing item")) {
            ScriptConsole.println("[SmithingTask] Has unfinished item and not animating. Interacting with Anvil.");
            if(anvil.interact("Smith")) {
                Execution.delayUntil(5000L, new Callable<Boolean>() { @Override public Boolean call() { return player.getAnimationId() != -1; } }); 
                if (player.getAnimationId() != -1) {
                    ScriptConsole.println("[SmithingTask] Smithing animation started.");
                    // itemsCrafted++; // Increment when smithing *completes*, not just starts animation.
                                   // Need a way to detect completion (e.g. item appears, animation ends and no unfinished item).
                } else {
                    ScriptConsole.println("[SmithingTask] Failed to start smithing animation.");
                }
            } else {
                 ScriptConsole.println("[SmithingTask] Failed to interact with Anvil.");
            }
        } else if (player.getAnimationId() != -1) {
            ScriptConsole.println("[SmithingTask] Currently smithing (Animation: " + player.getAnimationId() + "). Waiting.");
            // Need logic to detect when smithing of one item is finished to either continue or heat.
        } else if (!Backpack.contains("Unfinished smithing item")) {
             ScriptConsole.println("[SmithingTask] No unfinished smithing item found. Assuming task for current item is done or needs materials.");
             // This could be a point to transition to HEATING or SELECTING_ITEM if more are to be made.
             // For now, stays in SMITHING state, may loop if conditions not met.
             // currentSmithingState = SmithingState.HEATING; // Example transition
        }
        
        return random.nextLong(800, 1200);
    }
    
    private long handleHeatingProcess(LocalPlayer player) {
        if (player == null) return random.nextLong(600,1000);

        SceneObject forge = SceneObjectQuery.newQuery().name("Forge").option("Heat").results().nearest(); // Assuming "Heat" is an option
        if (forge == null) {
            ScriptConsole.println("[SmithingTask] No forge found nearby.");
            // currentSmithingState = SmithingState.IDLE; // Or some error/recovery state
            return random.nextLong(600, 1000);
        }
        
        // Headbar ID 5 for heat is specific. Verification needed if this is standard.
        Headbar heatbar = player.getHeadbars().stream()
            .filter(bar -> bar.getId() == 5) 
            .findFirst()
            .orElse(null);
            
        if (heatbar != null) {
            // Assuming getWidth() returns current heat and 255.0 is max heat for this specific bar (ID 5)
            double currentHeatPercentage = (double) heatbar.getWidth() / 255.0; 
            ScriptConsole.println(String.format("[SmithingTask] Current heat level: %d/255 (%.2f%%)", heatbar.getWidth(), currentHeatPercentage * 100));
            // Heat if below 67%
            if (currentHeatPercentage < 0.67) {
                ScriptConsole.println("[SmithingTask] Heat is low. Heating item at forge.");
                if(forge.interact("Heat")) { // Ensure "Heat" is the correct action
                    Execution.delay(600L); // Initial delay after interaction
                    // Wait until heat is near full (e.g., > 250, which is ~98%)
                    boolean heated = Execution.delayUntil(5000L, () -> {
                        Headbar newHeatbar = player.getHeadbars().stream()
                                .filter(bar -> bar.getId() == 5)
                                .findFirst()
                                .orElse(null);
                        // Check if newHeatbar is not null and its width indicates sufficient heat
                        return newHeatbar != null && ((double)newHeatbar.getWidth() / 255.0) >= 0.98;
                    });
                    if (heated) {
                        ScriptConsole.println("[SmithingTask] Item heated sufficiently.");
                        currentSmithingState = SmithingState.SMITHING; // Go back to smithing
                    } else {
                        ScriptConsole.println("[SmithingTask] Failed to heat item sufficiently or heat bar not found after interaction.");
                    }
                } else {
                    ScriptConsole.println("[SmithingTask] Failed to interact with Forge to heat.");
                }
            } else {
                ScriptConsole.println("[SmithingTask] Heat is sufficient. Transitioning to SMITHING state.");
                currentSmithingState = SmithingState.SMITHING; // Heat is fine, go smith
            }
        } else {
            ScriptConsole.println("[SmithingTask] Heat headbar (ID 5) not found on player. Cannot determine heat level.");
            // What to do here? Maybe try to smith anyway or assume it needs heating?
            // For safety, perhaps go to smithing, or try one heat cycle.
            // currentSmithingState = SmithingState.SMITHING;
        }
        
        return random.nextLong(800, 1200);
    }
    
    // Smithing Recipe Checking (Utility method from user)
    public void checkRecipeDetails(int structId) {
        StructType structType = ConfigManager.getStructType(structId);
        if (structType == null) {
            ScriptConsole.println("[SmithingTask Debug] Struct " + structId + " not found.");
            // printToTextFile("Struct " + structId + " not found."); // File logging commented out
            return;
        }
        
        Map<Integer, Serializable> params = structType.getParams();
        int requiredItemId = params.getOrDefault(2656, -1) instanceof Integer ? (int) params.get(2656) : -1;
        int requiredQuantity = params.getOrDefault(2665, -1) instanceof Integer ? (int) params.get(2665) : -1;
        int param2666Value = params.getOrDefault(2666, -1) instanceof Integer ? (int) params.get(2666) : -1;
        
        String requiredItemName = "Unknown";
        if (requiredItemId > 0) {
            ItemType requiredItem = ConfigManager.getItemType(requiredItemId);
            if (requiredItem != null) {
                requiredItemName = requiredItem.getName();
            }
        }
        
        ScriptConsole.println("==== Smithing Recipe (Struct ID: " + structId + ") ====");
        if (requiredItemId > 0) {
            ScriptConsole.println("Required Material: " + requiredItemName + " (ID: " + requiredItemId + ")");
            ScriptConsole.println("Required Quantity: " + requiredQuantity);
        } else {
            ScriptConsole.println("No crafting materials found in this struct (Param 2656/2665).");
        }
        ScriptConsole.println("Param 2666: " + param2666Value + " (Unknown purpose)");
    }
    
    // Getters and setters from user's code for smithing functionality
    public void setSmithingState(SmithingState state) { // Changed int to SmithingState enum
        if (this.currentSmithingState != state) {
            ScriptConsole.println("[SmithingTask] State changing from " + this.currentSmithingState + " to " + state);
            this.currentSmithingState = state;
            LocalPlayer localPlayer = Client.getLocalPlayer();
            if (state == SmithingState.SELECTING_ITEM && localPlayer != null && Skills.SMITHING.getActualLevel() > 0) { // Reset XP on new selection cycle
                 startingSmithingXp = Skills.SMITHING.getExperienceAt(Skills.SMITHING.getActualLevel());
                 itemsCrafted = 0;
            }
        }
    }
    
    public SmithingState getSmithingState() { // Changed int to SmithingState enum
        return this.currentSmithingState;
    }
    
    public void setSelectedBarIndex(int index) {
        // TODO: User needs to provide data for barNames. Length check depends on it.
        if (barNames != null && index >= 0 && index < barNames.length) {
            this.selectedBarIndex = index;
            ScriptConsole.println("[SmithingTask] Selected Bar Index set to: " + index + " (" + barNames[index] + ")");
            // When bar index is set, typically item selection process should begin.
            setSmithingState(SmithingState.SELECTING_ITEM);
        } else {
            ScriptConsole.println("[SmithingTask] Attempted to set invalid selectedBarIndex: " + index);
        }
    }
    
    public int getSelectedBarIndex() {
        return this.selectedBarIndex;
    }
    
    public void setSelectedItemIndex(int index) {
        // TODO: This needs context. Index of what? Products for the selected bar?
        this.selectedItemIndex = index;
        ScriptConsole.println("[SmithingTask] Selected Item Index set to: " + index);
        // This might be where we transition fully to start smithing/heating.
        // setSmithingState(SmithingState.HEATING); // Or SMITHING if heating not always first
    }
    
    public int getSelectedItemIndex() {
        return this.selectedItemIndex;
    }
    
    public void setGoByLevel(boolean goByLevel) {
        this.goByLevel = goByLevel;
        if (goByLevel) {
            // TODO: User needs to provide data for barLevels.
            LocalPlayer localPlayer = Client.getLocalPlayer();
            if (barLevels != null && barNames != null && localPlayer != null) {
                int currentLevel = Skills.SMITHING.getActualLevel();
                for (int i = barLevels.length - 1; i >= 0; i--) {
                    if (currentLevel >= barLevels[i]) {
                        setSelectedBarIndex(i); // This will also log and potentially change state
                        break;
                    }
                }
            } else {
                 ScriptConsole.println("[SmithingTask] Cannot set bar by level: barLevels/barNames not initialized or player not available.");
            }
        }
    }
    
    public boolean isGoByLevel() {
        return this.goByLevel;
    }
    
    public void setCheckRecipe(boolean check) {
        this.checkRecipe = check;
    }
    
    public boolean isCheckRecipe() {
        return this.checkRecipe;
    }
    
    public void setCurrentRecipeStructId(int id) {
        this.currentRecipeStructId = id;
    }
    
    public int getCurrentRecipeStructId() {
        return this.currentRecipeStructId;
    }
    
    public int getXpGained() {
        LocalPlayer localPlayer = Client.getLocalPlayer();
        if (localPlayer == null || Skills.SMITHING.getActualLevel() <= 0) return 0; // Prevent issues if level is 0 or player is null
        return Skills.SMITHING.getExperienceAt(Skills.SMITHING.getActualLevel()) - startingSmithingXp;
    }
    
    public int getItemsCrafted() {
        return this.itemsCrafted;
    }

    // Utility methods from user's 'handleSkilling' and other sections
    // These seem like debug/data-exploration tools.
    private void checkStructParams(int structId) { // Renamed from user's second checkStructParams to avoid conflict
        StructType structType = ConfigManager.getStructType(structId);
        if (structType == null) {
            ScriptConsole.println("[SmithingTask Debug] Struct " + structId + " not found.");
            return;
        }
        Map<Integer, Serializable> params = structType.getParams();
        int requiredItemId = params.getOrDefault(2656, -1) instanceof Integer ? (int) params.get(2656) : -1;
        int requiredQuantity = params.getOrDefault(2665, -1) instanceof Integer ? (int) params.get(2665) : -1;
        int param2666Value = params.getOrDefault(2666, -1) instanceof Integer ? (int) params.get(2666) : -1;
        
        String requiredItemName = "Unknown";
        if (requiredItemId > 0) {
            ItemType requiredItem = ConfigManager.getItemType(requiredItemId);
            if (requiredItem != null) {
                requiredItemName = requiredItem.getName();
            }
        }
        ScriptConsole.println("==== [Debug] Crafting Recipe (Struct ID: " + structId + ") ====");
        if (requiredItemId > 0) {
            ScriptConsole.println("[Debug] Required Material: " + requiredItemName + " (ID: " + requiredItemId + ")");
            ScriptConsole.println("[Debug] Required Quantity: " + requiredQuantity);
        } else {
            ScriptConsole.println("[Debug] No crafting materials found in this struct (Param 2656/2665).");
        }
        ScriptConsole.println("[Debug] Param 2666: " + param2666Value + " (Unknown purpose)");
    }

    private int getSpecificItemParamData(int enumId, int targetItemId) {
        EnumType enumType = ConfigManager.getEnumType(enumId);
        if (enumType == null) {
            ScriptConsole.println("[SmithingTask Debug] Enum " + enumId + " not found.");
            return -1;
        }
        List<Object> outputs = enumType.getOutputs();
        boolean itemFound = false;
        int itemToCheck = targetItemId;
        
        for (Object output : outputs) {
            if (output instanceof Integer) {
                int itemId = (int) output;
                if (itemId == targetItemId) {
                    itemToCheck = itemId;
                    itemFound = true;
                    break;
                }
            }
        }
        if (!itemFound && !outputs.isEmpty() && outputs.get(0) instanceof Integer) {
            itemToCheck = (int) outputs.get(0);
            ScriptConsole.println("[SmithingTask Debug] Target item " + targetItemId + " not found in enum. Using first item: " + itemToCheck);
        }
        
        ItemType itemType = ConfigManager.getItemType(itemToCheck);
        if (itemType != null) {
            ScriptConsole.println("[SmithingTask Debug] Item Details - ID: " + itemType.getId() + ", Name: " + itemType.getName());
            int param2645 = itemType.getIntParam(2645); // Defaulting to 0 if not found
            int param2675 = itemType.getIntParam(2675); // Defaulting to 0 if not found
            ScriptConsole.println("[SmithingTask Debug] Param 2645: " + param2645);
            ScriptConsole.println("[SmithingTask Debug] Param 2675: " + param2675);
            return param2675; // Return param2675 value
        } else {
            ScriptConsole.println("[SmithingTask Debug] Failed to get ItemType for ID: " + itemToCheck);
            return -1;
        }
    }

    private void readCustomEnum(int enumId) {
        EnumType enumType = ConfigManager.getEnumType(enumId);
        if (enumType == null) {
            ScriptConsole.println("[SmithingTask Debug] Enum " + enumId + " not found.");
            return;
        }
        List<Object> outputs = enumType.getOutputs();
        ScriptConsole.println("[SmithingTask Debug] Processing enum " + enumId + " with " + outputs.size() + " entries");
        for (Object output : outputs) {
            if (output instanceof Integer) {
                int itemId = (int) output;
                ItemType itemType = ConfigManager.getItemType(itemId);
                if (itemType != null) {
                    ScriptConsole.println("[SmithingTask Debug] Item ID: " + itemType.getId() + "  Item name: " + itemType.getName());
                }
            } else {
                ScriptConsole.println("[SmithingTask Debug] Non-integer output: " + output);
            }
        }
        ScriptConsole.println("[SmithingTask Debug] Finished processing enum " + enumId);
    }

    // --- End of User Provided Smithing Logic ---

    @Override
    public void execute() {
/*         if (script.getBotState() != CoaezUtility.BotState.SMITHING) { 
            // If not in smithing state, and our internal state is not IDLE, reset it.
            if (currentSmithingState != SmithingState.IDLE) {
                 ScriptConsole.println("[SmithingTask] Bot state is not SMITHING. Resetting internal smithing state to IDLE.");
                 currentSmithingState = SmithingState.IDLE;
            }
            return;
        }
 */
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[SmithingTask] LocalPlayer is null. Waiting...");
            Execution.delay(random.nextLong(600,1000)); // Added random delay
            return;
        }

        // Initialize startingSmithingXp if it hasn't been (e.g. script reloaded while task active)
        if (startingSmithingXp == 0 && Skills.SMITHING.getActualLevel() > 0) {
            startingSmithingXp = Skills.SMITHING.getExperienceAt(Skills.SMITHING.getActualLevel());
        }
        
        // Optionally, trigger recipe check if flag is set
        if (checkRecipe && currentRecipeStructId > 0) {
            ScriptConsole.println("[SmithingTask] Debug: Checking recipe for struct ID: " + currentRecipeStructId);
            checkRecipeDetails(currentRecipeStructId); // or checkStructParams(currentRecipeStructId)
            // checkRecipe = false; // Optionally reset flag after checking once
        }

        // Main logic delegation
        long delay = handleSmithing(player);
        Execution.delay(delay);
    }
} 