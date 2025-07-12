package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.hud.interfaces.UseFlag;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.scene.entities.characters.Hitmark;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.Client;

import java.util.concurrent.CompletableFuture;

public class MapNavigatorTask implements Task {
    private final CoaezUtility script;
    private Coordinate targetCoordinate;
    private String markerName;
    private boolean isNavigating = false;
    
    // Simple map integration
    private SimpleMapLoader simpleMapLoader;
    private LocationManager locationManager;
    private boolean mapLoaderInitialized = false;
    
    public MapNavigatorTask(CoaezUtility script) {
        this.script = script;
        this.simpleMapLoader = new SimpleMapLoader();
        this.locationManager = new LocationManager();
        this.mapLoaderInitialized = true; // Simple loader is always ready
        ScriptConsole.println("[MapNavigatorTask] Simple map loader initialized");
        
        // Initialize location manager
        locationManager.initialize().thenAccept(success -> {
            if (success) {
                ScriptConsole.println("[MapNavigatorTask] Location manager initialized with " + 
                    locationManager.getLocationCount() + " locations");
            } else {
                ScriptConsole.println("[MapNavigatorTask] Failed to initialize location manager");
            }
        });
    }
    
    /**
     * Gets the simple map loader
     */
    public SimpleMapLoader getMapLoader() {
        return simpleMapLoader;
    }
    
    /**
     * Checks if the map loader is initialized
     */
    public boolean isMapLoaderInitialized() {
        return mapLoaderInitialized;
    }
    
