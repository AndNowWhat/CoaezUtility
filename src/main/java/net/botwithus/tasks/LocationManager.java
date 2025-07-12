package net.botwithus.tasks;

import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.Coordinate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Manages RuneScape locations from explv.github.io API
 */
public class LocationManager {
    private static final String LOCATIONS_URL = "https://explv.github.io/resources/locations.json";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    private List<Location> locations = new ArrayList<>();
    private List<String> locationNames = new ArrayList<>();
    private Map<String, Location> locationMap = new HashMap<>();
    private boolean initialized = false;
    private boolean loading = false;
    
    /**
     * Represents a game location
     */
    public static class Location {
        private String name;
        private Coordinate coordinate;
        private String size;
        
        public Location(String name, int x, int y, int z, String size) {
            this.name = name;
            this.coordinate = new Coordinate(x, y, z);
            this.size = size;
        }
        
        public String getName() { return name; }
        public Coordinate getCoordinate() { return coordinate; }
        public String getSize() { return size; }
        
        @Override
        public String toString() {
            return name + " (" + coordinate.getX() + ", " + coordinate.getY() + ", " + coordinate.getZ() + ")";
        }
    }
    
    /**
     * Initializes the location manager by fetching locations from the API
     */
    public CompletableFuture<Boolean> initialize() {
        if (initialized) {
            return CompletableFuture.completedFuture(true);
        }
        
        if (loading) {
            return CompletableFuture.completedFuture(false);
        }
        
        loading = true;
        ScriptConsole.println("[LocationManager] Fetching locations from API...");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonResponse = fetchLocationsJson();
                if (jsonResponse == null) {
                    ScriptConsole.println("[LocationManager] Failed to fetch locations JSON");
                    loading = false;
                    return false;
                }
                
                parseLocations(jsonResponse);
                initialized = true;
                loading = false;
                
                ScriptConsole.println("[LocationManager] Successfully loaded " + locations.size() + " locations");
                return true;
                
            } catch (Exception e) {
                ScriptConsole.println("[LocationManager] Error initializing: " + e.getMessage());
                loading = false;
                return false;
            }
        });
    }
    
    /**
     * Fetches the locations JSON from the API
     */
    private String fetchLocationsJson() {
        try {
            URL url = new URL(LOCATIONS_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                ScriptConsole.println("[LocationManager] HTTP " + responseCode + " for locations API");
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            ScriptConsole.println("[LocationManager] Error fetching locations: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses the locations JSON response
     */
    private void parseLocations(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray locationsArray = jsonObject.getAsJsonArray("locations");
            
            locations.clear();
            locationNames.clear();
            locationMap.clear();
            
            for (JsonElement element : locationsArray) {
                JsonObject locationObj = element.getAsJsonObject();
                
                String name = locationObj.get("name").getAsString();
                JsonArray coords = locationObj.getAsJsonArray("coords");
                String size = locationObj.has("size") ? locationObj.get("size").getAsString() : "default";
                
                int x = coords.get(0).getAsInt();
                int y = coords.get(1).getAsInt();
                int z = coords.get(2).getAsInt();
                
                Location location = new Location(name, x, y, z, size);
                locations.add(location);
                locationNames.add(name);
                locationMap.put(name.toLowerCase(), location);
            }
            
            // Sort locations alphabetically
            Collections.sort(locations, Comparator.comparing(Location::getName));
            Collections.sort(locationNames);
            
        } catch (Exception e) {
            ScriptConsole.println("[LocationManager] Error parsing locations JSON: " + e.getMessage());
        }
    }
    
    /**
     * Gets all locations
     */
    public List<Location> getLocations() {
        return new ArrayList<>(locations);
    }
    
    /**
     * Gets all location names
     */
    public List<String> getLocationNames() {
        return new ArrayList<>(locationNames);
    }
    
    /**
     * Gets location names as array (for ImGui combo)
     */
    public String[] getLocationNamesArray() {
        return locationNames.toArray(new String[0]);
    }
    
    /**
     * Gets a location by name (case-insensitive)
     */
    public Location getLocationByName(String name) {
        return locationMap.get(name.toLowerCase());
    }
    
    /**
     * Searches for locations by name (case-insensitive, partial match)
     */
    public List<Location> searchLocations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getLocations();
        }
        
        String lowerQuery = query.toLowerCase().trim();
        return locations.stream()
                .filter(location -> location.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets filtered location names for search
     */
    public String[] getFilteredLocationNames(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getLocationNamesArray();
        }
        
        List<Location> filtered = searchLocations(query);
        return filtered.stream()
                .map(Location::getName)
                .toArray(String[]::new);
    }
    
    /**
     * Gets a location by index from the current list
     */
    public Location getLocationByIndex(int index) {
        if (index >= 0 && index < locations.size()) {
            return locations.get(index);
        }
        return null;
    }
    
    /**
     * Gets the index of a location by name
     */
    public int getLocationIndex(String name) {
        for (int i = 0; i < locationNames.size(); i++) {
            if (locationNames.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Checks if the manager is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Checks if the manager is currently loading
     */
    public boolean isLoading() {
        return loading;
    }
    
    /**
     * Gets the number of loaded locations
     */
    public int getLocationCount() {
        return locations.size();
    }
    
    /**
     * Refreshes the locations from the API
     */
    public CompletableFuture<Boolean> refresh() {
        initialized = false;
        return initialize();
    }
} 