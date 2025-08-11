package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.clayurn.*;

/**
 * Refactored ClayUrnTask with proper OOP structure
 * 
 * This class now follows better OOP principles:
 * - Single Responsibility: Each helper class handles one specific concern
 * - Dependency Injection: Dependencies are injected rather than created
 * internally
 * - Composition over Inheritance: Uses composition with helper classes
 * - State Pattern: Clean state management with separate state classes
 * - Encapsulation: Data and behavior are properly encapsulated
 */
public class ClayUrnTaskRefactored implements Task {
    private final CoaezUtility script;

    // Core components using composition
    private final UrnDataManager dataManager;
    private final UrnQueue urnQueue;
    private final UrnCraftingManager craftingManager;
    private final GameObjectInteractionHelper objectHelper;

    // State management
    private UrnTaskState currentState;
    private final UrnTaskState mineClayState = new MineClayState();
    private final UrnTaskState goUpstairsState = new GoUpstairsState();
    private final UrnTaskState softenClayState = new SoftenClayState();
    private final UrnTaskState spinUrnsState = new SpinUrnsState();
    private final UrnTaskState fireUrnsState = new FireUrnsState();
    private final UrnTaskState addRunesState = new AddRunesState();
    private final UrnTaskState depositUrnsState = new DepositUrnsState();

    // Configuration
    private UrnCategory selectedCategory;
    private UrnType selectedUrn;
    private boolean skipAddRunes = false;

    public ClayUrnTaskRefactored(CoaezUtility script) {
        this.script = script;
        this.dataManager = new UrnDataManager();
        this.urnQueue = new UrnQueue();
        this.craftingManager = new UrnCraftingManager(script);
        this.objectHelper = new GameObjectInteractionHelper(script);

        // Initialize data and set default state
        initializeData();
        currentState = mineClayState;
    }

    private void initializeData() {
        dataManager.loadUrnData();
        if (dataManager.isDataLoaded()) {
            var categories = dataManager.getAvailableCategories();
            var urns = dataManager.getAvailableUrns();

            if (!categories.isEmpty()) {
                selectedCategory = categories.get(0);
                ScriptConsole
                        .println("[ClayUrnTaskRefactored] Set default category: " + selectedCategory.getDisplayName());

                if (!urns.isEmpty()) {
                    selectedUrn = urns.get(0);
                    ScriptConsole.println("[ClayUrnTaskRefactored] Set default urn: " + selectedUrn.getDisplayName());
                }
            }
        }
    }

    @Override
    public void execute() {
        if (!dataManager.isDataLoaded()) {
            ScriptConsole.println("[ClayUrnTaskRefactored] Urn data not yet loaded, attempting to load...");
            dataManager.loadUrnData();
            if (!dataManager.isDataLoaded()) {
                ScriptConsole.println("[ClayUrnTaskRefactored] Failed to load urn data, cannot proceed");
                Execution.delay(1000);
                return;
            }
        }
        currentState.handle(this);
    }

    // State transition methods
    public void setStateToMineClay() {
        currentState = mineClayState;
    }

    public void setStateToGoUpstairs() {
        currentState = goUpstairsState;
    }

    public void setStateToSoftenClay() {
        currentState = softenClayState;
    }

    public void setStateToSpinUrns() {
        currentState = spinUrnsState;
    }

    public void setStateToFireUrns() {
        currentState = fireUrnsState;
    }

    public void setStateToAddRunes() {
        currentState = addRunesState;
    }

    public void setStateToDepositUrns() {
        currentState = depositUrnsState;
    }

    // Getters for components (used by state classes)
    public CoaezUtility getScript() {
        return script;
    }

    public UrnDataManager getUrnDataManager() {
        return dataManager;
    }

    public UrnQueue getUrnQueue() {
        return urnQueue;
    }

    public UrnCraftingManager getCraftingManager() {
        return craftingManager;
    }

    public GameObjectInteractionHelper getObjectHelper() {
        return objectHelper;
    }

    // Configuration getters and setters
    public UrnCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(UrnCategory category) {
        if (category != null && dataManager.getAvailableCategories().contains(category)) {
            this.selectedCategory = category;
            // Update selected urn to match the category if needed
            if (selectedUrn != null && !selectedUrn.getCategory().equals(category)) {
                var urnsInCategory = dataManager.getUrnsInCategory(category);
                if (!urnsInCategory.isEmpty()) {
                    selectedUrn = urnsInCategory.get(0);
                }
            }
        }
    }

    public UrnType getSelectedUrn() {
        return selectedUrn;
    }

    public void setSelectedUrn(UrnType urn) {
        if (urn != null && dataManager.getAvailableUrns().contains(urn)) {
            this.selectedUrn = urn;
            this.selectedCategory = urn.getCategory();
        }
    }

    public boolean isSkipAddRunes() {
        return skipAddRunes;
    }

    public void setSkipAddRunes(boolean skipAddRunes) {
        this.skipAddRunes = skipAddRunes;
    }

    // Public API methods for external use
    public void queueUrn(UrnType urnType, int quantity) {
        urnQueue.addUrn(urnType, quantity);
    }

    public void removeUrnFromQueue(UrnType urnType) {
        urnQueue.removeUrn(urnType);
    }

    public void onUrnCrafted(UrnType urnType) {
        urnQueue.onUrnCrafted(urnType);
    }

    public void clearUrnQueue() {
        urnQueue.clear();
    }

    public boolean setSelectedUrnById(int urnId) {
        UrnType urn = dataManager.getUrnById(urnId);
        if (urn != null) {
            setSelectedUrn(urn);
            ScriptConsole.println("[ClayUrnTaskRefactored] Set urn by ID: " + urn.getDisplayName());
            return true;
        }

        ScriptConsole.println("[ClayUrnTaskRefactored] Urn with ID " + urnId + " not found in available urns");
        return false;
    }

    public String getStatus() {
        if (!dataManager.isDataLoaded()) {
            return "Data not loaded";
        }
        if (selectedCategory == null || selectedUrn == null) {
            return "No urn selected";
        }
        return "Selected: " + selectedUrn.getDisplayName() + " (" + selectedCategory.getDisplayName() + ")";
    }

    public void printAvailableUrns() {
        dataManager.printAvailableUrns();
    }

    public void reloadUrnData() {
        ScriptConsole.println("[ClayUrnTaskRefactored] Reloading urn data...");
        dataManager.loadUrnData();
        if (dataManager.isDataLoaded()) {
            ScriptConsole.println("[ClayUrnTaskRefactored] Successfully reloaded urn data");
            initializeData(); // Re-initialize selections
        } else {
            ScriptConsole.println("[ClayUrnTaskRefactored] Failed to reload urn data");
        }
    }

    // Convenience methods for getting data arrays (for GUI compatibility)
    public UrnCategory[] getAvailableCategories() {
        var categories = dataManager.getAvailableCategories();
        return categories.toArray(new UrnCategory[0]);
    }

    public UrnType[] getUrnsInCategory(UrnCategory category) {
        var urns = dataManager.getUrnsInCategory(category);
        return urns.toArray(new UrnType[0]);
    }

    public UrnType[] getAllAvailableUrns() {
        var urns = dataManager.getAvailableUrns();
        return urns.toArray(new UrnType[0]);
    }
}