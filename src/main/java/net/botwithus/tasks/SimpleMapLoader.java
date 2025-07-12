package net.botwithus.tasks;

import net.botwithus.rs3.script.ScriptConsole;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple map loader that fetches map data from explv.github.io
 * This is a fallback solution while JavaFX integration is being resolved
 */
public class SimpleMapLoader {
    private static final String MAP_BASE_URL = "https://explv.github.io/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    /**
     * Result class for map operations
     */
    public static class MapResult {
        private final boolean success;
        private final String data;
        private final String error;
        
        public MapResult(boolean success, String data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }
        
        public boolean isSuccess() { return success; }
        public String getData() { return data; }
        public String getError() { return error; }
    }
    
    /**
     * Fetches the map page HTML to extract map data
     */
    public CompletableFuture<MapResult> fetchMapData(int centerX, int centerY, int centerZ, int zoom) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String mapUrl = MAP_BASE_URL + "?centreX=" + centerX + "&centreY=" + centerY + "&centreZ=" + centerZ + "&zoom=" + zoom;
                ScriptConsole.println("[SimpleMapLoader] Fetching map data from: " + mapUrl);
                
                String htmlContent = fetchHtmlContent(mapUrl);
                if (htmlContent == null) {
                    return new MapResult(false, null, "Failed to fetch map HTML");
                }
                
                // Extract useful information from the HTML
                String mapInfo = extractMapInfo(htmlContent, centerX, centerY, centerZ, zoom);
                
                return new MapResult(true, mapInfo, null);
                
            } catch (Exception e) {
                ScriptConsole.println("[SimpleMapLoader] Error fetching map data: " + e.getMessage());
                return new MapResult(false, null, e.getMessage());
            }
        });
    }
    
    /**
     * Fetches HTML content from the given URL
     */
    private String fetchHtmlContent(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                ScriptConsole.println("[SimpleMapLoader] HTTP " + responseCode + " for URL: " + urlString);
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            ScriptConsole.println("[SimpleMapLoader] Error fetching HTML: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts useful map information from the HTML content
     */
    private String extractMapInfo(String htmlContent, int centerX, int centerY, int centerZ, int zoom) {
        StringBuilder info = new StringBuilder();
        
        info.append("Map Center: ").append(centerX).append(", ").append(centerY).append(", ").append(centerZ).append("\n");
        info.append("Zoom Level: ").append(zoom).append("\n");
        info.append("Map URL: ").append(MAP_BASE_URL).append("?centreX=").append(centerX)
            .append("&centreY=").append(centerY).append("&centreZ=").append(centerZ).append("&zoom=").append(zoom).append("\n");
        
        // Try to extract any JavaScript configuration or map data
        Pattern scriptPattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
        Matcher scriptMatcher = scriptPattern.matcher(htmlContent);
        
        int scriptCount = 0;
        while (scriptMatcher.find() && scriptCount < 3) { // Limit to first 3 scripts
            String scriptContent = scriptMatcher.group(1);
            if (scriptContent.contains("map") || scriptContent.contains("coordinate") || scriptContent.contains("zoom")) {
                info.append("Script ").append(scriptCount + 1).append(" (relevant): ").append(scriptContent.substring(0, Math.min(200, scriptContent.length()))).append("...\n");
                scriptCount++;
            }
        }
        
        // Try to extract any map-related meta information
        Pattern metaPattern = Pattern.compile("<meta[^>]*name=[\"']([^\"']*)[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher metaMatcher = metaPattern.matcher(htmlContent);
        
        while (metaMatcher.find()) {
            String name = metaMatcher.group(1);
            String content = metaMatcher.group(2);
            if (name.toLowerCase().contains("map") || name.toLowerCase().contains("coordinate")) {
                info.append("Meta ").append(name).append(": ").append(content).append("\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * Generates a direct link to the map at specific coordinates
     */
    public String generateMapLink(int x, int y, int z, int zoom) {
        return MAP_BASE_URL + "?centreX=" + x + "&centreY=" + y + "&centreZ=" + z + "&zoom=" + zoom;
    }
    
    /**
     * Generates a direct link to the map at specific coordinates with default zoom
     */
    public String generateMapLink(int x, int y, int z) {
        return generateMapLink(x, y, z, 7);
    }
    
    /**
     * Opens the map in the system's default browser
     */
    public boolean openMapInBrowser(int x, int y, int z, int zoom) {
        try {
            String mapUrl = generateMapLink(x, y, z, zoom);
            ScriptConsole.println("[SimpleMapLoader] Opening map in browser: " + mapUrl);
            
            // Try to open in default browser
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(mapUrl));
                    return true;
                }
            }
            
            // Fallback: try system-specific commands
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + mapUrl);
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + mapUrl);
                return true;
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec("xdg-open " + mapUrl);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            ScriptConsole.println("[SimpleMapLoader] Error opening map in browser: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Opens the map in the system's default browser with default zoom
     */
    public boolean openMapInBrowser(int x, int y, int z) {
        return openMapInBrowser(x, y, z, 7);
    }
} 