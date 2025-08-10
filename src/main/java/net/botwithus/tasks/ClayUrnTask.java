package net.botwithus.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.api.game.hud.inventories.DepositBox;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Distance;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.EnumType;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.Headbar;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.hud.interfaces.Component;

public class ClayUrnTask implements Task {
    private final CoaezUtility script;
    
    // Enum ID for pottery crafting groups
    private static final int POTTERY_GROUP_ENUM_ID = 7004;
    
    // Dynamic urn categories and types loaded from game data
    private List<UrnCategory> availableCategories;
    private List<UrnType> availableUrns;
    
    // Default selections - will be updated when data is loaded
    private UrnCategory selectedCategory;
    private UrnType selectedUrn;
    
    public static class UrnCategory {
        private final int index;
        private final String displayName;
        private final int enumId;
        
        public UrnCategory(int index, String displayName, int enumId) {
            this.index = index;
            this.displayName = displayName;
            this.enumId = enumId;
        }
        
        public int getIndex() { return index; }
        public String getDisplayName() { return displayName; }
        public int getEnumId() { return enumId; }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public static class UrnType {
        private final int id;
        private final String displayName;
        private final UrnCategory category;
        
        public UrnType(int id, String displayName, UrnCategory category) {
            this.id = id;
            this.displayName = displayName;
            this.category = category;
        }
        
        public int getId() { return id; }
        public String getDisplayName() { return displayName; }
        public UrnCategory getCategory() { return category; }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // State logic
    private UrnTaskState currentState;

    // State instances
    private final UrnTaskState mineClayState = new MineClayState();
    private final UrnTaskState goUpstairsState = new GoUpstairsState();
    private final UrnTaskState softenClayState = new SoftenClayState();
    private final UrnTaskState spinUrnsState = new SpinUrnsState();
    private final UrnTaskState fireUrnsState = new FireUrnsState();
    private final UrnTaskState addRunesState = new AddRunesState();
    private final UrnTaskState depositUrnsState = new DepositUrnsState();

    public ClayUrnTask(CoaezUtility script) {
        this.script = script;
        loadUrnData();
        currentState = depositUrnsState;
    }
    
    private void loadUrnData() {
        try {
            availableCategories = new ArrayList<>();
            availableUrns = new ArrayList<>();
            
            ScriptConsole.println("[ClayUrnTask] Attempting to load urn data from enum " + POTTERY_GROUP_ENUM_ID);
            
            // Load the main pottery group enum
            EnumType potteryGroupEnum = ConfigManager.getEnumType(POTTERY_GROUP_ENUM_ID);
            if (potteryGroupEnum == null) {
                ScriptConsole.println("[ClayUrnTask] Error: ConfigManager.getEnumType(" + POTTERY_GROUP_ENUM_ID + ") returned null");
                return;
            }
            if (potteryGroupEnum.getOutputs() == null) {
                ScriptConsole.println("[ClayUrnTask] Error: Enum " + POTTERY_GROUP_ENUM_ID + " has no outputs");
                return;
            }
            
            ScriptConsole.println("[ClayUrnTask] Successfully loaded pottery group enum with " + potteryGroupEnum.getOutputs().size() + " outputs");
            
            // Process each category enum ID from the main group
            for (int i = 0; i < potteryGroupEnum.getOutputs().size(); i++) {
                Object categoryEnumIdObj = potteryGroupEnum.getOutputs().get(i);
                ScriptConsole.println("[ClayUrnTask] Processing output " + i + ": " + categoryEnumIdObj + " (type: " + (categoryEnumIdObj != null ? categoryEnumIdObj.getClass().getSimpleName() : "null") + ")");
                
                if (categoryEnumIdObj instanceof Integer) {
                    int categoryEnumId = (Integer) categoryEnumIdObj;
                    ScriptConsole.println("[ClayUrnTask] Loading category enum " + categoryEnumId);
                    
                    // Load the category enum to get its name and products
                    EnumType categoryEnum = ConfigManager.getEnumType(categoryEnumId);
                    if (categoryEnum == null) {
                        ScriptConsole.println("[ClayUrnTask] Warning: Failed to load category enum " + categoryEnumId + ", skipping");
                        continue;
                    }
                    if (categoryEnum.getOutputs() == null) {
                        ScriptConsole.println("[ClayUrnTask] Warning: Category enum " + categoryEnumId + " has no outputs, skipping");
                        continue;
                    }
                    
                    // Create category
                    UrnCategory category = new UrnCategory(i, getCategoryDisplayName(i), categoryEnumId);
                    availableCategories.add(category);
                    
                    ScriptConsole.println("[ClayUrnTask] Category " + i + ": " + category.getDisplayName() + " (enum " + categoryEnumId + ") with " + categoryEnum.getOutputs().size() + " products");
                    
                    // Process products in this category
                    for (Object productIdObj : categoryEnum.getOutputs()) {
                        if (productIdObj instanceof Integer) {
                            int productId = (Integer) productIdObj;
                            
                            // Get item details from ConfigManager
                            ItemType itemType = ConfigManager.getItemType(productId);
                            if (itemType != null && itemType.getName() != null) {
                                String itemName = itemType.getName();
                                // Filter for unfired urns only
                                if (itemName.contains("urn") && itemName.contains("unfired")) {
                                    UrnType urn = new UrnType(productId, itemName, category);
                                    availableUrns.add(urn);
                                    ScriptConsole.println("[ClayUrnTask]   - " + itemName + " (ID: " + productId + ")");
                                }
                            } else {
                                ScriptConsole.println("[ClayUrnTask]   - Warning: Could not get item type for ID " + productId);
                            }
                        } else {
                            ScriptConsole.println("[ClayUrnTask]   - Warning: Non-integer product ID: " + productIdObj);
                        }
                    }
                } else {
                    ScriptConsole.println("[ClayUrnTask] Warning: Non-integer category enum ID: " + categoryEnumIdObj);
                }
            }
            
            // Set default selections
            if (!availableCategories.isEmpty()) {
                selectedCategory = availableCategories.get(0);
                ScriptConsole.println("[ClayUrnTask] Set default category: " + selectedCategory.getDisplayName());
                if (!availableUrns.isEmpty()) {
                    selectedUrn = availableUrns.get(0);
                    ScriptConsole.println("[ClayUrnTask] Set default urn: " + selectedUrn.getDisplayName());
                }
            }
            
            ScriptConsole.println("[ClayUrnTask] Successfully loaded " + availableCategories.size() + " categories and " + availableUrns.size() + " urn types");
            
        } catch (Exception e) {
            ScriptConsole.println("[ClayUrnTask] Error loading urn data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getCategoryDisplayName(int index) {
        // Map the index to readable names based on the enum data you provided
        switch (index) {
            case 0: return "Pottery";
            case 1: return "Cooking Urns";
            case 2: return "Divination Urns";
            case 3: return "Farming Urns";
            case 4: return "Fishing Urns";
            case 5: return "Hunter Urns";
            case 6: return "Mining Urns";
            case 7: return "Prayer Urns";
            case 8: return "Runecrafting Urns";
            case 9: return "Smelting Urns";
            case 10: return "Woodcutting Urns";
            default: return "Category " + index;
        }
    }

    @Override
    public void execute() {
        if (!isDataLoaded()) {
            ScriptConsole.println("[ClayUrnTask] Urn data not yet loaded, attempting to load...");
            loadUrnData();
            if (!isDataLoaded()) {
                ScriptConsole.println("[ClayUrnTask] Failed to load urn data, cannot proceed");
                Execution.delay(1000);
                return;
            }
        }
        if (currentState != null) {
            currentState.handle(this);
        }
    }

    // State transition helpers
    public void setStateToMineClay() { currentState = mineClayState; }
    public void setStateToGoUpstairs() { currentState = goUpstairsState; }
    public void setStateToSoftenClay() { currentState = softenClayState; }
    public void setStateToSpinUrns() { currentState = spinUrnsState; }
    public void setStateToFireUrns() { currentState = fireUrnsState; }
    public void setStateToAddRunes() { currentState = addRunesState; }
    public void setStateToDepositUrns() { currentState = depositUrnsState; }

    void handleMineClayUnderground() {
        // Only transition if backpack is full
        if (Backpack.isFull()) {
            ScriptConsole.println("[ClayUrnTask] Backpack full, transitioning to upstairs");
            setStateToGoUpstairs();
            return;
        }

        // Basic interface handling for dialogs
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }

        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Clay rock").option("Mine").results();
        SceneObject rock = results.nearest();

        boolean hasRockInRange = false;
        if (rock != null && rock.getCoordinate() != null && Client.getLocalPlayer() != null && Client.getLocalPlayer().getCoordinate() != null) {
            double distanceToRock = Distance.between(Client.getLocalPlayer().getCoordinate(), rock.getCoordinate());
            hasRockInRange = distanceToRock <= 25.0;
        }

        if (!hasRockInRange) {
            if (attemptEnterUnderground()) {
                return;
            }
            Execution.delay(script.getRandom().nextInt(800, 1400));
            return;
        }

        boolean isMining = Client.getLocalPlayer() != null && Client.getLocalPlayer().getAnimationId() != -1;
        if (!isMining || isAdrenalineLow()) {
            if (rock != null && rock.interact("Mine")) {
                Execution.delay(script.getRandom().nextInt(1500, 3000));
            } else {
                Execution.delay(script.getRandom().nextInt(600, 1200));
            }
        } else {
            Execution.delay(script.getRandom().nextInt(800, 1400));
        }
    }
    
    void handleGoUpstairs() {
        // Only transition if we have reached upstairs (simulate by successful exit interaction)
        ScriptConsole.println("[ClayUrnTask] Going upstairs...");
        
        // Basic interface handling for dialogs
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        // Look for cave exit to go upstairs
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Cave").option("Exit").results();
        SceneObject caveExit = results.nearest();
        
        if (caveExit != null && caveExit.interact("Exit")) {
            ScriptConsole.println("[ClayUrnTask] Exiting cave, transitioning to soften clay");
            Execution.delay(script.getRandom().nextInt(4000, 5000));
            setStateToSoftenClay();
        }

        if(caveExit == null){
            setStateToSoftenClay();
        }
    }

    void handleSoftenClayAtSink() {
        ScriptConsole.println("[ClayUrnTask] Softening clay at sink...");
        // Basic interface handling for dialogs
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[ClayUrnTask] Sink interface is open, confirming softening clay");
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            return;
        }
        // Fill sink until no regular clay and all clay in backpack is soft clay
        if (!Backpack.contains("Clay")) {
            ScriptConsole.println("[ClayUrnTask] No regular clay in backpack, moving to spin urns");
            setStateToSpinUrns();
            return;
        }
        // Otherwise, interact with sink to fill
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Sink").hidden(false).option("Fill").results();
        SceneObject sink = results.nearest();
        if (sink != null) {
            if (sink.interact("Fill")) {
                ScriptConsole.println("[ClayUrnTask] Using sink to soften clay");
                Execution.delayUntil(10000, () -> Interfaces.isOpen(1370));
                return;
            }
        }
    }
    
    void handleSpinUrns() {
        // Only spin urns if we have enough soft clay for the selected urn
        if (!isDataLoaded() || selectedUrn == null) {
            ScriptConsole.println("[ClayUrnTask] No urn selected or data not loaded");
            Execution.delay(1000);
            return;
        }

        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }

        // Check for at least 2 soft clay before attempting to craft
        int softClayCount = Backpack.getItems().stream()
                .filter(item -> item != null && "Soft clay".equals(item.getName()))
                .mapToInt(item -> item.getStackSize())
                .sum();
        if (softClayCount < 2) {
            ScriptConsole.println("[ClayUrnTask] Not enough soft clay (need at least 2), transitioning to mining.");
            setStateToFireUrns();
            return;
        }

        // Check if pottery wheel interface is open
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[ClayUrnTask] Pottery wheel interface is open, handling urn selection");
            handlePotteryWheelInterface();
        } else {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Pottery Wheel").hidden(false).option("Form").results();
            SceneObject potteryWheel = results.nearest();
            if (potteryWheel != null) {
                ScriptConsole.println("[ClayUrnTask] Found pottery wheel, attempting to open interface");
                if (potteryWheel.interact("Form")) {
                    ScriptConsole.println("[ClayUrnTask] Interacting with pottery wheel");
                    Execution.delay(script.getRandom().nextInt(1500, 3000));
                }
            } else {
                ScriptConsole.println("[ClayUrnTask] Pottery wheel not found");
                Execution.delay(script.getRandom().nextInt(800, 1200));
            }
        }
    }
    
