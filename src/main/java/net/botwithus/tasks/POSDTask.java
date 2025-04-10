package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.util.RandomGenerator;

public class POSDTask implements Task {
    private final CoaezUtility script;
    
    public POSDTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        ScriptConsole.println("[POSDTask] Executing POSD task");
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[POSDTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        // Check for auto retaliate
        if (VarManager.getVarValue(VarDomainType.PLAYER, 462) == 1) {
            ScriptConsole.println("[POSDTask] Auto Retaliate is off, turning it on");
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93716537);
            Execution.delay(RandomGenerator.nextInt(1000, 3000));
            ScriptConsole.println("[POSDTask] Auto Retaliate enabled");
            return;
        }
        
        // Check for death
        Npc death = NpcQuery.newQuery().name("Death").results().nearest();
        if (death != null) {
            ScriptConsole.println("[POSDTask] Death detected, stopping script");
            Execution.delay(5000L);
            LoginManager.setAutoLogin(false);
            MiniMenu.interact(14, 1, -1, 93913156);
            Execution.delay(5000L);
            return;
        }
        
        // Handle POD steps
        handlePODSteps();
        
        // Handle looting if enabled
        if (script.getPOSD().isUseLoot()) {
            script.getPOSD().processLooting();
        }
        
        if (script.getPOSD().isInteractWithLootAll()) {
            script.getPOSD().lootEverything();
        }
    }
    
    private void handlePODSteps() {
        int currentStep = script.getPOSD().getCurrentStep();
        LocalPlayer player = Client.getLocalPlayer();
        
        switch (currentStep) {
            case 1:
                ScriptConsole.println("[POSDTask] Step 1: Traveling to POD");
                if (travelToPOD()) {
                    ScriptConsole.println("[POSDTask] Arrived at POD. Proceeding to interaction.");
                    script.getPOSD().setCurrentStep(2);
                }
                break;
                
            case 2:
                ScriptConsole.println("[POSDTask] Step 2: Interacting with Kags");
                if (interactWithKags()) {
                    ScriptConsole.println("[POSDTask] Interacted with Kags. Proceeding to the next step.");
                    script.getPOSD().setCurrentStep(3);
                }
                break;
                
            case 3:
                ScriptConsole.println("[POSDTask] Step 3: Interacting with first door");
                if (script.getPOSD().interactWithFirstDoor()) {
                    ScriptConsole.println("[POSDTask] Interacted with the first door. Proceeding to the next step.");
                    script.getPOSD().setCurrentStep(4);
                }
                break;
                
            case 4:
                ScriptConsole.println("[POSDTask] Step 4: Interacting with other door");
                if (script.getPOSD().interactWithOtherDoor()) {
                    ScriptConsole.println("[POSDTask] Interacted with the other door. Proceeding to the next step.");
                    script.getPOSD().setCurrentStep(5);
                }
                break;
                
            case 5:
                ScriptConsole.println("[POSDTask] Step 5: Moving player east");
                if (script.getPOSD().movePlayerEast()) {
                    ScriptConsole.println("[POSDTask] Moved player east. Proceeding to the next step.");
                    script.getPOSD().setCurrentStep(6);
                }
                break;
                
            case 6:
                ScriptConsole.println("[POSDTask] Step 6: Attacking targets");
                script.getPOSD().attackTarget(player);
                if (shouldBank(player)) {
                    script.getPOSD().setCurrentStep(7);
                }
                break;
                
            case 7:
                ScriptConsole.println("[POSDTask] Step 7: Banking");
                if (script.getPOSD().bankingForPOD(player)) {
                    script.getPOSD().setCurrentStep(1);
                }
                break;
                
            default:
                ScriptConsole.println("[POSDTask] Invalid step. Resetting to step 1.");
                script.getPOSD().setCurrentStep(1);
        }
    }

    
    
    private boolean travelToPOD() {
        return script.getPOSD().travelToPOD();
    }
    
    private boolean interactWithKags() {
        return script.getPOSD().interactWithKags();
    }
    
    private boolean shouldBank(LocalPlayer player) {
        boolean needsOverloads = script.getPOSD().isUseOverloads() && script.getPOSD().drinkOverloads(player) == 1L;
        boolean needsPrayerPots = script.getPOSD().isUsePrayerPots() && script.getPOSD().usePrayerOrRestorePots(player) == 1L;
        boolean needsAggroPots = script.getPOSD().isUseAggroPots() && script.getPOSD().useAggression(player) == 1L;
        boolean needsWeaponPoison = script.getPOSD().isUseWeaponPoison() && script.getPOSD().useWeaponPoison(player) == 1L;
        
        return needsOverloads || needsPrayerPots || needsAggroPots || needsWeaponPoison;
    }
} 