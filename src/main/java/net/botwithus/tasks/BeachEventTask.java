package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.PathingEntity;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;

public class BeachEventTask implements Task {
    private final CoaezUtility script;
    
    // Cached objects
    private SceneObject cachedDungeoneeringHole;
    private SceneObject cachedCoconutShy;
    private SceneObject cachedBarbeque;
    private SceneObject cachedHookADuck;
    private SceneObject cachedBodybuilding;
    
    // Beach event constants
    private static final int BEACH_TEMP_VARBIT = 28441;
    private static final int HAPPY_HOUR_VARBIT = 33485;
    private static final int SPOTLIGHT_ACTIVITY_VARBIT = 28460;
    private static final int MAX_BEACH_TEMP = 1500;
    
    // Configuration
    private BeachActivity selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
    private boolean useCocktails = false;
    private boolean useBattleship = false;
    private boolean fightClawdia = true;
    private boolean useSpotlight = false;
    private String spotlightHappyHour = "Hunter";
    
    // State
    private boolean canDeployShip = false;
    private int failCount = 0;
    private String lastBattleshipMessage = "";
    
    public BeachEventTask(CoaezUtility script) {
        this.script = script;
    }
    
    public void setSelectedActivity(BeachActivity activity) {
        this.selectedActivity = activity;
    }
    
    public void setUseCocktails(boolean useCocktails) {
        this.useCocktails = useCocktails;
    }
    
    public void setUseBattleship(boolean useBattleship) {
        this.useBattleship = useBattleship;
    }
    
    public void setFightClawdia(boolean fightClawdia) {
        this.fightClawdia = fightClawdia;
    }
    
    public void setUseSpotlight(boolean useSpotlight) {
        this.useSpotlight = useSpotlight;
    }
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[BeachEventTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        if (player.getAnimationId() != -1) {
            ScriptConsole.println("[BeachEventTask] Player is animating (" + player.getAnimationId() + "), waiting...");
            return;
        }
        
        if (failCount > 4) {
            ScriptConsole.println("[BeachEventTask] Too many failures, stopping...");
            return;
        }
        
        int beachTemp = VarManager.getVarbitValue(BEACH_TEMP_VARBIT);
        int happyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT);
        int spotlightActivity = VarManager.getVarbitValue(SPOTLIGHT_ACTIVITY_VARBIT);
        
        ScriptConsole.println("[BeachEventTask] Beach temp: " + beachTemp + "/" + MAX_BEACH_TEMP + 
                            ", Happy hour: " + (happyHour == 1 ? "Yes" : "No") + 
                            ", Spotlight: " + BeachActivity.getById(spotlightActivity));
        
        if (useSpotlight) {
            updateActivityFromSpotlight(spotlightActivity, happyHour == 1);
        }
        
        if (!eatIceCream(beachTemp, happyHour == 1)) {
            return;
        }
        
        if (fightClawdia && handleClawdia()) {
            return;
        }
        
        if (useBattleship) {
            handleBattleship();
        }
        
        executeActivity();
        