    /**
     * Loads the map at the current player location
     */
    public CompletableFuture<Boolean> loadMapAtCurrentLocation() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        Coordinate playerCoord = player.getCoordinate();
        return loadMapAtCoordinate(playerCoord.getX(), playerCoord.getY(), playerCoord.getZ());
    }
    
    /**
     * Loads the map at specific coordinates
     */
    public CompletableFuture<Boolean> loadMapAtCoordinate(int x, int y, int z) {
        return loadMapAtCoordinate(x, y, z, 7); // Default zoom level
    }
    
    /**
     * Loads the map at specific coordinates with zoom level
     */
    public CompletableFuture<Boolean> loadMapAtCoordinate(int x, int y, int z, int zoom) {
        if (!mapLoaderInitialized) {
            ScriptConsole.println("[MapNavigatorTask] Map loader not initialized");
            return CompletableFuture.completedFuture(false);
        }
        
        ScriptConsole.println("[MapNavigatorTask] Loading map at: " + x + ", " + y + ", " + z + " (zoom: " + zoom + ")");
        return simpleMapLoader.fetchMapData(x, y, z, zoom).thenApply(result -> {
            if (result.isSuccess()) {
                ScriptConsole.println("[MapNavigatorTask] Map data fetched successfully");
                ScriptConsole.println(result.getData());
                
                // Optionally open in browser
                boolean browserOpened = simpleMapLoader.openMapInBrowser(x, y, z, zoom);
                if (browserOpened) {
                    ScriptConsole.println("[MapNavigatorTask] Map opened in browser");
                }
                
                return true;
            } else {
                ScriptConsole.println("[MapNavigatorTask] Failed to fetch map data: " + result.getError());
                return false;
            }
        });
    }
    
    /**
     * Gets the current map center coordinates (not supported by simple loader)
     */
    public CompletableFuture<SimpleMapLoader.MapResult> getCurrentMapCenter() {
        return CompletableFuture.completedFuture(
            new SimpleMapLoader.MapResult(false, null, "Simple map loader doesn't track current center")
        );
    }
    
    /**
     * Sets the map center to specific coordinates
     */
    public CompletableFuture<SimpleMapLoader.MapResult> setMapCenter(int x, int y, int z) {
        return loadMapAtCoordinate(x, y, z).thenApply(success -> {
            if (success) {
                return new SimpleMapLoader.MapResult(true, "Map loaded at coordinates", null);
            } else {
                return new SimpleMapLoader.MapResult(false, null, "Failed to load map at coordinates");
            }
        });
    }
    
    /**
     * Executes JavaScript in the map context (not supported by simple loader)
     */
    public CompletableFuture<Object> executeMapScript(String script) {
        ScriptConsole.println("[MapNavigatorTask] JavaScript execution not supported with simple map loader");
        return CompletableFuture.completedFuture("JavaScript execution not supported with simple map loader");
    }
    
    /**
     * Opens the map in browser at specific coordinates
     */
    public boolean openMapInBrowser(int x, int y, int z, int zoom) {
        return simpleMapLoader.openMapInBrowser(x, y, z, zoom);
    }
    
    /**
     * Opens the map in browser at specific coordinates with default zoom
     */
    public boolean openMapInBrowser(int x, int y, int z) {
        return simpleMapLoader.openMapInBrowser(x, y, z);
    }
    
    /**
     * Generates a map link for specific coordinates
     */
    public String generateMapLink(int x, int y, int z, int zoom) {
        return simpleMapLoader.generateMapLink(x, y, z, zoom);
    }
    
    /**
     * Generates a map link for specific coordinates with default zoom
     */
    public String generateMapLink(int x, int y, int z) {
        return simpleMapLoader.generateMapLink(x, y, z);
    }
    

    
    /**
     * Gets the location manager
     */
    public LocationManager getLocationManager() {
        return locationManager;
    }
    
    /**
     * Navigates to a location by name
     */
    public void navigateToLocation(String locationName) {
        ScriptConsole.println("[MapNavigatorTask] navigateToLocation called with: " + locationName);
        if (locationManager.isInitialized()) {
            ScriptConsole.println("[MapNavigatorTask] LocationManager is initialized");
            LocationManager.Location location = locationManager.getLocationByName(locationName);
            if (location != null) {
                Coordinate coord = location.getCoordinate();
                ScriptConsole.println("[MapNavigatorTask] Found location coord: " + coord);
                navigateTo(coord.getX(), coord.getY(), coord.getZ());
            } else {
                ScriptConsole.println("[MapNavigatorTask] Location not found: " + locationName);
            }
        } else {
            ScriptConsole.println("[MapNavigatorTask] LocationManager not initialized");
        }
    }
    
    /**
     * Gets all available location names
     */
    public String[] getAvailableLocationNames() {
        if (!locationManager.isInitialized()) {
            return new String[0];
        }
        return locationManager.getLocationNamesArray();
    }
    
    /**
     * Searches for locations by name
     */
    public String[] searchLocationNames(String query) {
        if (!locationManager.isInitialized()) {
            return new String[0];
        }
        return locationManager.getFilteredLocationNames(query);
    }
    
    /**
     * Gets location information by name
     */
    public LocationManager.Location getLocationInfo(String locationName) {
        if (!locationManager.isInitialized()) {
            return null;
        }
        return locationManager.getLocationByName(locationName);
    }
    
    public void navigateToMarker(String markerName) {
        this.markerName = markerName;
        this.targetCoordinate = null;
        this.isNavigating = true;
        ScriptConsole.println("Set navigation target to marker: " + markerName);
    }
    
    public void navigateTo(int x, int y, int z) {
        Coordinate target = new Coordinate(x, y, z);
        ScriptConsole.println("[MapNavigatorTask] Resolving path to: " + target);
        NavPath path = NavPath.resolve(target);
        if (path != null) {
            ScriptConsole.println("[MapNavigatorTask] Path resolved, traversing");
            Movement.traverse(path);
        } else {
            ScriptConsole.println("[MapNavigatorTask] Failed to resolve path");
        }
    }
    
    public void stopNavigation() {
        this.isNavigating = false;
        this.targetCoordinate = null;
        this.markerName = null;
        ScriptConsole.println("[MapNavigatorTask] Navigation stopped");
    }
    
    public boolean isNavigating() {
        return isNavigating;
    }
    
    public Coordinate getTargetCoordinate() {
        return targetCoordinate;
    }
    
    public String getMarkerName() {
        return markerName;
    }

    @Override
    public void execute() {
        // Do nothing - navigation happens immediately in navigateTo()
    }
    
} 