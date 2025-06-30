package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

public class DungeoneeringHoleTask implements Task {
    private final CoaezUtility script;
    private SceneObject cachedDungeoneeringHole;
    
    private static final int BEACH_TEMP_VARBIT = 28441;
    private static final int HAPPY_HOUR_VARBIT = 33485;
    private static final int SPOTLIGHT_ACTIVITY_VARBIT = 28460;
    private static final int MAX_BEACH_TEMP = 1500;
    
    public DungeoneeringHoleTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[DungeoneeringHoleTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        if (player.getAnimationId() != -1) {
            ScriptConsole.println("[DungeoneeringHoleTask] Player is animating (" + player.getAnimationId() + "), waiting...");
            return;
        }
        
        int beachTemp = VarManager.getVarbitValue(BEACH_TEMP_VARBIT);
        int happyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT);
        int spotlightActivity = VarManager.getVarbitValue(SPOTLIGHT_ACTIVITY_VARBIT);
        
        ScriptConsole.println("[DungeoneeringHoleTask] Beach temp: " + beachTemp + "/" + MAX_BEACH_TEMP + 
                            ", Happy hour: " + (happyHour == 1 ? "Yes" : "No") + 
                            ", Spotlight: " + BeachActivity.getById(spotlightActivity));
        
        if (beachTemp >= MAX_BEACH_TEMP && happyHour == 0) {
            ScriptConsole.println("[DungeoneeringHoleTask] Beach temp at max (" + beachTemp + "), need to eat ice cream!");
            if(Backpack.contains("Ice cream")) {
                if(Backpack.interact("Ice cream", "Eat")) {
                    Execution.delayUntil(5000, () -> VarManager.getVarbitValue(BEACH_TEMP_VARBIT) < MAX_BEACH_TEMP);
                }
            }
            return;
        }
        
        if (cachedDungeoneeringHole == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Dungeoneering hole")
                .option("Dungeoneer")
                .results();
                
            cachedDungeoneeringHole = results.nearest();
        }
        
        if (cachedDungeoneeringHole != null) {
            ScriptConsole.println("[DungeoneeringHoleTask] Found dungeoneering hole, interacting...");
            if (cachedDungeoneeringHole.interact("Dungeoneer")) {
                boolean animationStarted = Execution.delayUntil(5000, () -> player.getAnimationId() != -1);
                if (animationStarted) {
                    ScriptConsole.println("[DungeoneeringHoleTask] Animation started successfully");
                } else {
                    ScriptConsole.println("[DungeoneeringHoleTask] Failed to start animation");
                }
            } else {
                ScriptConsole.println("[DungeoneeringHoleTask] Failed to interact with dungeoneering hole");
            }
        } else {
            ScriptConsole.println("[DungeoneeringHoleTask] No dungeoneering hole found nearby");
        }
        
        Execution.delay(script.getRandom().nextInt(300, 600));
    }
} 