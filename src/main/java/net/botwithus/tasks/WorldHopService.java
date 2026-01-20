package net.botwithus.tasks;

import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.script.Execution;

import java.util.*;

public class WorldHopService {
    
    private static final Integer[] MEMBERS_WORLDS = {
        1, 2, 4, 5, 6, 9, 10, 12, 14, 16, 21, 22, 23, 24, 25, 26, 27, 28, 31, 32, 35, 36, 37, 39, 40, 42, 44, 45,
        46, 49, 50, 51, 53, 54, 56, 58, 59, 60, 62, 63, 64, 65, 67, 68, 69, 70, 71, 72, 73, 74, 76, 77, 78, 79, 82, 83,
        85, 87, 88, 89, 91, 92, 96, 98, 99, 100, 103, 104, 105, 106, 116, 117, 119, 123, 124, 138, 139,
        140, 252, 257, 258, 259
    };
    
    private final Random random = new Random();
    private long lastHopTime = 0;
    
    private boolean worldHopEnabled = true;
    private int hopDelayMs = 120000;
    
    public int getNextAvailableWorld() {
        int currentWorld = LoginManager.getWorld();
        return getRandomWorld(currentWorld);
    }
    
    private int getRandomWorld(int excludeWorld) {
        int world;
        do {
            world = MEMBERS_WORLDS[random.nextInt(MEMBERS_WORLDS.length)];
        } while (world == excludeWorld);
        return world;
    }
    
    public boolean hopToWorld(int targetWorld) {
        long delaySmall = 600 + random.nextInt(1200);
        
        boolean success = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 7, 93782016);
        if (!success) {
            return false;
        }
        Execution.delay(delaySmall);
        
        success = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93913154);
        if (!success) {
            return false;
        }
        Execution.delay(delaySmall);
        
        int interfaceId = 1587;
        success = Execution.delayUntil(5000, () -> Interfaces.isOpen(interfaceId));
        if (!success) {
            return false;
        }

        success = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, targetWorld, 104005638);
        if (!success) {
            return false;
        }
        
        Execution.delay(10000 + random.nextInt(5000));
        lastHopTime = System.currentTimeMillis();
        return true;
    }
    
    public boolean shouldHop() {
        return worldHopEnabled && 
               System.currentTimeMillis() - lastHopTime > hopDelayMs;
    }
    
    public void setWorldHopEnabled(boolean enabled) {
        this.worldHopEnabled = enabled;
    }
    
    public void setHopDelay(int delayMs) {
        this.hopDelayMs = delayMs;
    }
    
    public boolean isWorldHopEnabled() {
        return worldHopEnabled;
    }
    
    public int getHopDelayMs() {
        return hopDelayMs;
    }
    
    public long getLastHopTime() {
        return lastHopTime;
    }
}