        Execution.delay(script.getRandom().nextInt(300, 600));
    }
    
    private void updateActivityFromSpotlight(int spotlightActivity, boolean isHappyHour) {
        if (isHappyHour) {
            switch (spotlightHappyHour) {
                case "Dung":
                    selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
                    break;
                case "Strength":
                    selectedActivity = BeachActivity.BODY_BUILDING;
                    break;
                case "Construction":
                    selectedActivity = BeachActivity.SANDCASTLE_BUILDING;
                    break;
                case "Hunter":
                    selectedActivity = BeachActivity.HOOK_A_DUCK;
                    break;
                case "Ranged":
                    selectedActivity = BeachActivity.COCONUT_SHY;
                    break;
                case "Cooking":
                    selectedActivity = BeachActivity.BARBEQUES;
                    break;
                case "Farming":
                    selectedActivity = BeachActivity.PALM_TREE_FARMING;
                    break;
            }
        } else {
            BeachActivity spotlightBeachActivity = BeachActivity.getById(spotlightActivity);
            if (spotlightBeachActivity != null) {
                selectedActivity = spotlightBeachActivity;
            }
        }
    }
    
    private boolean eatIceCream(int beachTemp, boolean isHappyHour) {
        if (beachTemp >= MAX_BEACH_TEMP && !isHappyHour) {
            ScriptConsole.println("[BeachEventTask] Beach temp at max (" + beachTemp + "), need to eat ice cream!");
            if (Backpack.contains("Ice cream")) {
                if (Backpack.interact("Ice cream", "Eat")) {
                    Execution.delayUntil(5000, () -> VarManager.getVarbitValue(BEACH_TEMP_VARBIT) < MAX_BEACH_TEMP);
                    return true;
                }
            }
            failCount++;
            ScriptConsole.println("[BeachEventTask] It's too hot to work, time for an ice cream.");
            return false;
        }
        return true;
    }
    
    private boolean handleClawdia() {
        EntityResultSet<Npc> clawdiaResults = NpcQuery.newQuery()
            .name("Clawdia")
            .results();
        
        Npc clawdia = clawdiaResults.nearest();
        PathingEntity<?> currentTarget = Client.getLocalPlayer().getTarget();
        
        if (clawdia != null && currentTarget == null) {
            LocalPlayer player = Client.getLocalPlayer();
            if (player != null && player.getTarget() == null) {
                ScriptConsole.println("[BeachEventTask] Found Clawdia, attacking...");
                if (clawdia.interact("Attack")) {
                    Execution.delay(600);
                    return true;
                }
            }
            return true;
        }
        return false;
    }
    
    private void handleBattleship() {
        if (!Backpack.contains("Toy royal battleship")) {
            ScriptConsole.println("[BeachEventTask] No battleship in inventory");
            return;
        }
        
        if (lastBattleshipMessage.isEmpty()) {
            ScriptConsole.println("[BeachEventTask] Waiting for battleship result message...");
            return;
        }
        
        ScriptConsole.println("[BeachEventTask] Deploying battleship based on message: " + lastBattleshipMessage);
        
        if (Backpack.interact("Toy royal battleship", "Deploy")) {
            boolean interfaceOpened = Execution.delayUntil(3000, () -> 
                Interfaces.isOpen(751));
            
            if (interfaceOpened) {
                if (lastBattleshipMessage.contains("Our accuracy penetrated their defences!")) {
                    // Deploy aggressive ship
                    ScriptConsole.println("[BeachEventTask] Deploying aggressive ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217602);
                } else if (lastBattleshipMessage.contains("Our defences withstood their aggression!")) {
                    // Deploy accurate ship
                    ScriptConsole.println("[BeachEventTask] Deploying accurate ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217586);
                } else if (lastBattleshipMessage.contains("Our aggression overcame their accuracy!")) {
                    // Deploy defensive ship
                    ScriptConsole.println("[BeachEventTask] Deploying defensive ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217594);
                }
                
                lastBattleshipMessage = "";
                Execution.delay(1200);
            } else {
                ScriptConsole.println("[BeachEventTask] Failed to open battleship interface");
            }
        }
    }
    
    private void executeActivity() {
        switch (selectedActivity) {
            case DUNGEONEERING_HOLE:
                executeDungeoneering();
                break;
            case BODY_BUILDING:
                executeBodybuilding();
                break;
            case SANDCASTLE_BUILDING:
                executeSandcastleBuilding();
                break;
            case HOOK_A_DUCK:
                executeHookADuck();
                break;
            case COCONUT_SHY:
                executeCoconutShy();
                break;
            case BARBEQUES:
                executeBarbeques();
                break;
            case PALM_TREE_FARMING:
                executePalmTreeFarming();
                break;
            case ROCK_POOLS:
                executeRockPools();
                break;
            default:
                ScriptConsole.println("[BeachEventTask] Unknown activity: " + selectedActivity);
        }
    }
    
    private void executeDungeoneering() {
        if (useCocktails) {
            useDungeoneeringCocktail();
        }
        
        if (cachedDungeoneeringHole == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Dungeoneering hole")
                .option("Dungeoneer")
                .results();
            cachedDungeoneeringHole = results.nearest();
        }
        
        if (cachedDungeoneeringHole != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Interacting with dungeoneering hole...");
            if (cachedDungeoneeringHole.interact("Dungeoneer")) {
                ScriptConsole.println("[BeachEventTask] Get Back in that Hole!");
            }
        }
    }
    
    private void executeBodybuilding() {
        if (useCocktails) {
            useBodybuildingCocktail();
        }
        
        if (cachedBodybuilding == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.BODYBUILDING.getId())
                .results();
            cachedBodybuilding = results.nearest();
        }
        
        if (cachedBodybuilding != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Interacting with bodybuilding platform...");
            if (cachedBodybuilding.interact("Use")) {
                Execution.delay(1500);
            }
        }
    }
    
    private void executeSandcastleBuilding() {
        if (useCocktails) {
            useSandcastleCocktail();
        }
        
        EntityResultSet<Npc> wizardsNpc = NpcQuery.newQuery().id(BeachEventNPCs.WIZARDS_NPC.getId()).results();
        EntityResultSet<Npc> lumbridgeNpc = NpcQuery.newQuery().id(BeachEventNPCs.LUMBRIDGE_NPC.getId()).results();
        EntityResultSet<Npc> pyramidNpc = NpcQuery.newQuery().id(BeachEventNPCs.PYRAMID_NPC.getId()).results();
        EntityResultSet<Npc> exchangeNpc = NpcQuery.newQuery().id(BeachEventNPCs.EXCHANGE_NPC.getId()).results();
        
        if (!wizardsNpc.isEmpty()) {
            interactWithSandcastles(BeachEventObjects.getWizardsSandcastles());
        } else if (!lumbridgeNpc.isEmpty()) {
            interactWithSandcastles(BeachEventObjects.getLumbridgeSandcastles());
        } else if (!pyramidNpc.isEmpty()) {
            interactWithSandcastles(BeachEventObjects.getPyramidSandcastles());
        } else if (!exchangeNpc.isEmpty()) {
            interactWithSandcastles(BeachEventObjects.getExchangeSandcastles());
        }
    }
    
    private void interactWithSandcastles(int[] sandcastleIds) {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
            .ids(sandcastleIds)
            .results();
        
        SceneObject sandcastle = results.nearest();
        if (sandcastle != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Building sandcastle...");
            sandcastle.interact("Build");
        }
    }
    
    private void executeHookADuck() {
        if (useCocktails) {
            useHookADuckCocktail();
        }
        
        if (cachedHookADuck == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.HOOK_A_DUCK.getId())
                .results();
            cachedHookADuck = results.nearest();
        }
        
        if (cachedHookADuck != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Playing hook-a-duck...");
            if (cachedHookADuck.interact("Play")) {
                ScriptConsole.println("[BeachEventTask] Go catch dat ducky!");
            }
        }
    }
    
    private void executeCoconutShy() {
        if (useCocktails) {
            useCoconutShyCocktail();
        }
        
        if (cachedCoconutShy == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.COCONUT_SKY.getId())
                .results();
            cachedCoconutShy = results.nearest();
        }
        
        if (cachedCoconutShy != null) {
            ScriptConsole.println("[BeachEventTask] Playing coconut shy...");
            if (cachedCoconutShy.interact("Play")) {
                ScriptConsole.println("[BeachEventTask] Throw that coconut!");
            }
        }
    }
    
    private void executeBarbeques() {
        if (useCocktails) {
            useBarbequeCocktail();
        }
        
        if (cachedBarbeque == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.BARBEQUE_GRILL.getId())
                .results();
            cachedBarbeque = results.nearest();
        }
        
        if (cachedBarbeque != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Using barbeque...");
            if (cachedBarbeque.interact("Cook")) {
                ScriptConsole.println("[BeachEventTask] Get that fish cooked!");
            }
        }
    }
    
    private void executePalmTreeFarming() {
        if (useCocktails) {
            usePalmTreeCocktail();
        }
        
        if (Backpack.isFull() && Backpack.contains("Coconut")) {
            // Deposit coconuts
            EntityResultSet<SceneObject> pileResults = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.PILEOFCOCONUTS.getId())
                .results();
            
            SceneObject pile = pileResults.nearest();
            if (pile != null) {
                ScriptConsole.println("[BeachEventTask] Inventory full, depositing coconuts...");
                pile.interact("Deposit");
                return;
            }
        }
        
        EntityResultSet<SceneObject> treeResults = SceneObjectQuery.newQuery()
            .ids(BeachEventObjects.getPalmTrees())
            .results();
        
        SceneObject tree = treeResults.nearest();
        if (tree != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Chopping palm tree...");
            if (tree.interact("Chop")) {
                ScriptConsole.println("[BeachEventTask] Back to chopping trees.");
            }
        }
    }
    
    private void executeRockPools() {
        if (useCocktails) {
            useRockPoolCocktail();
        }
        
        if (Backpack.isFull() && Backpack.contains("Tropical trout")) {
            EntityResultSet<Npc> wellingtonResults = NpcQuery.newQuery()
                .id(BeachEventNPCs.WELLINGTON.getId())
                .results();
            
            Npc wellington = wellingtonResults.nearest();
            if (wellington != null) {
                ScriptConsole.println("[BeachEventTask] Inventory full, depositing fish...");
                wellington.interact("Trade");
                return;
            }
        }
        
        EntityResultSet<Npc> fishingSpotResults = NpcQuery.newQuery()
            .id(BeachEventNPCs.FISHING_SPOT.getId())
            .results();
        
        Npc fishingSpot = fishingSpotResults.nearest();
        if (fishingSpot != null && !canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Fishing at rock pools...");
            if (fishingSpot.interact("Fish")) {
                ScriptConsole.println("[BeachEventTask] Back to fishing.");
            }
        }
    }
    
    private void useDungeoneeringCocktail() {
        boolean isHappyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT) == 1;
        
        if (!isHappyHour) {
            if (Backpack.contains(BeachEventItems.A_HOLE_IN_ONE.getId()) && !hasBuffActive(BeachEventItems.A_HOLE_IN_ONE.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking A Hole in One cocktail...");
                Backpack.interact(BeachEventItems.A_HOLE_IN_ONE.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.LEMON_SOUR.getId()) && !hasBuffActive(BeachEventItems.LEMON_SOUR.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Lemon Sour cocktail...");
                Backpack.interact(BeachEventItems.LEMON_SOUR.getId(), "Drink");
                Execution.delay(600);
            }
        } else {
            if (Backpack.contains(BeachEventItems.LEMON_SOUR.getId()) && !hasBuffActive(BeachEventItems.LEMON_SOUR.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Lemon Sour cocktail...");
                Backpack.interact(BeachEventItems.LEMON_SOUR.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.A_HOLE_IN_ONE.getId()) && !hasBuffActive(BeachEventItems.A_HOLE_IN_ONE.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking A Hole in One cocktail...");
                Backpack.interact(BeachEventItems.A_HOLE_IN_ONE.getId(), "Drink");
                Execution.delay(600);
            }
        }
    }
    
    private void useBodybuildingCocktail() {
        if (Backpack.contains(BeachEventItems.PINK_FIZZ.getId()) && !hasBuffActive(BeachEventItems.PINK_FIZZ.getId())) {
            ScriptConsole.println("[BeachEventTask] Drinking Pink Fizz cocktail...");
            Backpack.interact(BeachEventItems.PINK_FIZZ.getId(), "Drink");
            Execution.delay(600);
        }
    }
    
    private void useSandcastleCocktail() {
        boolean isHappyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT) == 1;
        
        if (!isHappyHour) {
            if (Backpack.contains(BeachEventItems.GEORGES_PEACH_DELIGHT.getId()) && !hasBuffActive(BeachEventItems.GEORGES_PEACH_DELIGHT.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking George's Peach Delight cocktail...");
                Backpack.interact(BeachEventItems.GEORGES_PEACH_DELIGHT.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.PURPLE_LUMBRIDGE.getId()) && !hasBuffActive(BeachEventItems.PURPLE_LUMBRIDGE.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Purple Lumbridge cocktail...");
                Backpack.interact(BeachEventItems.PURPLE_LUMBRIDGE.getId(), "Drink");
                Execution.delay(600);
            }
        } else {
            if (Backpack.contains(BeachEventItems.PURPLE_LUMBRIDGE.getId()) && !hasBuffActive(BeachEventItems.PURPLE_LUMBRIDGE.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Purple Lumbridge cocktail...");
                Backpack.interact(BeachEventItems.PURPLE_LUMBRIDGE.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.GEORGES_PEACH_DELIGHT.getId()) && !hasBuffActive(BeachEventItems.GEORGES_PEACH_DELIGHT.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking George's Peach Delight cocktail...");
                Backpack.interact(BeachEventItems.GEORGES_PEACH_DELIGHT.getId(), "Drink");
                Execution.delay(600);
            }
        }
    }
    
    private void useHookADuckCocktail() {
        boolean isHappyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT) == 1;
        
        if (!isHappyHour) {
            if (Backpack.contains(BeachEventItems.UGLY_DUCKLING.getId()) && !hasBuffActive(BeachEventItems.UGLY_DUCKLING.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Ugly Duckling cocktail...");
                Backpack.interact(BeachEventItems.UGLY_DUCKLING.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.PINEAPPLETINI.getId()) && !hasBuffActive(BeachEventItems.PINEAPPLETINI.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Pineappletini cocktail...");
                Backpack.interact(BeachEventItems.PINEAPPLETINI.getId(), "Drink");
                Execution.delay(600);
            }
        } else {
            if (Backpack.contains(BeachEventItems.PINEAPPLETINI.getId()) && !hasBuffActive(BeachEventItems.PINEAPPLETINI.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Pineappletini cocktail...");
                Backpack.interact(BeachEventItems.PINEAPPLETINI.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.UGLY_DUCKLING.getId()) && !hasBuffActive(BeachEventItems.UGLY_DUCKLING.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Ugly Duckling cocktail...");
                Backpack.interact(BeachEventItems.UGLY_DUCKLING.getId(), "Drink");
                Execution.delay(600);
            }
        }
    }
    
    private void useCoconutShyCocktail() {
        if (Backpack.contains(BeachEventItems.PINK_FIZZ.getId()) && !hasBuffActive(BeachEventItems.PINK_FIZZ.getId())) {
            ScriptConsole.println("[BeachEventTask] Drinking Pink Fizz cocktail...");
            Backpack.interact(BeachEventItems.PINK_FIZZ.getId(), "Drink");
            Execution.delay(600);
        }
    }
    
    private void useBarbequeCocktail() {
        if (Backpack.contains(BeachEventItems.PURPLE_LUMBRIDGE.getId()) && !hasBuffActive(BeachEventItems.PURPLE_LUMBRIDGE.getId())) {
            ScriptConsole.println("[BeachEventTask] Drinking Purple Lumbridge cocktail...");
            Backpack.interact(BeachEventItems.PURPLE_LUMBRIDGE.getId(), "Drink");
            Execution.delay(600);
        }
    }
    
    private void usePalmTreeCocktail() {
        boolean isHappyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT) == 1;
        
        if (!isHappyHour) {
            if (Backpack.contains(BeachEventItems.PALMER_FARMER.getId()) && !hasBuffActive(BeachEventItems.PALMER_FARMER.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Palmer Farmer cocktail...");
                Backpack.interact(BeachEventItems.PALMER_FARMER.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.PINEAPPLETINI.getId()) && !hasBuffActive(BeachEventItems.PINEAPPLETINI.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Pineappletini cocktail...");
                Backpack.interact(BeachEventItems.PINEAPPLETINI.getId(), "Drink");
                Execution.delay(600);
            }
        } else {
            if (Backpack.contains(BeachEventItems.PINEAPPLETINI.getId()) && !hasBuffActive(BeachEventItems.PINEAPPLETINI.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Pineappletini cocktail...");
                Backpack.interact(BeachEventItems.PINEAPPLETINI.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.PALMER_FARMER.getId()) && !hasBuffActive(BeachEventItems.PALMER_FARMER.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Palmer Farmer cocktail...");
                Backpack.interact(BeachEventItems.PALMER_FARMER.getId(), "Drink");
                Execution.delay(600);
            }
        }
    }
    
    private void useRockPoolCocktail() {
        boolean isHappyHour = VarManager.getVarbitValue(HAPPY_HOUR_VARBIT) == 1;
        
        if (!isHappyHour) {
            if (Backpack.contains(BeachEventItems.FISHERMANS_FRIEND.getId()) && !hasBuffActive(BeachEventItems.FISHERMANS_FRIEND.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Fisherman's Friend cocktail...");
                Backpack.interact(BeachEventItems.FISHERMANS_FRIEND.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.PINEAPPLETINI.getId()) && !hasBuffActive(BeachEventItems.PINEAPPLETINI.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Pineappletini cocktail...");
                Backpack.interact(BeachEventItems.PINEAPPLETINI.getId(), "Drink");
                Execution.delay(600);
            }
        } else {
            if (Backpack.contains(BeachEventItems.PINEAPPLETINI.getId()) && !hasBuffActive(BeachEventItems.PINEAPPLETINI.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Pineappletini cocktail...");
                Backpack.interact(BeachEventItems.PINEAPPLETINI.getId(), "Drink");
                Execution.delay(600);
            } else if (Backpack.contains(BeachEventItems.FISHERMANS_FRIEND.getId()) && !hasBuffActive(BeachEventItems.FISHERMANS_FRIEND.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking Fisherman's Friend cocktail...");
                Backpack.interact(BeachEventItems.FISHERMANS_FRIEND.getId(), "Drink");
                Execution.delay(600);
            }
        }
    }
    
    private boolean hasBuffActive(int itemId) {
        // TODO: Implement buff checking logic
        return false;
    }
    
    public void handleBattleshipMessage(String message) {
        if (message.contains("Our accuracy penetrated their defences!") ||
            message.contains("Our defences withstood their aggression!") ||
            message.contains("Our aggression overcame their accuracy!")) {
            lastBattleshipMessage = message;
            ScriptConsole.println("[BeachEventTask] Battleship message received: " + message);
        }
    }
} 