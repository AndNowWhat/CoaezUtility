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
            // Get canvas information using script 14244
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
            
            ScriptConsole.println("Window Size = " + windowWidth + " x " + windowHeight);
            ScriptConsole.println("UI Scale = " + scaleX + " x " + scaleY);
            
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
            ScriptConsole.println("[UIScaler] Not initialized and failed to initialize");
            return null;
        }
        
        try {
            // Use script 12613 to get interface coordinates
            ScriptBuilder inv = ScriptBuilder
                    .of(12613)
                    .args(Layout.INT, Layout.INT)
                    .returns(Layout.INT, Layout.INT, Layout.INT, Layout.INT);
            
            // For dialog options, we need to handle the child ID properly
            int packed;
            int childIdx;
            
            if (childId == -1) {
                // No child component, use the interface ID directly
                packed = interfaceId << 16;
                childIdx = -1;
            } else {
                // For dialog options, try different packing approaches
                packed = (interfaceId << 16) | (childId & 0xFFFF);
                childIdx = childId;
            }
            
            List<ReturnValue> raw = inv.invokeExact(packed, childIdx);
            
            int rawX = raw.get(0).asInt();
            int rawY = raw.get(1).asInt();
            int rawWidth = raw.get(2).asInt();
            int rawHeight = raw.get(3).asInt();
            
            // Check if we got valid coordinates
            if (rawX < 0 || rawY < 0 || rawWidth <= 0 || rawHeight <= 0) {
                ScriptConsole.println(String.format("[UIScaler] Interface %d:%d returned invalid coordinates: x=%d, y=%d, w=%d, h=%d", 
                                                   interfaceId, childId, rawX, rawY, rawWidth, rawHeight));
                return null;
            }
            
            // Apply scaling
            int scaledX = (int) Math.round(rawX * scaleX);
            int scaledY = (int) Math.round(rawY * scaleY);
            int scaledWidth = (int) Math.round(rawWidth * scaleX);
            int scaledHeight = (int) Math.round(rawHeight * scaleY);
            
            InterfaceRect result = new InterfaceRect(scaledX, scaledY, scaledWidth, scaledHeight);
            
            ScriptConsole.println(String.format("[UIScaler] Interface %d:%d - Raw: x=%d, y=%d, w=%d, h=%d | Scaled: x=%d, y=%d, w=%d, h=%d", 
                                               interfaceId, childId, rawX, rawY, rawWidth, rawHeight, 
                                               scaledX, scaledY, scaledWidth, scaledHeight));
            
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
     * Gets the current window dimensions.
     * @return InterfaceRect with window width and height (x and y will be 0)
     */
    public InterfaceRect getWindowSize() {
        if (!initialized && !initialize()) {
            return null;
        }
        return new InterfaceRect(0, 0, windowWidth, windowHeight);
    }
} 