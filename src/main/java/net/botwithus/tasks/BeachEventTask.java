package net.botwithus.tasks;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.Hud;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.Distance;
import net.botwithus.rs3.game.Item;
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
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;

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
    private BeachActivity selectedActivity = null;
    private boolean useCocktails = false;
    private boolean useBattleship = false;
    private boolean fightClawdia = true;
    private boolean useSpotlight = false;
    private String spotlightHappyHour = "Hunter";
    
    // Individual cocktail settings
    private boolean usePinkFizz = false;
    private boolean usePurpleLumbridge = false;
    private boolean usePineappletini = false;
    private boolean useLemonSour = false;
    private boolean useFishermanssFriend = false;
    private boolean useGeorgesPeachDelight = false;
    private boolean useAHoleInOne = false;
    private boolean usePalmerFarmer = false;
    private boolean useUglyDuckling = false;
    
    // State
    private boolean canDeployShip = true;
    private int failCount = 0;
    private String lastBattleshipMessage = "";
    
    // State for bodybuilding
    private int lastIvanAnimation = -1;
    private long lastAnimationChangeTime = 0;
    
    public BeachEventTask(CoaezUtility script) {
        this.script = script;
    }
    
    public void setSelectedActivity(BeachActivity activity) {
        this.selectedActivity = activity;
    }
    
    public BeachActivity getSelectedActivity() {
        return selectedActivity;
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
    
    public void setSpotlightHappyHour(String spotlightHappyHour) {
        this.spotlightHappyHour = spotlightHappyHour;
    }
    
    public boolean isUsePinkFizz() { return usePinkFizz; }
    public void setUsePinkFizz(boolean usePinkFizz) { this.usePinkFizz = usePinkFizz; }
    
    public boolean isUsePurpleLumbridge() { return usePurpleLumbridge; }
    public void setUsePurpleLumbridge(boolean usePurpleLumbridge) { this.usePurpleLumbridge = usePurpleLumbridge; }
    
    public boolean isUsePineappletini() { return usePineappletini; }
    public void setUsePineappletini(boolean usePineappletini) { this.usePineappletini = usePineappletini; }
    
    public boolean isUseLemonSour() { return useLemonSour; }
    public void setUseLemonSour(boolean useLemonSour) { this.useLemonSour = useLemonSour; }
    
    public boolean isUseFishermanssFriend() { return useFishermanssFriend; }
    public void setUseFishermanssFriend(boolean useFishermanssFriend) { this.useFishermanssFriend = useFishermanssFriend; }
    
    public boolean isUseGeorgesPeachDelight() { return useGeorgesPeachDelight; }
    public void setUseGeorgesPeachDelight(boolean useGeorgesPeachDelight) { this.useGeorgesPeachDelight = useGeorgesPeachDelight; }
    
    public boolean isUseAHoleInOne() { return useAHoleInOne; }
    public void setUseAHoleInOne(boolean useAHoleInOne) { this.useAHoleInOne = useAHoleInOne; }
    
    public boolean isUsePalmerFarmer() { return usePalmerFarmer; }
    public void setUsePalmerFarmer(boolean usePalmerFarmer) { this.usePalmerFarmer = usePalmerFarmer; }
    
    public boolean isUseUglyDuckling() { return useUglyDuckling; }
    public void setUseUglyDuckling(boolean useUglyDuckling) { this.useUglyDuckling = useUglyDuckling; }
    
    @Override
    public void execute() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[BeachEventTask] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        // Check if player has headbar ID 13 (indicating already interacting)
        if (player.getHeadbars().stream().anyMatch(headbar -> headbar.getId() == 13)) {
            ScriptConsole.println("[BeachEventTask] Player has active headbar ID 13, already interacting...");
            return;
        }

        if (shouldDrinkCocktails()) {
            drinkSelectedCocktails();
            Execution.delay(1200);
            return; 
        }
                

        
        ScriptConsole.println("[BeachEventTask] Execute called - Selected activity: " + (selectedActivity != null ? selectedActivity.getName() : "NULL"));

        if (player.getAnimationId() != -1 && selectedActivity != BeachActivity.BODY_BUILDING) {
            ScriptConsole.println("[BeachEventTask] Player is animating (" + player.getAnimationId() + "), waiting...");
            Execution.delay(script.getRandom().nextInt(1200, 4000));
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
        ScriptConsole.println("[BeachEventTask] Spotlight update - Activity ID: " + spotlightActivity + 
                             ", Is Happy Hour: " + isHappyHour + 
                             ", Happy Hour Preference: " + spotlightHappyHour);
        
        if (isHappyHour) {
            ScriptConsole.println("[BeachEventTask] Happy hour active, switching to preference: " + spotlightHappyHour);
            switch (spotlightHappyHour) {
                case "Dung":
                    selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
                    break;
                case "Strength":
                    selectedActivity = BeachActivity.BODY_BUILDING;
                    break;
                case "Construction":
                    if (!Client.isMember()) {
                        ScriptConsole.println("[BeachEventTask] Sandcastle building requires membership, switching to default activity");
                        selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
                    } else {
                        selectedActivity = BeachActivity.SANDCASTLE_BUILDING;
                    }
                    break;
                case "Hunter":
                    if (!Client.isMember()) {
                        ScriptConsole.println("[BeachEventTask] Hook-a-duck requires membership, switching to default activity");
                        selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
                    } else {
                        selectedActivity = BeachActivity.HOOK_A_DUCK;
                    }
                    break;
                case "Ranged":
                    selectedActivity = BeachActivity.COCONUT_SHY;
                    break;
                case "Cooking":
                    selectedActivity = BeachActivity.BARBEQUES;
                    break;
                case "Farming":
                    if (!Client.isMember()) {
                        ScriptConsole.println("[BeachEventTask] Palm tree farming requires membership, switching to default activity");
                        selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
                    } else {
                        selectedActivity = BeachActivity.PALM_TREE_FARMING;
                    }
                    break;
                default:
                    ScriptConsole.println("[BeachEventTask] Unknown happy hour preference: " + spotlightHappyHour);
            }
        } else {
            BeachActivity spotlightBeachActivity = BeachActivity.getById(spotlightActivity);
            ScriptConsole.println("[BeachEventTask] Normal time, following spotlight activity: " + spotlightBeachActivity);
            if (spotlightBeachActivity != null) {
                if (!Client.isMember() && (spotlightBeachActivity == BeachActivity.HOOK_A_DUCK || 
                                           spotlightBeachActivity == BeachActivity.SANDCASTLE_BUILDING || 
                                           spotlightBeachActivity == BeachActivity.PALM_TREE_FARMING)) {
                    ScriptConsole.println("[BeachEventTask] Spotlight activity " + spotlightBeachActivity + " requires membership, switching to default activity");
                    selectedActivity = BeachActivity.DUNGEONEERING_HOLE;
                } else {
                    selectedActivity = spotlightBeachActivity;
                }
                ScriptConsole.println("[BeachEventTask] Selected activity updated to: " + selectedActivity);
            } else {
                ScriptConsole.println("[BeachEventTask] Warning: Could not find activity for spotlight ID " + spotlightActivity);
            }
        }
        
        ScriptConsole.println("[BeachEventTask] Final selected activity: " + selectedActivity);
    }
    
    private boolean eatIceCream(int beachTemp, boolean isHappyHour) {
        // Check if we have any temperature-preventing cocktail buffs active
        if (hasTemperaturePreventingBuff()) {
            ScriptConsole.println("[BeachEventTask] Temperature-preventing cocktail buff active, skipping ice cream");
            return true;
        }
        
        if (beachTemp >= MAX_BEACH_TEMP && !isHappyHour) {
            ScriptConsole.println("[BeachEventTask] Beach temp at max (" + beachTemp + "), need to eat ice cream!");
            if (Backpack.contains("Ice cream")) {
                if (Backpack.interact("Ice cream", "Eat")) {
                    boolean brainFreezeOccurred = Execution.delayUntil(3000, () -> {
                        if (Interfaces.isOpen(1189)) {
                            Component brainFreezeComponent = ComponentQuery.newQuery(1189)
                                .componentIndex(3)
                                .results()
                                .first();
                            
                            if (brainFreezeComponent != null && brainFreezeComponent.getText() != null) {
                                return brainFreezeComponent.getText().contains("BRAIN FREEZE!");
                            }
                        }
                        return false;
                    });
                    
                    if (brainFreezeOccurred) {
                        ScriptConsole.println("[BeachEventTask] BRAIN FREEZE! occurred - stopping script");
                        LoginManager.setAutoLogin(false);
                        Hud.logout();
                        script.setActive(false);
                        return false;
                    }
                    
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
    
    private boolean hasTemperaturePreventingBuff() {
        // Check for temperature-preventing cocktail buffs using their specific varcs
        // A Hole in One (Dungeoneering) - varc 6925
        if ((VarManager.getVarc(6925) - Client.getClientCycle()) > 0) {
            int remainingTimer = VarManager.getVarc(6925) - Client.getClientCycle();
            ScriptConsole.println("[BeachEventTask] A Hole in One buff active, remaining timer: " + remainingTimer);
            return true;
        }
        
        // Ugly Duckling (Hook a Duck) - varc 6926
        if ((VarManager.getVarc(6926) - Client.getClientCycle()) > 0) {
            int remainingTimer = VarManager.getVarc(6926) - Client.getClientCycle();
            ScriptConsole.println("[BeachEventTask] Ugly Duckling buff active, remaining timer: " + remainingTimer);
            return true;
        }
        
        // Palmer Farmer (Palm Tree Farming) - varc 6927
        if ((VarManager.getVarc(6927) - Client.getClientCycle()) > 0) {
            int remainingTimer = VarManager.getVarc(6927) - Client.getClientCycle();
            ScriptConsole.println("[BeachEventTask] Palmer Farmer buff active, remaining timer: " + remainingTimer);
            return true;
        }
        
        // Fisherman's Friend (Rock Pools) - varc 6928
        if ((VarManager.getVarc(6928) - Client.getClientCycle()) > 0) {
            int remainingTimer = VarManager.getVarc(6928) - Client.getClientCycle();
            ScriptConsole.println("[BeachEventTask] Fisherman's Friend buff active, remaining timer: " + remainingTimer);
            return true;
        }
        
        return false;
    }
    
    private boolean handleClawdia() {
        EntityResultSet<Npc> clawdiaResults = NpcQuery.newQuery()
            .name("Clawdia")
            .results();
        
        Npc clawdia = clawdiaResults.nearest();
        
        if (clawdia != null) {
            ScriptConsole.println("[BeachEventTask] Clawdia is present! Prioritizing fight over other activities.");
            
            PathingEntity<?> currentTarget = Client.getLocalPlayer().getTarget();
            
            if (currentTarget == null) {
                LocalPlayer player = Client.getLocalPlayer();
                if (player != null) {
                    ScriptConsole.println("[BeachEventTask] Found Clawdia, attacking...");
                    if (clawdia.interact("Attack")) {
                        Execution.delay(600);
                    }
                }
            } else {
                ScriptConsole.println("[BeachEventTask] Already fighting Clawdia, continuing combat...");
            }
            
            return true;
        }
        
        return false;
    }
    
    private void handleBattleship() {
        if (!Backpack.contains(33769)) {
            ScriptConsole.println("[BeachEventTask] No battleship in inventory");
            return;
        }
        
        if (!canDeployShip) {
            ScriptConsole.println("[BeachEventTask] Ship already deployed, waiting for it to die");
            return;
        }
        
        if (lastBattleshipMessage.isEmpty()) {
            ScriptConsole.println("[BeachEventTask] No battleship message, deploying default aggressive ship");
            if (Backpack.interact("Toy royal battleship", "Deploy")) {
                boolean interfaceOpened = Execution.delayUntil(3000, () -> 
                    Interfaces.isOpen(751));
                
                if (interfaceOpened) {
                    ScriptConsole.println("[BeachEventTask] Deploying default aggressive ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217602);
                    canDeployShip = false;
                    Execution.delay(1200);
                    return;
                }
            }
            return;
        }
        
        ScriptConsole.println("[BeachEventTask] Deploying battleship based on message: " + lastBattleshipMessage);
        
        if (Backpack.interact("Toy royal battleship", "Deploy")) {
            boolean interfaceOpened = Execution.delayUntil(3000, () -> 
                Interfaces.isOpen(751));
            
            if (interfaceOpened) {
                if (lastBattleshipMessage.contains("Our accuracy penetrated their defences!")) {
                    ScriptConsole.println("[BeachEventTask] Deploying aggressive ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217602);
                } else if (lastBattleshipMessage.contains("Our defences withstood their aggression!")) {
                    ScriptConsole.println("[BeachEventTask] Deploying accurate ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217586);
                } else if (lastBattleshipMessage.contains("Our aggression overcame their accuracy!")) {
                    ScriptConsole.println("[BeachEventTask] Deploying defensive ship");
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 49217594);
                }
                
                lastBattleshipMessage = "";
                canDeployShip = false;
                Execution.delay(1200);
            } else {
                ScriptConsole.println("[BeachEventTask] Failed to open battleship interface");
            }
        }
    }
    
    private void executeActivity() {
        if (selectedActivity == null) {
            ScriptConsole.println("[BeachEventTask] No activity selected! Please configure an activity in the GUI.");
            return;
        }
        

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
    
    private boolean shouldDrinkCocktails() {
        if (!usePinkFizz && !usePurpleLumbridge && !usePineappletini && !useLemonSour && 
            !useFishermanssFriend && !useGeorgesPeachDelight && !useAHoleInOne && 
            !usePalmerFarmer && !useUglyDuckling) {
            return false;
        }
        
        List<Item> backpackItems = Backpack.getItems();
        if (backpackItems == null || backpackItems.isEmpty()) {
            return false;
        }
        
        List<Integer> enabledCocktailIds = new ArrayList<>();
        if (usePinkFizz) enabledCocktailIds.add(BeachEventItems.PINK_FIZZ.getId());
        if (usePurpleLumbridge) enabledCocktailIds.add(BeachEventItems.PURPLE_LUMBRIDGE.getId());
        if (usePineappletini) enabledCocktailIds.add(BeachEventItems.PINEAPPLETINI.getId());
        if (useLemonSour) enabledCocktailIds.add(BeachEventItems.LEMON_SOUR.getId());
        if (useFishermanssFriend) enabledCocktailIds.add(BeachEventItems.FISHERMANS_FRIEND.getId());
        if (useGeorgesPeachDelight) enabledCocktailIds.add(BeachEventItems.GEORGES_PEACH_DELIGHT.getId());
        if (useAHoleInOne) enabledCocktailIds.add(BeachEventItems.A_HOLE_IN_ONE.getId());
        if (usePalmerFarmer) enabledCocktailIds.add(BeachEventItems.PALMER_FARMER.getId());
        if (useUglyDuckling) enabledCocktailIds.add(BeachEventItems.UGLY_DUCKLING.getId());
        
        for (Item item : backpackItems) {
            if (item != null && enabledCocktailIds.contains(item.getId()) && !hasBuffActive(item.getId())) {
                return true;
            }
        }
        
        return false;
    }
    
    private void executeDungeoneering() {
        if (cachedDungeoneeringHole == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Dungeoneering hole")
                .option("Dungeoneer")
                .results();
            cachedDungeoneeringHole = results.nearest();
        }
        
        if (cachedDungeoneeringHole != null) {
            ScriptConsole.println("[BeachEventTask] Interacting with dungeoneering hole...");
            if (cachedDungeoneeringHole.interact("Dungeoneer")) {
            }
        }
    }
    
    private void executeBodybuilding() {
        if (cachedBodybuilding == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Body building podium")
                .option("Workout")
                .results();
            cachedBodybuilding = results.nearest();
        }
        
        if (cachedBodybuilding != null) {
            boolean workoutInterfaceOpen = Interfaces.isOpen(796);
            ScriptConsole.println("[BeachEventTask] Workout interface 796 open: " + workoutInterfaceOpen);
            
            if (!workoutInterfaceOpen) {
                ScriptConsole.println("[BeachEventTask] Workout interface not open, interacting with platform...");
                if (cachedBodybuilding.interact("Workout")) {
                    Execution.delayUntil(8000, () -> Interfaces.isOpen(796));
                }
                return;
            }
            
            EntityResultSet<Npc> ivanResults = NpcQuery.newQuery()
                .name("Ivan")
                .results();
            
            Npc ivan = ivanResults.nearest();
            if (ivan != null) {
                int currentIvanAnimation = ivan.getAnimationId();
                long currentTime = System.currentTimeMillis();
                
                ScriptConsole.println("[BeachEventTask] Ivan found - Animation: " + currentIvanAnimation + 
                                    ", Last: " + lastIvanAnimation + 
                                    ", Time since change: " + (currentTime - lastAnimationChangeTime) + "ms");
                
                // Only consider it an animation change if both old and new animations are not -1 (not idle)
                if (currentIvanAnimation != lastIvanAnimation && 
                    currentIvanAnimation != -1 && 
                    lastIvanAnimation != -1) {
                    lastIvanAnimation = currentIvanAnimation;
                    lastAnimationChangeTime = currentTime;
                    ScriptConsole.println("[BeachEventTask] Ivan animation changed to: " + currentIvanAnimation + " (meaningful change)");
                } else if (currentIvanAnimation != -1 && lastIvanAnimation == -1) {
                    lastIvanAnimation = currentIvanAnimation;
                    ScriptConsole.println("[BeachEventTask] Ivan started animating: " + currentIvanAnimation + " (initial animation)");
                }
                
                LocalPlayer player = Client.getLocalPlayer();
                if (player != null) {
                    int playerAnimation = player.getAnimationId();
                    ScriptConsole.println("[BeachEventTask] Player animation: " + playerAnimation + 
                                        ", Ivan animation: " + currentIvanAnimation);
                    

                    boolean shouldSwitch = currentIvanAnimation != -1 && (
                        (currentTime - lastAnimationChangeTime <= 5000) || 
                        (playerAnimation != currentIvanAnimation)
                    );
                    
                    if (shouldSwitch) {
                        switch (currentIvanAnimation) {
                            case 26552: // Curl
                                if (playerAnimation != 26552) {
                                    ScriptConsole.println("[BeachEventTask] Switching to curl workout (Ivan: " + currentIvanAnimation + ")");
                                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 52166662);
                                    Execution.delay(800);
                                }
                                break;
                            case 26553: // Lunge
                                if (playerAnimation != 26553) {
                                    ScriptConsole.println("[BeachEventTask] Switching to lunge workout (Ivan: " + currentIvanAnimation + ")");
                                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 52166672);
                                    Execution.delay(800);
                                }
                                break;
                            case 26554: // Fly
                                if (playerAnimation != 26554) {
                                    ScriptConsole.println("[BeachEventTask] Switching to fly workout (Ivan: " + currentIvanAnimation + ")");
                                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 52166682);
                                    Execution.delay(800);
                                }
                                break;
                            case 26549: // Raise
                                if (playerAnimation != 26549) {
                                    ScriptConsole.println("[BeachEventTask] Switching to raise workout (Ivan: " + currentIvanAnimation + ")");
                                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 52166692);
                                    Execution.delay(800);
                                }
                                break;
                            default:
                                if (playerAnimation == -1) {
                                    ScriptConsole.println("[BeachEventTask] Starting default workout (Ivan unknown anim: " + currentIvanAnimation + ")");
                                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 52166662); // Default to curl
                                    Execution.delay(800);
                                } else {
                                    ScriptConsole.println("[BeachEventTask] Unknown Ivan animation: " + currentIvanAnimation + ", player: " + playerAnimation);
                                }
                        }
                    } else {
                        ScriptConsole.println("[BeachEventTask] No workout switch needed - animations match or no recent change");
                    }
                }
            } else {
                ScriptConsole.println("[BeachEventTask] Ivan not found! Searching for Ivan NPC...");
                // Try to find Ivan with ID instead
                EntityResultSet<Npc> ivanByIdResults = NpcQuery.newQuery()
                    .id(BeachEventNPCs.IVAN.getId())
                    .results();
                
                Npc ivanById = ivanByIdResults.nearest();
                if (ivanById != null) {
                    ScriptConsole.println("[BeachEventTask] Found Ivan by ID: " + ivanById.getName() + " (ID: " + ivanById.getId() + ")");
                } else {
                    ScriptConsole.println("[BeachEventTask] Ivan not found by name or ID, using default workout");
                    LocalPlayer player = Client.getLocalPlayer();
                    if (player != null && player.getAnimationId() == -1) {
                        ScriptConsole.println("[BeachEventTask] Starting default curl workout");
                        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 52166662);
                        Execution.delay(800);
                    }
                }
            }
        }
    }
    
    private void executeSandcastleBuilding() {
        if (!Client.isMember()) {
            ScriptConsole.println("[BeachEventTask] Sandcastle building requires membership, skipping activity");
            return;
        }
        
        EntityResultSet<SceneObject> lumbridgeResults = SceneObjectQuery.newQuery()
            .name("Lumbridge Sandcastle")
            .option("Build")
            .results();
        
        EntityResultSet<SceneObject> wizardsResults = SceneObjectQuery.newQuery()
            .name("Wizards' Sandtower")
            .option("Build")
            .results();
        
        EntityResultSet<SceneObject> pyramidResults = SceneObjectQuery.newQuery()
            .name("Sand Pyramid")
            .option("Build")
            .results();
        
        EntityResultSet<SceneObject> exchangeResults = SceneObjectQuery.newQuery()
            .name("Sand Exchange")
            .option("Build")
            .results();
        
        // Find the nearest available sandcastle
        SceneObject sandcastle = null;
        if (!lumbridgeResults.isEmpty()) {
            sandcastle = lumbridgeResults.nearest();
            ScriptConsole.println("[BeachEventTask] Found Lumbridge Sandcastle");
        } else if (!wizardsResults.isEmpty()) {
            sandcastle = wizardsResults.nearest();
            ScriptConsole.println("[BeachEventTask] Found Wizards' Sandtower");
        } else if (!pyramidResults.isEmpty()) {
            sandcastle = pyramidResults.nearest();
            ScriptConsole.println("[BeachEventTask] Found Sand Pyramid");
        } else if (!exchangeResults.isEmpty()) {
            sandcastle = exchangeResults.nearest();
            ScriptConsole.println("[BeachEventTask] Found Sand Exchange");
        }
        
        if (sandcastle != null) {
            ScriptConsole.println("[BeachEventTask] Building sandcastle: " + sandcastle.getName());
            if (sandcastle.interact("Build")) {
                ScriptConsole.println("[BeachEventTask] Successfully interacted with sandcastle");
                Execution.delay(6000);
            } else {
                ScriptConsole.println("[BeachEventTask] Failed to interact with sandcastle");
            }
        } else if (sandcastle == null) {
            ScriptConsole.println("[BeachEventTask] No sandcastles found to build");
        }
    }
    
    private void executeHookADuck() {
        if (!Client.isMember()) {
            ScriptConsole.println("[BeachEventTask] Hook-a-duck requires membership, skipping activity");
            return;
        }
        
        if (cachedHookADuck == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.HOOK_A_DUCK.getId())
                .results();
            cachedHookADuck = results.nearest();
        }
        
        if (cachedHookADuck != null) {
            ScriptConsole.println("[BeachEventTask] Playing hook-a-duck...");
            if (cachedHookADuck.interact("Play")) {
                ScriptConsole.println("[BeachEventTask] Go catch dat ducky!");
            }
        }
    }
    
    private void executeCoconutShy() {
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
                Execution.delay(script.getRandom().nextInt(1200, 4000));
            }
        }
    }
    
    private void executeBarbeques() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player != null && player.getAnimationId() == -1) {
            // Player is not animating, try to interact with grill
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .name("Grill")
                .option("Use")
                .results();
            
            SceneObject grill = results.nearest();
            if (grill != null) {
                ScriptConsole.println("[BeachEventTask] Player not animating, interacting with grill...");
                if (grill.interact("Use")) {
                    ScriptConsole.println("[BeachEventTask] Started using grill!");
                }
                return;
            }
        }
        
        if (cachedBarbeque == null) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery()
                .id(BeachEventObjects.BARBEQUE_GRILL.getId())
                .results();
            cachedBarbeque = results.nearest();
        }
        
        if (cachedBarbeque != null) {
            ScriptConsole.println("[BeachEventTask] Using barbeque...");
            if (cachedBarbeque.interact("Cook")) {
                ScriptConsole.println("[BeachEventTask] Get that fish cooked!");
            }
        }
    }

    private Coordinate pileOfCoconutsCoordinate = new Coordinate(3172, 3215,0);
    
    private void executePalmTreeFarming() {
        if (!Client.isMember()) {
            ScriptConsole.println("[BeachEventTask] Palm tree farming requires membership, skipping activity");
            return;
        }
        
        if (Backpack.isFull() && Backpack.contains("Tropical coconut")) {
            EntityResultSet<SceneObject> pileResults = SceneObjectQuery.newQuery()
                .name("Pile of coconuts")
                .hidden(false)
                .results();
            
            SceneObject pile = pileResults.nearest();
            if (pile != null && pile.distanceTo(Client.getLocalPlayer().getCoordinate()) < 20 && !Client.getLocalPlayer().isMoving()) {
                ScriptConsole.println("[BeachEventTask] Inventory full, depositing coconuts...");
                pile.interact("Deposit coconuts");
                return;
            } else {
                ScriptConsole.println("[BeachEventTask] Pile of coconuts is too far, Moving closer");
                if(!Client.getLocalPlayer().isMoving()) {
                    Movement.walkTo(pileOfCoconutsCoordinate.getX(), pileOfCoconutsCoordinate.getY(), true);
                }
                
            }
        } else {
        
        EntityResultSet<SceneObject> treeResults = SceneObjectQuery.newQuery()
            .ids(BeachEventObjects.getPalmTrees())
            .hidden(false)
            .results();
        
        SceneObject tree = treeResults.nearest();
        if (tree != null) {
            ScriptConsole.println("[BeachEventTask] Chopping palm tree...");
            if (tree.interact("Pick coconut")) {
                ScriptConsole.println("[BeachEventTask] Back to chopping trees.");
            }
        }
    }
    }
    
    private void executeRockPools() {
        
        if (Backpack.isFull() && Backpack.contains(35106)) {
            EntityResultSet<Npc> results = NpcQuery.newQuery().name("Wellington").option("Hand in fish").results();
            
            Npc wellington = results.nearest();
            if (wellington != null) {
                ScriptConsole.println("[BeachEventTask] Inventory full, depositing fish...");
                wellington.interact("Hand in fish");
                return;
            }
        }
        
        EntityResultSet<Npc> results = NpcQuery.newQuery().name("Fishing spot").option("Catch").results();
        
        Npc fishingSpot = results.nearest();
        if (fishingSpot != null) {
            ScriptConsole.println("[BeachEventTask] Fishing at rock pools...");
            if (fishingSpot.interact("Catch")) {
                ScriptConsole.println("[BeachEventTask] Back to fishing.");
            }
        }
    }
    
    private void drinkSelectedCocktails() {
        List<Item> backpackItems = Backpack.getItems();
        if (backpackItems == null || backpackItems.isEmpty()) {
            return;
        }
        
        List<Integer> enabledCocktailIds = new ArrayList<>();
        if (usePinkFizz) enabledCocktailIds.add(BeachEventItems.PINK_FIZZ.getId());
        if (usePurpleLumbridge) enabledCocktailIds.add(BeachEventItems.PURPLE_LUMBRIDGE.getId());
        if (usePineappletini) enabledCocktailIds.add(BeachEventItems.PINEAPPLETINI.getId());
        if (useLemonSour) enabledCocktailIds.add(BeachEventItems.LEMON_SOUR.getId());
        if (useFishermanssFriend) enabledCocktailIds.add(BeachEventItems.FISHERMANS_FRIEND.getId());
        if (useGeorgesPeachDelight) enabledCocktailIds.add(BeachEventItems.GEORGES_PEACH_DELIGHT.getId());
        if (useAHoleInOne) enabledCocktailIds.add(BeachEventItems.A_HOLE_IN_ONE.getId());
        if (usePalmerFarmer) enabledCocktailIds.add(BeachEventItems.PALMER_FARMER.getId());
        if (useUglyDuckling) enabledCocktailIds.add(BeachEventItems.UGLY_DUCKLING.getId());
        
        for (Item item : backpackItems) {
            if (item != null && enabledCocktailIds.contains(item.getId()) && !hasBuffActive(item.getId())) {
                ScriptConsole.println("[BeachEventTask] Drinking cocktail: " + item.getName() + " from slot " + item.getSlot());
                if (Backpack.interact(item.getSlot(), "Drink")) {
                    ScriptConsole.println("[BeachEventTask] Successfully interacted with cocktail in slot " + item.getSlot());
                } else {
                    ScriptConsole.println("[BeachEventTask] Failed to interact with cocktail in slot " + item.getSlot());
                }
                Execution.delay(600);
                return;
            }
        }
    }
    
    private boolean hasBuffActive(int itemId) {
        int varcValue;
        int remainingTimer;
        switch (itemId) {
            case 35051: // Pink fizz
                varcValue = VarManager.getVarc(6921);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Pink Fizz buff - Varc 6921: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 35052: // Purple Lumbridge
                varcValue = VarManager.getVarc(6922);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Purple Lumbridge buff - Varc 6922: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 35053: // Pineappletini
                varcValue = VarManager.getVarc(6923);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Pineappletini buff - Varc 6923: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 35054: // Lemon sour
                varcValue = VarManager.getVarc(6924);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Lemon Sour buff - Varc 6924: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 51729: // A Hole in One
                varcValue = VarManager.getVarc(6925);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking A Hole in One buff - Varc 6925: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 51730: // Ugly Duckling (hook a duck)
                varcValue = VarManager.getVarc(6926);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Ugly Duckling buff - Varc 6926: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 51731: // Palmer Farmer (palm tree)
                varcValue = VarManager.getVarc(6927);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Palmer Farmer buff - Varc 6927: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 51732: // Fishermans Friend (rock pools)
                varcValue = VarManager.getVarc(6928);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking Fishermans Friend buff - Varc 6928: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            case 51733: // George's Peach Delight (sandcastle)
                varcValue = VarManager.getVarc(6929);
                remainingTimer = varcValue - Client.getClientCycle();
                ScriptConsole.println("[BeachEventTask] Checking George's Peach Delight buff - Varc 6929: " + varcValue + ", Timer: " + remainingTimer);
                return remainingTimer > 0;
            default:
                ScriptConsole.println("[BeachEventTask] Unknown cocktail item ID: " + itemId);
                return false;
        }
    }
    
    public void handleBattleshipMessage(String message) {
        if (message.contains("Our accuracy penetrated their defences!") ||
            message.contains("Our defences withstood their aggression!") ||
            message.contains("Our aggression overcame their accuracy!")) {
            lastBattleshipMessage = message;
            ScriptConsole.println("[BeachEventTask] Battleship message received: " + message);
        }
        
        if (message.contains("battleship was defeated")) {
            canDeployShip = true;
            ScriptConsole.println("[BeachEventTask] Ship is dead, can deploy new ship");
        }
    }
} 