    void handleFireUrns() {
        ScriptConsole.println("[ClayUrnTask] Handling firing urns...");
        
        // Check if we have unfired urns to fire using pattern matching
        boolean hasUnfiredUrns = Backpack.getItems().stream()
                .anyMatch(item -> item != null && item.getName() != null && 
                        item.getName().toLowerCase().contains("unfired") && 
                        item.getName().toLowerCase().contains("urn"));
        
        if (!hasUnfiredUrns) {
            ScriptConsole.println("[ClayUrnTask] No unfired urns found, transitioning to add runes");
            setStateToAddRunes();
            return;
        }
        
        // Basic interface handling for dialogs
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }
        
        // Check if pottery oven interface is open
        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[ClayUrnTask] Pottery oven interface is open, confirming firing");
            confirmFiringUrns();
            return;
        }
        
        // Find and interact with pottery oven
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Pottery oven").option("Fire").results();
        SceneObject oven = results.nearest();
        
        if (oven != null) {
            ScriptConsole.println("[ClayUrnTask] Found pottery oven, attempting to fire urns");
            if (oven.interact("Fire")) {
                ScriptConsole.println("[ClayUrnTask] Interacting with pottery oven");
                Execution.delay(script.getRandom().nextInt(1500, 3000));
            }
        } else {
            ScriptConsole.println("[ClayUrnTask] Pottery oven not found");
            Execution.delay(script.getRandom().nextInt(800, 1200));
        }
    }
    
    private void confirmFiringUrns() {
        ScriptConsole.println("[ClayUrnTask] Confirming firing using dialogue action (component ID: 89784350)");
        
        if (MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350)) {
            ScriptConsole.println("[ClayUrnTask] Successfully started firing urns");
            // Wait for firing to complete
            Execution.delay(script.getRandom().nextInt(2000, 4000));
            
            // Check if we still have unfired urns using pattern matching
            boolean stillHasUnfiredUrns = Backpack.getItems().stream()
                    .anyMatch(item -> item != null && item.getName() != null && 
                            item.getName().toLowerCase().contains("unfired") && 
                            item.getName().toLowerCase().contains("urn"));
            
            if (!stillHasUnfiredUrns) {
                ScriptConsole.println("[ClayUrnTask] All urns fired, transitioning to deposit");
                setStateToDepositUrns();
            }
        } else {
            ScriptConsole.println("[ClayUrnTask] Failed to start firing using dialogue action");
        }
    }
    
    void handleAddRunesToUrns() {
        ScriptConsole.println("[ClayUrnTask] Adding runes to fired urns...");
        var urnItem = Backpack.getItems().stream()
            .filter(item -> item != null && item.getId() != -1 &&
                item.getName().toLowerCase().contains("no rune"))
            .findFirst().orElse(null);
        if (urnItem == null) {
            ScriptConsole.println("[ClayUrnTask] No fired urns needing runes found, transitioning to deposit");
            setStateToDepositUrns();
            return;
        }

        // Basic interface handling for dialogs
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000L, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(1370)) {
            ScriptConsole.println("[ClayUrnTask] Sink interface is open, confirming softening clay");
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            return;
        }

        // Get options from the urn item
        List<String> options = Objects.requireNonNull(urnItem.getConfigType()).getBackpackOptions();
        if (options == null || options.isEmpty()) {
            ScriptConsole.println("[ClayUrnTask] No options found for urn item");
            return;
        }
        String option = options.get(0);
        ScriptConsole.println("[ClayUrnTask] Interacting with urn: " + urnItem.getName() + " using option: " + option);
        boolean interacted = Backpack.interact(urnItem.getName(), option);
        if (!interacted) {
            ScriptConsole.println("[ClayUrnTask] Could not interact with urn to add rune");
            return;
        }
        Execution.delay(script.getRandom().nextInt(1200, 1800));
    }

    void handleDepositUrns() {
        ScriptConsole.println("[ClayUrnTask] Handling depositing urns...");

        // Find all urns with (empty) in their name
        Pattern urnPattern = Pattern.compile("urn.*\\(empty\\)", Pattern.CASE_INSENSITIVE);

        // Try to open deposit box if not already open
        if (!DepositBox.isOpen()) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Bank deposit box").hidden(false).option("Deposit").results();
            SceneObject depositBox = results.nearest();
            if (depositBox != null && depositBox.interact("Deposit")) {
                boolean opened = Execution.delayUntil(10000, () -> DepositBox.isOpen());
                if (!opened) {
                    ScriptConsole.println("[ClayUrnTask] Deposit box did not open in time");
                    return;
                }
            } else {
                ScriptConsole.println("[ClayUrnTask] Deposit box not found or failed to interact, using bank preset");
                Bank.loadLastPreset();
                Execution.delay(script.getRandom().nextInt(5000, 7000));
                return;
            }
        }

        ScriptConsole.println("[ClayUrnTask] Deposit box is open, attempting to deposit urns...");
        // Find all slots with urns matching the pattern
        List<Item> items = Backpack.getItems();
        List<Integer> urnSlots = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item != null && item.getName() != null && urnPattern.matcher(item.getName()).find()) {
                urnSlots.add(i);
            }
        }
        boolean allDeposited = true;
        for (int slotNum : urnSlots) {
            if (net.botwithus.rs3.game.inventories.Backpack.getSlot(slotNum) != null) {
                boolean interacted = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 4, slotNum, 720915);
                if (!interacted) {
                    ScriptConsole.println("[ClayUrnTask] Failed to interact with deposit box for slot " + slotNum);
                    allDeposited = false;
                    continue;
                }
                boolean removed = Execution.delayUntil(1000L, () -> net.botwithus.rs3.game.inventories.Backpack.getSlot(slotNum) == null);
                if (!removed) {
                    ScriptConsole.println("[ClayUrnTask] Urn in slot " + slotNum + " was not removed after deposit");
                    allDeposited = false;
                }
            }
        }
        if (allDeposited) {
            ScriptConsole.println("[ClayUrnTask] All urns should be deposited.");
        } else {
            ScriptConsole.println("[ClayUrnTask] Some urns were not deposited or an error occurred.");
        }
        // Print current backpack urns for debug
        List<String> remainingUrns = Backpack.getItems().stream()
            .filter(item -> item != null && item.getName() != null && item.getName().toLowerCase().contains("urn") && item.getName().toLowerCase().contains("(empty)"))
            .map(item -> item.getName())
            .toList();
        if (remainingUrns.isEmpty()) {
            ScriptConsole.println("[ClayUrnTask] No urns remain in backpack after deposit.");
            setStateToMineClay();
        } else {
            ScriptConsole.println("[ClayUrnTask] Urns still in backpack after deposit: " + remainingUrns);
        }
        Execution.delay(script.getRandom().nextInt(500, 1200));
    }

    private void handlePotteryWheelInterface() {

        // Get current category and item selection from VarManager
        int currentCategoryVar = VarManager.getVarValue(VarDomainType.PLAYER, 1169);
        int currentItemVar = VarManager.getVarValue(VarDomainType.PLAYER, 1170);
        
        ScriptConsole.println("[ClayUrnTask] Current category var: " + currentCategoryVar + ", current item var: " + currentItemVar);
        ScriptConsole.println("[ClayUrnTask] Target urn ID: " + selectedUrn.getId());
        
        // Check what urn name is currently displayed in component 13
        var currentUrnNameComponent = ComponentQuery.newQuery(1370).componentIndex(13).results().first();
        if (currentUrnNameComponent != null) {
            String currentUrnText = currentUrnNameComponent.getText();
            ScriptConsole.println("[ClayUrnTask] Currently displayed urn: " + currentUrnText);
        }
        
        // Check if correct category is selected
        if (currentCategoryVar != selectedCategory.getEnumId()) {
            ScriptConsole.println("[ClayUrnTask] Incorrect category selected, selecting correct category: " + selectedCategory);
            selectUrnCategory(selectedCategory);
            return;
        }
        
        // Check if correct urn is selected
        if (currentItemVar == selectedUrn.getId()) {
            ScriptConsole.println("[ClayUrnTask] Correct urn already selected: " + selectedUrn.getDisplayName());
            
            // Check if we can make the urn (similar to herblore checking var 8847)
            int canMakeVar = VarManager.getVarValue(VarDomainType.PLAYER, 8847);
            ScriptConsole.println("[ClayUrnTask] Can make urn check (var 8847): " + canMakeVar);
            
            if (canMakeVar <= 0) {
                ScriptConsole.println("[ClayUrnTask] Cannot make urn, no materials or requirements not met");
                Execution.delay(script.getRandom().nextInt(800, 1200));
                return;
            }
            
            // Proceed with crafting
            if (craftUrn()) {
                // Wait for crafting to complete
                Execution.delay(script.getRandom().nextInt(2000, 4000));
                // Check if we still have soft clay
                if (!Backpack.contains("Soft clay")) {
                    ScriptConsole.println("[ClayUrnTask] No more soft clay, transitioning to firing");
                    setStateToFireUrns();
                } else {
                    // If we still have enough soft clay for another urn, stay in SPIN_URNS
                    int requiredSoftClay = getRequiredSoftClayForUrn(selectedUrn);
                    int softClayCount = Backpack.getItems().stream()
                        .filter(item -> item != null && "Soft clay".equals(item.getName()))
                        .mapToInt(item -> item.getStackSize())
                        .sum();
                    if (softClayCount < requiredSoftClay) {
                        ScriptConsole.println("[ClayUrnTask] Not enough soft clay for another urn, transitioning to firing");
                        setStateToFireUrns();
                    }
                }
            }
        } else {
            ScriptConsole.println("[ClayUrnTask] Incorrect urn selected, selecting correct urn: " + selectedUrn.getDisplayName());
            selectUrnItem(selectedUrn);
        }
    }

    private void selectUrnCategory(UrnCategory category) {
        // Only log if interacting with interface 1370
        int dropdownComponentId = 89849884;
        if (dropdownComponentId == 1370) {
            ScriptConsole.println("[ClayUrnTask] Selecting category: " + category);
        }
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, dropdownComponentId);
        Execution.delay(script.getRandom().nextInt(100, 200));
        int categoryIndex = category.getIndex();
        int componentIndex = (categoryIndex * 2) + 1;
        int categoryComponentId = 96797588;
        if (categoryComponentId == 1370) {
            ScriptConsole.println("[ClayUrnTask] Selected category: " + category + " at index: " + categoryIndex);
        }
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, componentIndex, categoryComponentId);
        Execution.delay(script.getRandom().nextInt(300, 800));
    }

    private void selectUrnItem(UrnType urn) {
        // Only log if interacting with interface 1370
        int categoryComponentId = 96797588;
        if (categoryComponentId == 1370) {
            ScriptConsole.println("[ClayUrnTask] Attempting to select urn: " + urn.getDisplayName() + " (ID: " + urn.getId() + ")");
        }
        int currentCategoryVar = VarManager.getVarValue(VarDomainType.PLAYER, 1169);
        EnumType categoryEnum = ConfigManager.getEnumType(currentCategoryVar);
        if (categoryEnum == null || categoryEnum.getOutputs() == null) {
            if (categoryComponentId == 1370) {
                ScriptConsole.println("[ClayUrnTask] Error: Could not get category enum for var " + currentCategoryVar);
            }
            return;
        }
        
        // Find the urn's index in the category enum
        int urnEnumIndex = -1;
        for (int i = 0; i < categoryEnum.getOutputs().size(); i++) {
            Object output = categoryEnum.getOutputs().get(i);
            if (output instanceof Integer && (Integer) output == urn.getId()) {
                urnEnumIndex = i;
                break;
            }
        }
        
        if (urnEnumIndex == -1) {
            ScriptConsole.println("[ClayUrnTask] Error: Could not find urn " + urn.getId() + " in category enum");
            return;
        }
        
        ScriptConsole.println("[ClayUrnTask] Found urn at enum index: " + urnEnumIndex);
        
        // Use the same formula as herblore: (index * 4) + 1
        int componentIndex = (urnEnumIndex * 4) + 1;
        ScriptConsole.println("[ClayUrnTask] Using component index: " + componentIndex + " (enum index " + urnEnumIndex + " * 4 + 1) with component ID: 89849878");
        
        // Use MiniMenu.interact directly with the component ID and calculated component index
        if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, componentIndex, 89849878)) {
            ScriptConsole.println("[ClayUrnTask] MiniMenu.interact returned true, verifying selection...");
            
            // Wait a moment for the selection to take effect
            Execution.delay(script.getRandom().nextInt(300, 600));
            
            // Verify the selection actually worked by checking the current item var
            int newCurrentItemVar = VarManager.getVarValue(VarDomainType.PLAYER, 1170);
            ScriptConsole.println("[ClayUrnTask] After selection, current item var: " + newCurrentItemVar + " (target: " + urn.getId() + ")");
            
            if (newCurrentItemVar == urn.getId()) {
                ScriptConsole.println("[ClayUrnTask] Successfully selected urn: " + urn.getDisplayName() + " using enum index " + urnEnumIndex);
                Execution.delay(script.getRandom().nextInt(300, 800));
                return;
            } else {
                ScriptConsole.println("[ClayUrnTask] Selection failed - current item var did not change to target urn ID");
                // Try again with a different approach or log error
                return;
            }
        } else {
            ScriptConsole.println("[ClayUrnTask] Failed to select urn using MiniMenu.interact");
        }
        
        Execution.delay(script.getRandom().nextInt(300, 800));
    }

    private boolean craftUrn() {
        ScriptConsole.println("[ClayUrnTask] Starting urn crafting using dialogue action (component ID: 89784350)");
        
        if (MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350)) {
            ScriptConsole.println("[ClayUrnTask] Successfully started crafting urn");
            return true;
        } else {
            ScriptConsole.println("[ClayUrnTask] Failed to start crafting using dialogue action");
        }
        return false;
    }

    // Getters and setters for GUI configuration
    public UrnCategory getSelectedCategory() {
        return selectedCategory;
    }
    
    public void setSelectedCategory(UrnCategory category) {
        if (category != null && availableCategories.contains(category)) {
            this.selectedCategory = category;
            // Update selected urn to match the category if needed
            if (selectedUrn != null && selectedUrn.getCategory() != category) {
                // Find first urn in the new category
                for (UrnType urn : availableUrns) {
                    if (urn.getCategory() == category) {
                        selectedUrn = urn;
                        break;
                    }
                }
            }
        }
    }
    
    public UrnType getSelectedUrn() {
        return selectedUrn;
    }
    
    public void setSelectedUrn(UrnType urn) {
        if (urn != null && availableUrns.contains(urn)) {
            this.selectedUrn = urn;
            this.selectedCategory = urn.getCategory();
        }
    }
    
    public UrnCategory[] getAvailableCategories() {
        if (availableCategories == null) {
            return new UrnCategory[0];
        }
        return availableCategories.toArray(new UrnCategory[0]);
    }
    
    public UrnType[] getUrnsInCategory(UrnCategory category) {
        if (availableUrns == null || category == null) {
            return new UrnType[0];
        }
        return availableUrns.stream()
                .filter(urn -> urn.getCategory() == category)
                .toArray(UrnType[]::new);
    }
    
    public UrnType[] getAllAvailableUrns() {
        if (availableUrns == null) {
            return new UrnType[0];
        }
        return availableUrns.toArray(new UrnType[0]);
    }
    
    public boolean isDataLoaded() {
        return availableCategories != null && availableUrns != null && 
               !availableCategories.isEmpty() && !availableUrns.isEmpty();
    }
    
    public void reloadUrnData() {
        ScriptConsole.println("[ClayUrnTask] Reloading urn data...");
        loadUrnData();
        if (isDataLoaded()) {
            ScriptConsole.println("[ClayUrnTask] Successfully reloaded urn data");
        } else {
            ScriptConsole.println("[ClayUrnTask] Failed to reload urn data");
        }
    }
    
    public String getStatus() {
        if (!isDataLoaded()) {
            return "Data not loaded";
        }
        if (selectedCategory == null || selectedUrn == null) {
            return "No urn selected";
        }
        return "Selected: " + selectedUrn.getDisplayName() + " (" + selectedCategory.getDisplayName() + ")";
    }
    
    public boolean setSelectedUrnById(int urnId) {
        if (!isDataLoaded()) {
            ScriptConsole.println("[ClayUrnTask] Cannot set urn by ID: data not loaded");
            return false;
        }
        
        for (UrnType urn : availableUrns) {
            if (urn.getId() == urnId) {
                setSelectedUrn(urn);
                ScriptConsole.println("[ClayUrnTask] Set urn by ID: " + urn.getDisplayName());
                return true;
            }
        }
        
        ScriptConsole.println("[ClayUrnTask] Urn with ID " + urnId + " not found in available urns");
        return false;
    }
    
    public void printAvailableUrns() {
        if (!isDataLoaded()) {
            ScriptConsole.println("[ClayUrnTask] No urn data available");
            return;
        }
        
        ScriptConsole.println("[ClayUrnTask] Available urn categories:");
        for (UrnCategory category : availableCategories) {
            ScriptConsole.println("  " + category.getIndex() + ": " + category.getDisplayName() + " (enum " + category.getEnumId() + ")");
        }
        
        ScriptConsole.println("[ClayUrnTask] Available urn types:");
        for (UrnType urn : availableUrns) {
            ScriptConsole.println("  " + urn.getId() + ": " + urn.getDisplayName() + " (" + urn.getCategory().getDisplayName() + ")");
        }
    }

    private boolean attemptEnterUnderground() {
        SceneObject entrance = SceneObjectQuery.newQuery()
                .name("Cave entrance")
                .option("Enter")
                .hidden(false)
                .results()
                .nearest();
        if (entrance != null) {
            if (entrance.interact("Enter")) {
                Execution.delay(script.getRandom().nextInt(1200, 2200));
                return true;
            }
        }
        return false;
    }

    private boolean isAdrenalineLow() {
        try {
            if (Client.getLocalPlayer() == null) return true;
            for (Headbar headbar : Client.getLocalPlayer().getHeadbars()) {
                if (headbar.getId() == 5) { // adrenaline bar
                    return headbar.getWidth() <= 10; // close to bottom
                }
            }
        } catch (Exception ignored) {}
        return true;
    }

    private int getRequiredSoftClayForUrn(UrnType urn) {
        ItemType itemType = ConfigManager.getItemType(urn.getId());
        if (itemType == null) return 0;
        return itemType.getIntParam(2665); // craft_quantity_1
    }
}
