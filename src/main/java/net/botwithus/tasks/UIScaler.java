package net.botwithus.tasks;

import net.botwithus.rs3.game.cs2.ScriptBuilder;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.cs2.ReturnValue;
import net.botwithus.rs3.game.cs2.layouts.Layout;

import java.util.List;

/**
 * UIScaler class for calculating scaled interface coordinates and dimensions.
 * Handles UI scaling for drawing around specific interfaces and dialogs.
 */
public class UIScaler {
    
    private int windowWidth;
    private int windowHeight;
    private int rawCanvasWidth;
    private int rawCanvasHeight;
    private double scaleX;
    private double scaleY;
    private boolean initialized = false;
    
    /**
     * Result class containing scaled interface coordinates and dimensions.
     */
    public static class InterfaceRect {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        
        public InterfaceRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        @Override
        public String toString() {
            return String.format("InterfaceRect{x=%d, y=%d, width=%d, height=%d}", x, y, width, height);
        }
    }
    
    /**
     * Initializes or updates the UI scaling information.
     * This should be called before getting interface coordinates.
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        try {
            // Get canvas information
            ScriptBuilder getCanvas = ScriptBuilder
                    .of(14244)
                    .returns(Layout.INT, Layout.INT, Layout.INT, Layout.INT);
            List<ReturnValue> canvas = getCanvas.invokeExact();
            
            rawCanvasWidth = canvas.get(2).asInt();
            rawCanvasHeight = canvas.get(3).asInt();
            
            // Get window dimensions
            windowWidth = VarManager.getVarc(8267);
            windowHeight = VarManager.getVarc(8268);
            
            // Calculate scale factors
            scaleX = (double) windowWidth / (double) rawCanvasWidth;
            scaleY = (double) windowHeight / (double) rawCanvasHeight;
            
            initialized = true;
            
            ScriptConsole.println(String.format("[UIScaler] Initialized - Window: %dx%d, Canvas: %dx%d, Scale: %.3fx%.3f", 
                                               windowWidth, windowHeight, rawCanvasWidth, rawCanvasHeight, scaleX, scaleY));
            
            return true;
            
        } catch (Exception e) {
            ScriptConsole.println("[UIScaler] Failed to initialize: " + e.getMessage());
            initialized = false;
            return false;
        }
    }
    
    /**
     * Gets the scaled coordinates and dimensions for a specific interface component.
     * @param interfaceId The interface ID (parent component)
     * @param childId The child component ID (-1 for no child)
     * @return InterfaceRect containing scaled coordinates and dimensions, or null if failed
     */
    public InterfaceRect getInterfaceRect(int interfaceId, int childId) {
        if (!initialized && !initialize()) {
            return null;
        }
        
        try {
            ScriptBuilder interfaceScript = ScriptBuilder
                    .of(12613)
                    .args(Layout.INT, Layout.INT)
                    .returns(Layout.INT, Layout.INT, Layout.INT, Layout.INT);
            
            // Pack the interface ID and child ID into a single value
            int packed = (interfaceId << 16) | (childId & 0xFFFF);
            
            // For the second parameter, use 0 if childId is -1, otherwise use childId
            int secondParam = (childId == -1) ? 0 : childId;
            
            List<ReturnValue> raw = interfaceScript.invokeExact(packed, secondParam);
            
            int rawX = raw.get(0).asInt();
            int rawY = raw.get(1).asInt();
            int rawWidth = raw.get(2).asInt();
            int rawHeight = raw.get(3).asInt();
            
            // Apply scaling
            int scaledX = (int) Math.round(rawX * scaleX);
            int scaledY = (int) Math.round(rawY * scaleY);
            int scaledWidth = (int) Math.round(rawWidth * scaleX);
            int scaledHeight = (int) Math.round(rawHeight * scaleY);
            
            InterfaceRect result = new InterfaceRect(scaledX, scaledY, scaledWidth, scaledHeight);
            
            ScriptConsole.println(String.format("[UIScaler] Interface %d:%d - Raw: (%d,%d,%d,%d) -> Scaled: %s", 
                                               interfaceId, childId, rawX, rawY, rawWidth, rawHeight, result));
            
            return result;
            
        } catch (Exception e) {
            ScriptConsole.println(String.format("[UIScaler] Failed to get interface rect for %d:%d - %s", 
                                               interfaceId, childId, e.getMessage()));
            return null;
        }
    }
    
    /**
     * Gets the scaled coordinates and dimensions for a specific interface component.
     * Convenience method for when there's no child component.
     * @param interfaceId The interface ID
     * @return InterfaceRect containing scaled coordinates and dimensions, or null if failed
     */
    public InterfaceRect getInterfaceRect(int interfaceId) {
        return getInterfaceRect(interfaceId, -1);
    }
    
