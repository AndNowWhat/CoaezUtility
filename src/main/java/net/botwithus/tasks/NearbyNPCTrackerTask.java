package net.botwithus.tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

/**
 * Simple task that queries nearby NPCs within a configurable range,
 * exposes name and distance to the UI, and teleports to War's Retreat when HP reaches 0.
 */
public class NearbyNPCTrackerTask implements Task {

    private static final String WAR_RETREAT_TELEPORT = "War's Retreat Teleport";
    private static final int DEFAULT_RANGE = 20;
    private static final int MAX_NPC_DISPLAY = 15;

    private int range = DEFAULT_RANGE;
    private String selectedNpcName = "";
    private final List<NpcInfo> allNearbyNpcs = new ArrayList<>();

    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[NearbyNPCTracker] Player is null, waiting...");
            Execution.delay(1000);
            return;
        }

        // Check HP - if 0 or critically low, teleport to War's Retreat
        double currentHealth = player.getCurrentHealth();
        double maximumHealth = player.getMaximumHealth();
        if (maximumHealth > 0) {
            double healthPercent = (currentHealth / maximumHealth) * 100.0;
            if (healthPercent <= 0 || currentHealth <= 0) {
                ScriptConsole.println("[NearbyNPCTracker] HP reached 0, using War's Retreat teleport");
                if (ActionBar.containsAbility(WAR_RETREAT_TELEPORT) && ActionBar.getCooldown(WAR_RETREAT_TELEPORT) <= 0) {
                    if (ActionBar.useAbility(WAR_RETREAT_TELEPORT)) {
                        ScriptConsole.println("[NearbyNPCTracker] War's Retreat teleport used successfully");
                        Execution.delay(5000);
                        return;
                    }
                } else {
                    ScriptConsole.println("[NearbyNPCTracker] War's Retreat Teleport not available or on cooldown");
                }
            }
        }

        // Update nearby NPC list
        updateNearbyNpcs(player);
        Execution.delay(500);
    }

    private void updateNearbyNpcs(LocalPlayer player) {
        allNearbyNpcs.clear();
        Coordinate playerPos = player.getCoordinate();
        if (playerPos == null) return;

        for (Npc npc : NpcQuery.newQuery().results()) {
            if (npc == null) continue;
            String name = npc.getName();
            if (name == null || name.isEmpty()) continue;

            Coordinate npcPos = npc.getCoordinate();
            if (npcPos == null) continue;

            double distance = playerPos.distanceTo(npcPos);
            if (distance <= range) {
                allNearbyNpcs.add(new NpcInfo(name, distance));
            }
        }

        // Sort by distance
        allNearbyNpcs.sort(Comparator.comparingDouble(NpcInfo::getDistance));
    }

    /**
     * Get unique NPC names in range (for selection dropdown).
     */
    public List<String> getNearbyNpcNames() {
        return allNearbyNpcs.stream()
                .map(NpcInfo::getName)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Get list of nearby NPCs (name + distance) for UI display.
     * When an NPC is selected, returns only matching NPCs; otherwise returns all (limited).
     */
    public List<NpcInfo> getNearbyNpcs() {
        var stream = (selectedNpcName == null || selectedNpcName.isEmpty())
                ? allNearbyNpcs.stream()
                : allNearbyNpcs.stream().filter(info -> selectedNpcName.equals(info.getName()));
        return stream.limit(MAX_NPC_DISPLAY).toList();
    }

    public void setSelectedNpcName(String name) {
        this.selectedNpcName = name != null ? name : "";
    }

    public String getSelectedNpcName() {
        return selectedNpcName;
    }

    public void setRange(int range) {
        this.range = Math.max(1, Math.min(50, range));
    }

    public int getRange() {
        return range;
    }

    /**
     * Immutable info for UI display.
     */
    public static class NpcInfo {
        private final String name;
        private final double distance;

        public NpcInfo(String name, double distance) {
            this.name = name;
            this.distance = distance;
        }

        public String getName() {
            return name;
        }

        public double getDistance() {
            return distance;
        }

        @Override
        public String toString() {
            return String.format("%s (%.1f)", name, distance);
        }
    }
}
