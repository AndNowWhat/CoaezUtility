package net.botwithus.tasks;

import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

/**
 * Simple combat task that moves to target coordinates and returns to fixed coordinates
 */
public class SimpleCombatTask implements Task {
    
    private final int targetX = 3018;
    private final int targetY = 4844;
    private final int targetZ = 0;
    
    private final int returnX = 3029;
    private final int returnY = 4808;
    private final int returnZ = 0;
    
    // Varp monitoring variables
    private int lastVarpValue = -1;
    private long varpCheckStartTime = 0;
    private boolean monitoringVarp = false;
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[SimpleCombatTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        Coordinate currentPos = player.getCoordinate();
        
        // Check if we're at return coordinates (3030, 4807)
        if (isAtPosition(currentPos, returnX, returnY, returnZ)) {
            // We're at return coordinates, check varp value and handle accordingly
            int currentVarpValue = VarManager.getVarpValue(4501);
            
            // Start monitoring if we haven't started yet
            if (!monitoringVarp) {
                lastVarpValue = currentVarpValue;
                varpCheckStartTime = System.currentTimeMillis();
                monitoringVarp = true;
                ScriptConsole.println("[SimpleCombatTask] Started monitoring varp 4501, initial value: " + currentVarpValue);
            }
            
            // Check if varp value has changed
            if (currentVarpValue != lastVarpValue) {
                ScriptConsole.println("[SimpleCombatTask] Varp 4501 changed from " + lastVarpValue + " to " + currentVarpValue);
                lastVarpValue = currentVarpValue;
                varpCheckStartTime = System.currentTimeMillis(); // Reset timer
            }
            
            // Check if 5 seconds have passed without varp change
            long timeSinceLastChange = System.currentTimeMillis() - varpCheckStartTime;
            if (timeSinceLastChange >= 20000) {
                ScriptConsole.println("[SimpleCombatTask] Varp 4501 hasn't changed for 20+ seconds, looking for NPC to attack");
                if (findAndAttackNearestNPC()) {
                    // Reset monitoring after successful attack
                    monitoringVarp = false;
                    lastVarpValue = -1;
                    varpCheckStartTime = 0;
                } else {
                    ScriptConsole.println("[SimpleCombatTask] No attackable NPC found, continuing to wait");
                    Execution.delay(2000);
                }
                return;
            }
            
            // We're at return coordinates, check if not in combat
            if (!player.inCombat()) {
                // Not in combat, move to target
                ScriptConsole.println("[SimpleCombatTask] At return location, not in combat, moving to target: " + targetX + ", " + targetY + ", " + targetZ);
                Movement.walkTo(targetX, targetY, true);
                
                // Reset monitoring when moving away
                monitoringVarp = false;
                lastVarpValue = -1;
                varpCheckStartTime = 0;
                
                // Wait until we reach the target
                Execution.delayUntil(15000,
                    () -> {
                        LocalPlayer currentPlayer = Client.getLocalPlayer();
                        return currentPlayer != null && isAtPosition(currentPlayer.getCoordinate(), targetX, targetY, targetZ);
                    }
                );
                
                ScriptConsole.println("[SimpleCombatTask] Reached target location");
            } else {
                ScriptConsole.println("[SimpleCombatTask] At return location but in combat, waiting...");
                Execution.delay(2000);
            }
        } else {
            // We're not at return coordinates, move back to return coordinates
            ScriptConsole.println("[SimpleCombatTask] Not at return location, moving back to: " + returnX + ", " + returnY + ", " + returnZ);
            Movement.walkTo(returnX, returnY, true);
            
            // Wait until we reach the return location
            Execution.delayUntil(15000,
                () -> {
                    LocalPlayer currentPlayer = Client.getLocalPlayer();
                    return currentPlayer != null && isAtPosition(currentPlayer.getCoordinate(), returnX, returnY, returnZ);
                }
            );
            
            ScriptConsole.println("[SimpleCombatTask] Returned to return location");
        }
    }
    
    /**
     * Check if player is at a specific position
     */
    private boolean isAtPosition(Coordinate playerPosition, int targetX, int targetY, int targetZ) {
        if (playerPosition == null) return false;
        return Math.abs(playerPosition.getX() - targetX) <= 2 && 
               Math.abs(playerPosition.getY() - targetY) <= 2 && 
               playerPosition.getZ() == targetZ;
    }
    
    /**
     * Find and attack the nearest NPC with attack option
     * @return true if an NPC was found and attacked, false otherwise
     */
    private boolean findAndAttackNearestNPC() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        // Find all NPCs with attack option
        var npcResults = NpcQuery.newQuery()
                .option("Attack")
                .results();
        
        if (npcResults.isEmpty()) {
            ScriptConsole.println("[SimpleCombatTask] No NPCs with attack option found");
            return false;
        }
        
        // Find the nearest NPC
        Npc nearestNPC = npcResults.nearestTo(player);
        if (nearestNPC == null) {
            ScriptConsole.println("[SimpleCombatTask] No valid nearest NPC found");
            return false;
        }
        
        ScriptConsole.println("[SimpleCombatTask] Found nearest attackable NPC: " + nearestNPC.getName() + 
                            " at distance: " + player.getCoordinate().distanceTo(nearestNPC.getCoordinate()));
        
        // Attempt to attack the NPC
        if (nearestNPC.interact("Attack")) {
            ScriptConsole.println("[SimpleCombatTask] Successfully attacked NPC: " + nearestNPC.getName());
            Execution.delay(1000); // Small delay after attack
            return true;
        } else {
            ScriptConsole.println("[SimpleCombatTask] Failed to attack NPC: " + nearestNPC.getName());
            return false;
        }
    }
    
    /**
     * Reset the task state
     */
    public void reset() {
        ScriptConsole.println("[SimpleCombatTask] Task state reset");
        // Reset varp monitoring state
        monitoringVarp = false;
        lastVarpValue = -1;
        varpCheckStartTime = 0;
    }
}