    /**
     * Gets the current window dimensions.
     * @return InterfaceRect with window width and height (x and y will be 0)
     */
    public InterfaceRect getWindowSize() {
        if (!initialized && !initialize()) {
            return null;
        }
        return new InterfaceRect(0, 0, windowWidth, windowHeight);
    }
    
    /**
     * Gets the current UI scale factors.
     * @return double array with [scaleX, scaleY], or null if not initialized
     */
    public double[] getScaleFactors() {
        if (!initialized) {
            return null;
        }
        return new double[]{scaleX, scaleY};
    }
    
    /**
     * Checks if the UIScaler has been initialized.
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Forces a re-initialization of the UI scaling information.
     * Useful when the game window has been resized.
     * @return true if re-initialization was successful, false otherwise
     */
    public boolean refresh() {
        initialized = false;
        return initialize();
    }
    
    /**
     * Prints debug information about the current UI scaling state.
     */
    public void printDebugInfo() {
        if (!initialized) {
            ScriptConsole.println("[UIScaler] Not initialized");
            return;
        }
        
        ScriptConsole.println("[UIScaler] === Debug Info ===");
        ScriptConsole.println("Window Size: " + windowWidth + " x " + windowHeight);
        ScriptConsole.println("Canvas Size: " + rawCanvasWidth + " x " + rawCanvasHeight);
        ScriptConsole.println("Scale Factors: " + String.format("%.3f x %.3f", scaleX, scaleY));
        ScriptConsole.println("[UIScaler] =================");
    }
    
    /**
     * Alternative method to get interface coordinates using a different approach.
     * This might be more accurate for certain types of interfaces like dialogs.
     * @param interfaceId The interface ID
     * @param childId The child component ID
     * @return InterfaceRect containing coordinates and dimensions, or null if failed
     */
    public InterfaceRect getInterfaceRectAlternative(int interfaceId, int childId) {
        if (!initialized && !initialize()) {
            return null;
        }
        
        try {
            // Try using script 12610 which might handle dialog components better
            ScriptBuilder altScript = ScriptBuilder
                    .of(12610)
                    .args(Layout.INT)
                    .returns(Layout.INT, Layout.INT, Layout.INT, Layout.INT);
            
            int packed = (interfaceId << 16) | (childId & 0xFFFF);
            List<ReturnValue> raw = altScript.invokeExact(packed);
            
            int rawX = raw.get(0).asInt();
            int rawY = raw.get(1).asInt();
            int rawWidth = raw.get(2).asInt();
            int rawHeight = raw.get(3).asInt();
            
            // Apply scaling
            int scaledX = (int) Math.round(rawX * scaleX);
            int scaledY = (int) Math.round(rawY * scaleY);
            int scaledWidth = (int) Math.round(rawWidth * scaleX);
            int scaledHeight = (int) Math.round(rawHeight * scaleY);
            
            InterfaceRect result = new InterfaceRect(scaledX, scaledY, scaledWidth, scaledHeight);
            
            ScriptConsole.println(String.format("[UIScaler] Alternative Interface %d:%d - Raw: (%d,%d,%d,%d) -> Scaled: %s", 
                                               interfaceId, childId, rawX, rawY, rawWidth, rawHeight, result));
            
            return result;
            
        } catch (Exception e) {
            ScriptConsole.println(String.format("[UIScaler] Alternative method failed for %d:%d - %s", 
                                               interfaceId, childId, e.getMessage()));
            return null;
        }
    }
    
    /**
     * Gets interface coordinates with fallback methods for better accuracy.
     * Tries the primary method first, then falls back to alternative if needed.
     * @param interfaceId The interface ID
     * @param childId The child component ID
     * @return InterfaceRect containing coordinates and dimensions, or null if all methods failed
     */
    public InterfaceRect getInterfaceRectWithFallback(int interfaceId, int childId) {
        // Try primary method first
        InterfaceRect result = getInterfaceRect(interfaceId, childId);
        
        if (result != null) {
            return result;
        }
        
        ScriptConsole.println(String.format("[UIScaler] Primary method failed for %d:%d, trying alternative", interfaceId, childId));
        
        // Try alternative method
        result = getInterfaceRectAlternative(interfaceId, childId);
        
        if (result != null) {
            return result;
        }
        
        ScriptConsole.println(String.format("[UIScaler] All methods failed for %d:%d", interfaceId, childId));
        return null;
    }
} 