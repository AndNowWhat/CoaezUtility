package net.botwithus.tasks;

import java.util.ArrayList;
import java.util.List;

import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

/**
 * Combat task that shows nearby monsters on GUI and handles target switching
 * when current target's HP drops below 0
 */
public class MonsterCombatTask implements Task {
    
    // Selected monster from GUI
    private Npc selectedMonster = null;
    private String selectedMonsterName = "";
    
    // Monster detection settings
    private final int maxDetectionDistance = 20; // Maximum distance to detect monsters
    private final int maxMonsterCount = 10; // Maximum number of monsters to display
    
    // Cached monster names for GUI
    private List<String> cachedMonsterNames = new ArrayList<>();
    
    // Combat state - simplified
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[MonsterCombatTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        // Always refresh monster list
        ScriptConsole.println("[MonsterCombatTask] Refreshing monster list...");
        updateMonsterList();
        
        // Check if we have a selected monster
        if (selectedMonsterName.isEmpty()) {
            ScriptConsole.println("[MonsterCombatTask] No monster selected, waiting for selection...");
            Execution.delay(2000);
            return;
        }
        
        // Attack nearest monster
        attackSelectedMonster();
    }
    
    /**
     * Set the selected monster name from GUI
     */
    public void setSelectedMonster(Npc monster, String monsterName) {
        this.selectedMonsterName = monsterName;
        ScriptConsole.println("[MonsterCombatTask] Selected monster type: " + monsterName);
    }
    
    /**
     * Get list of nearby monster names for GUI display
     */
    public List<String> getNearbyMonsters() {
        return cachedMonsterNames;
    }
    
    /**
     * Update the cached monster list with unique names only
     */
    private void updateMonsterList() {
        ScriptConsole.println("[MonsterCombatTask] Getting nearby monsters...");
        cachedMonsterNames.clear();
        
        // Find all NPCs
        var npcResults = NpcQuery.newQuery().results();
        ScriptConsole.println("[MonsterCombatTask] Found " + npcResults.size() + " NPCs");
        
        // Use Set to track unique names
        java.util.Set<String> uniqueNames = new java.util.HashSet<>();
        
        for (Npc npc : npcResults) {
            if (npc != null && npc.getName() != null && !npc.getName().isEmpty()) {
                String name = npc.getName();
                if (!uniqueNames.contains(name)) {
                    ScriptConsole.println("[MonsterCombatTask] Adding unique NPC: " + name);
                    cachedMonsterNames.add(name);
                    uniqueNames.add(name);
                }
            }
        }
        
        ScriptConsole.println("[MonsterCombatTask] Cached " + cachedMonsterNames.size() + " unique monsters");
    }
    
    /**
     * Attack the nearest monster with the selected name
     */
    private void attackSelectedMonster() {
        if (selectedMonsterName.isEmpty()) {
            return;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return;
        }
        
        // Check if player already has a live target
        if (player.getTarget() != null && player.getTarget().getCurrentHealth() > 0) {
            ScriptConsole.println("[MonsterCombatTask] Player already has live target, not attacking");
            return;
        }
        
        // Find nearest monster with the selected name
        var npcResults = NpcQuery.newQuery().results();
        Npc nearestNpc = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Npc npc : npcResults) {
            if (npc != null && npc.getName().equals(selectedMonsterName)) {
                double distance = player.getCoordinate().distanceTo(npc.getCoordinate());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestNpc = npc;
                }
            }
        }
        
        if (nearestNpc != null) {
            if (nearestNpc.interact("Attack")) {
                ScriptConsole.println("[MonsterCombatTask] Attacking nearest " + selectedMonsterName + " at distance " + nearestDistance);
                Execution.delay(1000);
            }
        } else {
            ScriptConsole.println("[MonsterCombatTask] No " + selectedMonsterName + " found to attack");
        }
    }
    
    /**
     * Find the next nearest monster of the same type
     */
    private void findNextNearestMonster() {
        if (selectedMonsterName.isEmpty()) {
            ScriptConsole.println("[MonsterCombatTask] No monster name selected");
            return;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[MonsterCombatTask] Player is null");
            return;
        }
        
        ScriptConsole.println("[MonsterCombatTask] Looking for next " + selectedMonsterName);
        
        // Find nearest NPC with the selected name
        var npcResults = NpcQuery.newQuery().results();
        Npc nearestNpc = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Npc npc : npcResults) {
            if (npc != null && npc.getName().equals(selectedMonsterName)) {
                double distance = player.getCoordinate().distanceTo(npc.getCoordinate());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestNpc = npc;
                }
            }
        }
        
        if (nearestNpc != null) {
            ScriptConsole.println("[MonsterCombatTask] Found next " + selectedMonsterName + " at distance " + nearestDistance);
        } else {
            ScriptConsole.println("[MonsterCombatTask] No " + selectedMonsterName + " found");
        }
    }
    
    /**
     * Check if a monster is still valid (exists and is attackable)
     */
    private boolean isMonsterValid(Npc monster) {
        if (monster == null) {
            return false;
        }
        
        // Check if monster still has attack option
        return monster.getOptions().contains("Attack");
    }
    
    /**
     * Check if any monster with the selected name is dead
     */
    private boolean isTargetDead(Npc monster) {
        if (selectedMonsterName.isEmpty()) {
            return true;
        }
        
        // Check if any monster with the selected name still exists and is attackable
        var npcResults = NpcQuery.newQuery().results();
        for (Npc npc : npcResults) {
            if (npc != null && npc.getName().equals(selectedMonsterName) && npc.getOptions().contains("Attack")) {
                return false; // Found at least one alive monster with the selected name
            }
        }
        
        return true; // No alive monsters with the selected name found
    }
    
    /**
     * Reset the task state
     */
    public void reset() {
        ScriptConsole.println("[MonsterCombatTask] Task state reset");
        selectedMonsterName = "";
        cachedMonsterNames.clear();
    }
    
    /**
     * Get the currently selected monster name
     */
    public String getSelectedMonsterName() {
        return selectedMonsterName;
    }
    
    
    /**
     * Check if a monster type is currently selected
     */
    public boolean hasSelectedMonster() {
        return !selectedMonsterName.isEmpty();
    }
    
    /**
     * Inner class to hold monster information for GUI display
     */
    public static class MonsterInfo {
        private final String name;
        private final int id;
        private final double distance;
        private final Coordinate coordinate;
        private final Npc npc;
        
        public MonsterInfo(String name, int id, double distance, Coordinate coordinate, Npc npc) {
            this.name = name;
            this.id = id;
            this.distance = distance;
            this.coordinate = coordinate;
            this.npc = npc;
        }
        
        public String getName() { return name; }
        public int getId() { return id; }
        public double getDistance() { return distance; }
        public Coordinate getCoordinate() { return coordinate; }
        public Npc getNpc() { return npc; }
        
        @Override
        public String toString() {
            return String.format("%s (ID: %d, Distance: %.1f)", name, id, distance);
        }
    }
}
