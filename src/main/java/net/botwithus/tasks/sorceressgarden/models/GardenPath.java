package net.botwithus.tasks.sorceressgarden.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.botwithus.rs3.game.Coordinate;

/**
 * Model representing a complete path through a garden
 */
public class GardenPath {
    private final List<Waypoint> waypoints;
    private final String gardenName;
    private int currentWaypointIndex;
    
    public GardenPath(String gardenName) {
        this.gardenName = gardenName;
        this.waypoints = new ArrayList<>();
        this.currentWaypointIndex = 0;
    }
    
    public GardenPath(String gardenName, List<Waypoint> waypoints) {
        this.gardenName = gardenName;
        this.waypoints = new ArrayList<>(waypoints);
        this.currentWaypointIndex = 0;
    }
    
    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
    }
    
    public void addWaypoint(Coordinate position, String description) {
        addWaypoint(new Waypoint(position, description));
    }
    
    public void addWaypoint(Coordinate position, String description, int waitTime) {
        addWaypoint(new Waypoint(position, description, waitTime));
    }
    
    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }
    
    public String getGardenName() {
        return gardenName;
    }
    
    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }
    
    public void setCurrentWaypointIndex(int index) {
        if (index >= 0 && index < waypoints.size()) {
            this.currentWaypointIndex = index;
        }
    }
    
    public Waypoint getCurrentWaypoint() {
        if (currentWaypointIndex >= 0 && currentWaypointIndex < waypoints.size()) {
            return waypoints.get(currentWaypointIndex);
        }
        return null;
    }
    
    public Waypoint getNextWaypoint() {
        if (currentWaypointIndex + 1 < waypoints.size()) {
            return waypoints.get(currentWaypointIndex + 1);
        }
        return null;
    }
    
    public boolean hasNextWaypoint() {
        return currentWaypointIndex + 1 < waypoints.size();
    }
    
    public void advanceToNextWaypoint() {
        if (hasNextWaypoint()) {
            currentWaypointIndex++;
        }
    }
    
    public void reset() {
        currentWaypointIndex = 0;
    }
    
    public boolean isComplete() {
        return currentWaypointIndex >= waypoints.size() - 1;
    }
    
    public int getTotalWaypoints() {
        return waypoints.size();
    }
    
    public int getRemainingWaypoints() {
        return Math.max(0, waypoints.size() - currentWaypointIndex - 1);
    }
    
    public double getProgressPercentage() {
        if (waypoints.isEmpty()) return 0.0;
        return (double) currentWaypointIndex / (waypoints.size() - 1) * 100.0;
    }
    
    @Override
    public String toString() {
        return String.format("GardenPath{gardenName='%s', waypoints=%d, currentIndex=%d, progress=%.1f%%}", 
                           gardenName, waypoints.size(), currentWaypointIndex, getProgressPercentage());
    }
} 