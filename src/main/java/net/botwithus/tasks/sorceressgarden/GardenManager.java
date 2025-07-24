package net.botwithus.tasks.sorceressgarden;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.tasks.sorceressgarden.gardens.BaseGarden;
import net.botwithus.tasks.sorceressgarden.models.GardenState;
import net.botwithus.tasks.sorceressgarden.models.GardenType;

/**
 * Manages garden state and transitions in Sorceress's Garden
 */
public class GardenManager {
    private final CoaezUtility script;
    private final Map<GardenType, BaseGarden> gardens;
    private GardenType currentGarden;
    private GardenState currentState;
    private Set<GardenType> selectedGardens;
    
    public GardenManager(CoaezUtility script) {
        this.script = script;
        this.gardens = new HashMap<>();
        this.currentGarden = null;
        this.currentState = GardenState.IDLE;
        this.selectedGardens = Set.of();
    }
    
    /**
     * Register a garden implementation
     */
    public void registerGarden(BaseGarden garden) {
        if (garden != null) {
            gardens.put(garden.getGardenType(), garden);
        }
    }
    
    /**
     * Execute the current garden logic
     */
    public void executeCurrentGarden() {
        if (currentGarden == null) {
            ScriptConsole.println("No garden selected, selecting next garden");
            selectNextGarden();
            return;
        }

        BaseGarden garden = gardens.get(currentGarden);
        if (garden != null) {
            ScriptConsole.println("Executing garden: " + currentGarden.getDisplayName());
            garden.execute();
            
            if (garden.isCompleted()) {
                ScriptConsole.println("Garden completed: " + currentGarden.getDisplayName());
                onGardenCompleted();
            } else if (garden.hasFailed()) {
                ScriptConsole.println("Garden failed: " + currentGarden.getDisplayName());
                onGardenFailed();
            }
        } else {
            ScriptConsole.println("No implementation found for garden: " + currentGarden);
            currentState = GardenState.FAILED;
        }
    }
    
    /**
     * Select the next garden to execute
     */
    private void selectNextGarden() {
        if (selectedGardens.isEmpty()) {
            ScriptConsole.println("No gardens selected for execution");
            return;
        }
        GardenType firstGarden = selectedGardens.iterator().next();
        ScriptConsole.println("Repeating selected garden: " + firstGarden.getDisplayName());
        switchToGarden(firstGarden);
    }
    
    /**
     * Switch to a specific garden
     */
    public void switchToGarden(GardenType gardenType) {
        if (gardenType == null) return;
        
        this.currentGarden = gardenType;
        this.currentState = GardenState.ENTERING;
        
        BaseGarden garden = gardens.get(gardenType);
        if (garden != null) {
            garden.reset();
        }
        
        ScriptConsole.println("Switched to garden: " + gardenType.getDisplayName());
    }
    
    /**
     * Handle garden completion
     */
    private void onGardenCompleted() {
        ScriptConsole.println("Completed garden: " + currentGarden.getDisplayName());
        currentState = GardenState.COMPLETED;
        
        // Mark garden as recently completed
        BaseGarden garden = gardens.get(currentGarden);
        if (garden != null) {
            garden.markAsCompleted();
        }
        
        // Move to next garden
        currentGarden = null;
        currentState = GardenState.IDLE;
    }
    
    /**
     * Handle garden failure
     */
    private void onGardenFailed() {
        ScriptConsole.println("Failed garden: " + currentGarden.getDisplayName());
        currentState = GardenState.FAILED;
        
        // Reset current garden
        BaseGarden garden = gardens.get(currentGarden);
        if (garden != null) {
            garden.reset();
        }
        
        // Move to next garden
        currentGarden = null;
        currentState = GardenState.IDLE;
    }
    
    /**
     * Reset garden completion status
     */
    private void resetGardenCompletion() {
        for (BaseGarden garden : gardens.values()) {
            garden.resetCompletion();
        }
    }
    
    /**
     * Set the selected gardens
     */
    public void setSelectedGardens(Set<GardenType> selectedGardens) {
        this.selectedGardens = selectedGardens;
        ScriptConsole.println("Selected gardens: " + selectedGardens);
    }
    
    /**
     * Get the current garden
     */
    public GardenType getCurrentGarden() {
        return currentGarden;
    }
    
    /**
     * Get the current state
     */
    public GardenState getCurrentState() {
        return currentState;
    }
    
    /**
     * Get the selected gardens
     */
    public Set<GardenType> getSelectedGardens() {
        return selectedGardens;
    }
    
    /**
     * Check if any garden is currently active
     */
    public boolean isGardenActive() {
        return currentGarden != null && currentState != GardenState.IDLE;
    }
    
    /**
     * Get progress information for the current garden
     */
    public String getCurrentProgress() {
        if (currentGarden == null) {
            return "No garden active";
        }
        
        BaseGarden garden = gardens.get(currentGarden);
        if (garden == null) {
            return "Unknown garden: " + currentGarden.getDisplayName();
        }
        
        return String.format("%s - %s (%.1f%%)", 
                           currentGarden.getDisplayName(), 
                           currentState.getDisplayName(),
                           garden.getProgressPercentage());
    }
    
    /**
     * Stop current garden execution
     */
    public void stop() {
        if (currentGarden != null) {
            BaseGarden garden = gardens.get(currentGarden);
            if (garden != null) {
                garden.stop();
            }
        }
        
        currentGarden = null;
        currentState = GardenState.IDLE;
        ScriptConsole.println("Stopped garden execution");
    }
    
    /**
     * Reset all gardens
     */
    public void reset() {
        for (BaseGarden garden : gardens.values()) {
            garden.reset();
        }
        
        currentGarden = null;
        currentState = GardenState.IDLE;
        ScriptConsole.println("Reset all gardens");
    }
    
    /**
     * Get a specific garden implementation
     */
    public BaseGarden getGarden(GardenType gardenType) {
        return gardens.get(gardenType);
    }
    
    /**
     * Check if a garden type is available
     */
    public boolean hasGarden(GardenType gardenType) {
        return gardens.containsKey(gardenType);
    }
    
    @Override
    public String toString() {
        return String.format("GardenManager{currentGarden=%s, currentState=%s, selectedGardens=%d}", 
                           currentGarden, currentState, selectedGardens.size());
    }
} 