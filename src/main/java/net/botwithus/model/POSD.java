package net.botwithus.model;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Equipment;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.api.game.hud.inventories.LootInventory;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.Distance;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.movement.TraverseEvent;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.GroundItemQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.item.GroundItem;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.util.RandomGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POSD {
    private final CoaezUtility script;
    private final Random random = new Random();
    
    // Configuration options
    private boolean interactWithLootAll = false;
    private boolean useLoot = false;
    private List<String> targetItemNames = new ArrayList<>();
    private boolean quickPrayersActive = false;
    private int currentStep = 1;
    private int quickPrayersNumber = 4;
    private boolean useOverloads = false;
    private boolean usePrayerPots = false;
    private boolean useAggroPots = false;
    private int prayerPointsThreshold = 5000;
    private int healthPointsThreshold = 50;
    private boolean useWeaponPoison = false;
    private boolean useQuickPrayers = false;
    private String selectedItem = "";
    private boolean useScrimshaws = false;
    private boolean bankForFood = false;

    public POSD(CoaezUtility script) {
        this.script = script;
    }

    public void processLooting() {
        if (Backpack.isFull()) {
            ScriptConsole.println("[POSD] Backpack is full. Cannot loot more items.");
        } else {
            if (Interfaces.isOpen(1622)) {
                this.lootFromInventory();
            } else {
                LootInventory.open();
                this.lootFromGround();
            }
        }
    }

    private Pattern generateLootPattern(List<String> names) {
        return Pattern.compile(names.stream()
                .map(Pattern::quote)
                .reduce((name1, name2) -> name1 + "|" + name2)
                .orElse(""), Pattern.CASE_INSENSITIVE);
    }

    private boolean canLoot() {
        return !targetItemNames.isEmpty();
    }

    public void lootFromInventory() {
        if (!this.canLoot()) {
            ScriptConsole.println("[POSD] No target items specified for looting.");
        } else {
            Pattern lootPattern = this.generateLootPattern(targetItemNames);
            List<Item> inventoryItems = LootInventory.getItems();
            
            for (Item item : inventoryItems) {
                if (item.getName() != null) {
                    Matcher matcher = lootPattern.matcher(item.getName());
                    if (matcher.find()) {
                        LootInventory.take(item.getName());
                        ScriptConsole.println("[POSD] Successfully looted item: " + item.getName());
                    }
                }
            }
        }
    }

    public void lootFromGround() {
        if (targetItemNames.isEmpty()) {
            ScriptConsole.println("[POSD] No target items specified for looting.");
        } else if (LootInventory.isOpen()) {
            ScriptConsole.println("[POSD] Loot interface is open, skipping ground looting.");
        } else {
            Pattern lootPattern = this.generateLootPattern(targetItemNames);
            List<GroundItem> groundItems = GroundItemQuery.newQuery().results().stream().toList();
            
            for (GroundItem groundItem : groundItems) {
                if (groundItem.getName() != null) {
                    Matcher matcher = lootPattern.matcher(groundItem.getName());
                    if (matcher.find()) {
                        groundItem.interact("Take");
                        ScriptConsole.println("[POSD] Interacted with: " + groundItem.getName() + " on the ground.");
                        Execution.delay(5000L);
                    }
                }
            }
        }
    }

    public void lootEverything() {
        if (Interfaces.isOpen(1622)) {
            this.lootAll();
        } else {
            this.lootInterface();
            Execution.delayUntil(10000L, () -> Interfaces.isOpen(1622));
        }
    }

    public void lootAll() {
        EntityResultSet<GroundItem> groundItems = GroundItemQuery.newQuery().results();
        if (!groundItems.isEmpty()) {
            Execution.delay(RandomGenerator.nextInt(1500, 2000));
            ComponentQuery lootAllQuery = ComponentQuery.newQuery(1622);
            List<Component> components = lootAllQuery.componentIndex(22).results().stream().toList();
            if (!components.isEmpty() && components.get(0).interact(1)) {
                ScriptConsole.println("[POSD] Successfully interacted with Loot All.");
                Execution.delay(RandomGenerator.nextInt(806, 1259));
            }
        }
    }

    public void lootInterface() {
        EntityResultSet<GroundItem> groundItems = GroundItemQuery.newQuery().results();
        if (!groundItems.isEmpty() && !Backpack.isFull()) {
            GroundItem groundItem = groundItems.nearest();
            if (groundItem != null) {
                groundItem.interact("Take");
                Execution.delayUntil(RandomGenerator.nextInt(5000, 5500), () -> Client.getLocalPlayer().isMoving());
                
                if (Client.getLocalPlayer().isMoving() && groundItem.getCoordinate() != null && 
                        Distance.between(Client.getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) > 10.0D) {
                    ScriptConsole.println("[POSD] Used Surge: " + ActionBar.useAbility("Surge"));
                    Execution.delay(RandomGenerator.nextInt(200, 250));
                }

                if (groundItem.getCoordinate() != null) {
                    Execution.delayUntil(RandomGenerator.nextInt(100, 200), () -> 
                        Distance.between(Client.getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) <= 10.0D);
                }

                if (groundItem.interact("Take")) {
                    ScriptConsole.println("[POSD] Taking " + groundItem.getName() + "...");
                    Execution.delay(RandomGenerator.nextInt(600, 700));
                }

                boolean interfaceOpened = Execution.delayUntil(15000L, () -> Interfaces.isOpen(1622));
                if (!interfaceOpened) {
                    ScriptConsole.println("[POSD] Interface 1622 did not open. Attempting to interact with ground item again.");
                    if (groundItem.interact("Take")) {
                        ScriptConsole.println("[POSD] Attempting to take " + groundItem.getName() + " again...");
                        Execution.delay(RandomGenerator.nextInt(250, 300));
                    }
                }

                this.lootAll();
            }
        }
    }

    public void handlePOD() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            ScriptConsole.println("[POSD] Player is null, waiting...");
            Execution.delay(1200);
            return;
        }
        
        // Check for Death NPC
        Npc death = NpcQuery.newQuery().name("Death").results().nearest();
        if (death != null) {
            Execution.delay(5000L);
            LoginManager.setAutoLogin(false);
            MiniMenu.interact(14, 1, -1, 93913156);
            Execution.delay(5000L);
            script.setActive(false);
            return;
        }
        
        // Check auto retaliate
        if (VarManager.getVarValue(VarDomainType.PLAYER, 462) == 1) {
            ScriptConsole.println("[POSD] Auto Retaliate is off, turning it on");
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93716537);
            Execution.delay(RandomGenerator.nextInt(1000, 3000));
            ScriptConsole.println("[POSD] Auto Retaliate enabled");
            return;
        }
        
        if (useQuickPrayers) {
            this.manageQuickPrayers(player);
        }
        
        switch (this.currentStep) {
            case 1:
                if (this.travelToPOD()) {
                    ScriptConsole.println("[POSD] Arrived at POD. Proceeding to interaction.");
                    this.currentStep = 2;
                } else {
                    ScriptConsole.println("[POSD] Traveling to POD...");
                }
                break;
            case 2:
                if (this.interactWithKags()) {
                    ScriptConsole.println("[POSD] Interacted with Kags. Proceeding to the next step.");
                    this.currentStep = 3;
                }
                break;
            case 3:
                if (this.interactWithFirstDoor()) {
                    ScriptConsole.println("[POSD] Interacted with the first door. Proceeding to the next step.");
                    this.currentStep = 4;
                }
                break;
            case 4:
                if (this.interactWithOtherDoor()) {
                    ScriptConsole.println("[POSD] Interacted with the other door. Proceeding to the next step.");
                    this.currentStep = 5;
                }
                break;
            case 5:
                if (this.movePlayerEast()) {
                    ScriptConsole.println("[POSD] Moved player east. Proceeding to the next step.");
                    this.currentStep = 6;
                }
                break;
            case 6:
                this.attackTarget(player);
                if (this.shouldBank(player)) {
                    this.currentStep = 7;
                }
                
                if (useLoot) {
                    processLooting();
                }
                if (interactWithLootAll) {
                    lootEverything();
                }
                break;
            case 7:
                if (this.bankingForPOD(player)) {
                    this.currentStep = 1;
                }
                break;
            default:
                ScriptConsole.println("[POSD] Invalid step. Please check the process flow.");
        }
    }

    public boolean travelToPOD() {
        NavPath path = NavPath.resolve(new Coordinate(3122, 2632, 0));
        return Movement.traverse(path) == TraverseEvent.State.FINISHED;
    }

    private boolean shouldBank(LocalPlayer player) {
        long overloadCheck = drinkOverloads(player);
        long prayerCheck = usePrayerOrRestorePots(player);
        long aggroCheck = useAggression(player);
        long weaponPoisonCheck = useWeaponPoison(player);
        return (useWeaponPoison && weaponPoisonCheck == 1L) || 
               (useOverloads && overloadCheck == 1L) || 
               (usePrayerPots && prayerCheck == 1L) || 
               (useAggroPots && aggroCheck == 1L);
    }

    public boolean interactWithKags() {
        EntityResultSet<Npc> kags = NpcQuery.newQuery().name("Portmaster Kags").option("Travel").results();
        if (!kags.isEmpty()) {
            Npc nearestKags = kags.nearest();
            if (nearestKags != null && nearestKags.interact("Travel")) {
                Execution.delayUntil(5000L, () -> Interfaces.isOpen(1188));
                if (Interfaces.isOpen(1188)) {
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77856776);
                    Execution.delay(RandomGenerator.nextInt(5000, 8000));
                    return true;
                }
            }
        }
        return false;
    }

    public boolean interactWithFirstDoor() {
        ScriptConsole.println("[POSD] Searching for door to interact with...");
        EntityResultSet<SceneObject> door = SceneObjectQuery.newQuery().name("Door").option("Open").results();
        
        if (door.isEmpty()) {
            ScriptConsole.println("[POSD] No door found with name 'Door' and option 'Open'");
            return false;
        }
        
        ScriptConsole.println("[POSD] Found " + door.size() + " doors matching criteria");
        SceneObject nearestDoor = door.nearest();
        
        if (nearestDoor == null) {
            ScriptConsole.println("[POSD] Nearest door is null");
            return false;
        }
        
        ScriptConsole.println("[POSD] Attempting to interact with door at position: " + nearestDoor.getCoordinate());
        if (nearestDoor.interact("Open")) {
            ScriptConsole.println("[POSD] Successfully interacted with door, waiting for response...");
            Execution.delay(RandomGenerator.nextInt(5000, 8000));
            return true;
        } else {
            ScriptConsole.println("[POSD] Failed to interact with door");
            return false;
        }
    }

    public boolean interactWithOtherDoor() {
        EntityResultSet<SceneObject> otherDoor = SceneObjectQuery.newQuery().name("Barrier").option("Pass through").results();
        if (!otherDoor.isEmpty()) {
            SceneObject nearestOtherDoor = otherDoor.nearest();
            if (nearestOtherDoor != null && nearestOtherDoor.interact("Pass through")) {
                Execution.delay(RandomGenerator.nextInt(5000, 8000));
                return true;
            }
        }
        return false;
    }

    public boolean movePlayerEast() {
        if (Client.getLocalPlayer() != null) {
            Coordinate targetCoordinate = Client.getLocalPlayer().getCoordinate();
            Movement.walkTo(targetCoordinate.getX() + 7, targetCoordinate.getY(), true);
        }
        return true;
    }

    public boolean bankingForPOD(LocalPlayer player) {
        if (VarManager.getVarbitValue(16779) == 1) {
            ActionBar.useAbility("Soul Split");
        }
        ActionBar.useAbility("War's Retreat Teleport");
        Execution.delay(RandomGenerator.nextInt(6000, 8000));
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Bank chest").option("Use").results();
        if (!results.isEmpty()) {
            SceneObject chest = results.nearest();
            if (chest != null) {
                chest.interact("Load Last Preset from");
                Execution.delay(RandomGenerator.nextInt(6000, 8000));
            }
        }
        return true;
    }

    public void manageQuickPrayers(LocalPlayer player) {
        if (player.inCombat() && !this.quickPrayersActive) {
            this.updateQuickPrayersActivation(player);
        } else if (!player.inCombat() && this.quickPrayersActive) {
            this.updateQuickPrayersActivation(player);
        }
    }

    private void updateQuickPrayersActivation(LocalPlayer player) {
        boolean isCurrentlyActive = this.isQuickPrayersActive();
        boolean shouldBeActive = this.shouldActivateQuickPrayers(player);
        if (shouldBeActive && !isCurrentlyActive) {
            this.activateQuickPrayers();
        } else if (!shouldBeActive && isCurrentlyActive) {
            this.deactivateQuickPrayers();
        }
    }

    private void activateQuickPrayers() {
        if (!this.quickPrayersActive) {
            ScriptConsole.println("[POSD] Activating Quick Prayers.");
            if (ActionBar.useAbility("Quick-prayers " + quickPrayersNumber)) {
                ScriptConsole.println("[POSD] Quick Prayers activated successfully.");
                this.quickPrayersActive = true;
            } else {
                ScriptConsole.println("[POSD] Failed to activate Quick Prayers.");
            }
        }
    }

    public long useWeaponPoison(LocalPlayer player) {
        if (!useWeaponPoison) {
            return random.nextLong(300L, 750L);
        } else if (player != null && player.getAnimationId() != 18068 && VarManager.getVarbitValue(2102) <= 3) {
            Pattern poisonPattern = Pattern.compile("weapon poison\\+*?", Pattern.CASE_INSENSITIVE);
            Item weaponPoisonItem = InventoryItemQuery.newQuery().results().stream()
                    .filter(item -> item.getName() != null && poisonPattern.matcher(item.getName()).find())
                    .findFirst().orElse(null);
                    
            if (weaponPoisonItem == null) {
                ScriptConsole.println("[POSD] No weapon poison found in the Backpack.");
                return 1L;
            } else {
                boolean success = Backpack.interact(weaponPoisonItem.getName(), "Apply");
                if (success) {
                    ScriptConsole.println("[POSD] Successfully applied " + weaponPoisonItem.getName());
                    long delay = random.nextLong(1500L, 3000L);
                    Execution.delay(delay);
                    return delay;
                } else {
                    ScriptConsole.println("[POSD] Failed to apply weapon poison.");
                    return 0L;
                }
            }
        } else {
            return 0L;
        }
    }

    private void deactivateQuickPrayers() {
        if (this.quickPrayersActive) {
            ScriptConsole.println("[POSD] Deactivating Quick Prayers.");
            if (ActionBar.useAbility("Quick-prayers " + quickPrayersNumber)) {
                ScriptConsole.println("[POSD] Quick Prayers deactivated.");
                this.quickPrayersActive = false;
            } else {
                ScriptConsole.println("[POSD] Failed to deactivate Quick Prayers.");
            }
        }
    }

    private boolean isQuickPrayersActive() {
        int[] varbitIds = new int[]{16761, 16762, 16763, 16786, 16764, 16765, 16787, 16788, 16765, 16766, 16767, 16768, 16769, 16770, 16771, 16772, 16781, 16773, 16782, 16774, 16775, 16776, 16777, 16778, 16779, 16780, 16784, 16783, 29065, 29066, 29067, 29068, 29069, 49330, 29071, 34866, 34867, 34868, 53275, 53276, 53277, 53278, 53279, 53280, 53281, 16739, 16740, 16741, 16742, 16743, 16744, 16745, 16746, 16747, 16748, 16749, 16750, 16751, 16752, 16753, 16754, 16755, 16756, 16757, 16758, 16759, 16760, 53271, 53272, 53273, 53274};
        for (int varbitId : varbitIds) {
            if (VarManager.getVarbitValue(varbitId) == 1) {
                return true;
            }
        }
        return false;
    }

    public long useAggression(LocalPlayer player) {
        if (useAggroPots && player != null && player.inCombat() && player.getAnimationId() != 18000 && VarManager.getVarbitValue(33448) == 0) {
            ResultSet<Item> results = InventoryItemQuery.newQuery(93).name("Aggression", String::contains).option("Drink").results();
            if (results.isEmpty()) {
                ScriptConsole.println("[POSD] No aggression flasks found in the inventory.");
                return 1L;
            } else {
                Item aggressionFlask = results.first();
                if (aggressionFlask != null) {
                    boolean success = Backpack.interact(aggressionFlask.getName(), "Drink");
                    if (success) {
                        ScriptConsole.println("[POSD] Using aggression potion: " + aggressionFlask.getName());
                        long delay = random.nextLong(1500L, 3000L);
                        Execution.delay(delay);
                        return delay;
                    } else {
                        ScriptConsole.println("[POSD] Failed to use aggression potion: " + aggressionFlask.getName());
                        return 0L;
                    }
                } else {
                    return 0L;
                }
            }
        } else {
            return random.nextLong(300L, 750L);
        }
    }

    public long usePrayerOrRestorePots(LocalPlayer player) {
        if (usePrayerPots && player != null && player.inCombat() && player.getAnimationId() != 18000 && player.getPrayerPoints() <= prayerPointsThreshold) {
            ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();
            Item prayerOrRestorePot = items.stream()
                    .filter(item -> item.getName() != null && 
                            (item.getName().toLowerCase().contains("prayer") || 
                             item.getName().toLowerCase().contains("restore")))
                    .findFirst().orElse(null);
                     
            if (prayerOrRestorePot == null) {
                ScriptConsole.println("[POSD] No prayer or restore potions found in the backpack.");
                return 1L;
            } else {
                ScriptConsole.println("[POSD] Drinking " + prayerOrRestorePot.getName());
                boolean success = Backpack.interact(prayerOrRestorePot.getName(), "Drink");
                if (success) {
                    ScriptConsole.println("[POSD] Successfully drank " + prayerOrRestorePot.getName());
                    long delay = random.nextLong(1500L, 3000L);
                    Execution.delay(delay);
                    return delay;
                } else {
                    ScriptConsole.println("[POSD] Failed to interact with " + prayerOrRestorePot.getName());
                    return 0L;
                }
            }
        } else {
            return random.nextLong(300L, 750L);
        }
    }

    private void manageScrimshaws(LocalPlayer player) {
        Pattern scrimshawPattern = Pattern.compile("scrimshaw", Pattern.CASE_INSENSITIVE);
        Item scrimshaw = InventoryItemQuery.newQuery(94).name(scrimshawPattern).results().first();
        if (scrimshaw != null) {
            if (player.inCombat()) {
                Execution.delay(this.activateScrimshaws());
            } else {
                Execution.delay(this.deactivateScrimshaws());
            }
        } else {
            ScriptConsole.println("[POSD] Pocket slot does not contain a scrimshaw.");
        }
    }

    private long activateScrimshaws() {
        Pattern scrimshawPattern = Pattern.compile("scrimshaw", Pattern.CASE_INSENSITIVE);
        Item scrimshaw = InventoryItemQuery.newQuery(94).name(scrimshawPattern).results().first();
        if (scrimshaw != null && VarManager.getInvVarbit(scrimshaw.getInventoryType().getId(), scrimshaw.getSlot(), 17232) == 0) {
            ScriptConsole.println("[POSD] Activating Scrimshaws.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
            return RandomGenerator.nextInt(1500, 3000);
        } else {
            return 0L;
        }
    }

    private long deactivateScrimshaws() {
        Pattern scrimshawPattern = Pattern.compile("scrimshaw", Pattern.CASE_INSENSITIVE);
        Item scrimshaw = InventoryItemQuery.newQuery(94).name(scrimshawPattern).results().first();
        if (scrimshaw != null && VarManager.getInvVarbit(scrimshaw.getInventoryType().getId(), scrimshaw.getSlot(), 17232) == 1) {
            ScriptConsole.println("[POSD] Deactivating Scrimshaws.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
            return RandomGenerator.nextInt(1500, 3000);
        } else {
            return 0L;
        }
    }

    public long drinkOverloads(LocalPlayer player) {
        if (!useOverloads) {
            return random.nextLong(300L, 750L);
        } else if (player != null && player.inCombat() && VarManager.getVarbitValue(48834) == 0 && player.getAnimationId() != 18000) {
            Pattern overloadPattern = Pattern.compile("overload", Pattern.CASE_INSENSITIVE);
            Item overloadPot = InventoryItemQuery.newQuery().results().stream()
                    .filter(item -> item.getName() != null && overloadPattern.matcher(item.getName()).find())
                    .findFirst().orElse(null);
                    
            if (overloadPot == null) {
                ScriptConsole.println("[POSD] No overload potion found in the Backpack.");
                return 1L;
            } else {
                boolean success = Backpack.interact(overloadPot.getName(), "Drink");
                if (success) {
                    ScriptConsole.println("[POSD] Successfully drank " + overloadPot.getName());
                    long delay = random.nextLong(1500L, 3000L);
                    Execution.delay(delay);
                    return delay;
                } else {
                    ScriptConsole.println("[POSD] Failed to interact with overload potion.");
                    return 0L;
                }
            }
        } else {
            return 0L;
        }
    }

    private boolean shouldActivateQuickPrayers(LocalPlayer player) {
        return player.inCombat();
    }

    public long attackTarget(LocalPlayer player) {
        if (player == null) {
            return this.logAndDelay("[POSD] Local player not found.", 1500, 3000);
        } else {
            if (useQuickPrayers) {
                this.manageQuickPrayers(player);
            }
            if (useScrimshaws) {
                this.manageScrimshaws(player);
            }
            if (isHealthLow(player)) {
                eatFood(player);
                return this.logAndDelay("[POSD] Health is low.", 1000, 5000);
            } else if (player.hasTarget()) {
                return random.nextLong(100L, 300L);
            }
        }
        return 0L;
    }

    public void eatFood(LocalPlayer player) {
        boolean isPlayerEating = player.getAnimationId() == 18001;
        double healthPercentage = calculateHealthPercentage(player);
        boolean isHealthAboveThreshold = healthPercentage > healthPointsThreshold;
        if (!isPlayerEating && !isHealthAboveThreshold) {
            Execution.delay(healHealth(player));
        }
    }

    public long healHealth(LocalPlayer player) {
        ResultSet<Item> foodItems = InventoryItemQuery.newQuery(93).option("Eat").results();
        Item food = foodItems.isEmpty() ? null : foodItems.first();
        if (food == null) {
            if (bankForFood) {
                this.currentStep = 7;
            }
            ScriptConsole.println("[POSD] No food found and banking for food is disabled.");
            return 0L;
        } else {
            boolean eatSuccess = Backpack.interact(food.getName(), "Eat");
            if (eatSuccess) {
                ScriptConsole.println("[POSD] Successfully ate " + food.getName());
                Execution.delay(RandomGenerator.nextInt(250, 450));
            } else {
                ScriptConsole.println("[POSD] Failed to eat.");
            }
            return 0L;
        }
    }

    private long logAndDelay(String message, int minDelay, int maxDelay) {
        ScriptConsole.println(message);
        long delay = random.nextLong(minDelay, maxDelay);
        Execution.delay(delay);
        return delay;
    }

    public double calculateHealthPercentage(LocalPlayer player) {
        double currentHealth = player.getCurrentHealth();
        double maximumHealth = player.getMaximumHealth();
        if (maximumHealth == 0.0D) {
            throw new ArithmeticException("Maximum health cannot be zero.");
        } else {
            return currentHealth / maximumHealth * 100.0D;
        }
    }

    public boolean isHealthLow(LocalPlayer player) {
        double healthPercentage = calculateHealthPercentage(player);
        return healthPercentage < healthPointsThreshold;
    }

    // Getters and setters for configuration
    public boolean isInteractWithLootAll() {
        return interactWithLootAll;
    }

    public void setInteractWithLootAll(boolean interactWithLootAll) {
        this.interactWithLootAll = interactWithLootAll;
    }

    public boolean isUseLoot() {
        return useLoot;
    }

    public void setUseLoot(boolean useLoot) {
        this.useLoot = useLoot;
    }

    public List<String> getTargetItemNames() {
        return targetItemNames;
    }

    public void addTargetItem(String itemName) {
        if (!targetItemNames.contains(itemName)) {
            targetItemNames.add(itemName);
        }
    }

    public void removeTargetItem(String itemName) {
        targetItemNames.remove(itemName);
    }

    public void clearTargetItems() {
        targetItemNames.clear();
    }

    public int getQuickPrayersNumber() {
        return quickPrayersNumber;
    }

    public void setQuickPrayersNumber(int quickPrayersNumber) {
        this.quickPrayersNumber = quickPrayersNumber;
    }

    public boolean isUseOverloads() {
        return useOverloads;
    }

    public void setUseOverloads(boolean useOverloads) {
        this.useOverloads = useOverloads;
    }

    public boolean isUsePrayerPots() {
        return usePrayerPots;
    }

    public void setUsePrayerPots(boolean usePrayerPots) {
        this.usePrayerPots = usePrayerPots;
    }

    public boolean isUseAggroPots() {
        return useAggroPots;
    }

    public void setUseAggroPots(boolean useAggroPots) {
        this.useAggroPots = useAggroPots;
    }

    public int getPrayerPointsThreshold() {
        return prayerPointsThreshold;
    }

    public void setPrayerPointsThreshold(int prayerPointsThreshold) {
        this.prayerPointsThreshold = prayerPointsThreshold;
    }

    public int getHealthPointsThreshold() {
        return healthPointsThreshold;
    }

    public void setHealthPointsThreshold(int healthPointsThreshold) {
        this.healthPointsThreshold = healthPointsThreshold;
    }

    public boolean isUseWeaponPoison() {
        return useWeaponPoison;
    }

    public void setUseWeaponPoison(boolean useWeaponPoison) {
        this.useWeaponPoison = useWeaponPoison;
    }

    public boolean isUseQuickPrayers() {
        return useQuickPrayers;
    }

    public void setUseQuickPrayers(boolean useQuickPrayers) {
        this.useQuickPrayers = useQuickPrayers;
    }

    public String getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(String selectedItem) {
        this.selectedItem = selectedItem;
    }

    public boolean isUseScrimshaws() {
        return useScrimshaws;
    }

    public void setUseScrimshaws(boolean useScrimshaws) {
        this.useScrimshaws = useScrimshaws;
    }

    public boolean isBankForFood() {
        return bankForFood;
    }

    public void setBankForFood(boolean bankForFood) {
        this.bankForFood = bankForFood;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }
}