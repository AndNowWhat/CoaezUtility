package net.botwithus.tasks.clayurn;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Distance;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.Headbar;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.script.Execution;

/**
 * Helper class for interacting with game objects like clay rocks, pottery wheels, etc.
 */
public class GameObjectInteractionHelper {
    private final CoaezUtility script;
    
    public GameObjectInteractionHelper(CoaezUtility script) {
        this.script = script;
    }
    
    public boolean interactWithClayRock() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Clay rock")
                .option("Mine")
                .results();
        SceneObject rock = results.nearest();
        
        if (!isRockInRange(rock)) {
            return attemptEnterUnderground();
        }
        
        boolean isMining = Client.getLocalPlayer() != null && Client.getLocalPlayer().getAnimationId() != -1;
        if (!isMining || isAdrenalineLow()) {
            if (rock != null && rock.interact("Mine")) {
                Execution.delay(script.getRandom().nextInt(1500, 3000));
                return true;
            }
        }
        
        Execution.delay(script.getRandom().nextInt(800, 1400));
        return false;
    }
    
    public boolean interactWithCaveExit() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Cave")
                .option("Exit")
                .results();
        SceneObject caveExit = results.nearest();
        
        if (caveExit != null && caveExit.interact("Exit")) {
            Execution.delay(script.getRandom().nextInt(4000, 5000));
            return true;
        }
        
        return false;
    }
    
    public boolean interactWithSink() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Sink")
                .hidden(false)
                .option("Fill")
                .results();
        SceneObject sink = results.nearest();
        
        if (sink != null && sink.interact("Fill")) {
            return true;
        }
        
        return false;
    }
    
    public boolean interactWithPotteryWheel() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Pottery Wheel")
                .hidden(false)
                .option("Form")
                .results();
        SceneObject potteryWheel = results.nearest();
        
        if (potteryWheel != null && potteryWheel.interact("Form")) {
            Execution.delay(script.getRandom().nextInt(1500, 3000));
            return true;
        }
        
        return false;
    }
    
    public boolean interactWithPotteryOven() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Pottery oven")
                .option("Fire")
                .results();
        SceneObject oven = results.nearest();
        
        if (oven != null && oven.interact("Fire")) {
            Execution.delay(script.getRandom().nextInt(1500, 3000));
            return true;
        }
        
        return false;
    }
    
    public boolean interactWithDepositBox() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Bank deposit box")
                .hidden(false)
                .option("Deposit")
                .results();
        SceneObject depositBox = results.nearest();
        
        if (depositBox != null && depositBox.interact("Deposit")) {
            return Execution.delayUntil(10000, () -> 
                    net.botwithus.api.game.hud.inventories.DepositBox.isOpen());
        }
        
        return false;
    }
    
    private boolean isRockInRange(SceneObject rock) {
        if (rock == null || rock.getCoordinate() == null || 
            Client.getLocalPlayer() == null || Client.getLocalPlayer().getCoordinate() == null) {
            return false;
        }
        
        double distanceToRock = Distance.between(Client.getLocalPlayer().getCoordinate(), rock.getCoordinate());
        return distanceToRock <= 25.0;
    }
    
    private boolean attemptEnterUnderground() {
        SceneObject entrance = SceneObjectQuery.newQuery()
                .name("Cave entrance")
                .option("Enter")
                .hidden(false)
                .results()
                .nearest();
                
        if (entrance != null && entrance.interact("Enter")) {
            Execution.delay(script.getRandom().nextInt(1200, 2200));
            return true;
        }
        
        return false;
    }
    
    private boolean isAdrenalineLow() {
        try {
            if (Client.getLocalPlayer() == null) return true;
            for (Headbar headbar : Client.getLocalPlayer().getHeadbars()) {
                if (headbar.getId() == 5) { // adrenaline bar
                    return headbar.getWidth() <= 10; // close to bottom
                }
            }
        } catch (Exception ignored) {}
        return true;
    }
}