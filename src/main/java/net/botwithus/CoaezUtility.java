package net.botwithus;

import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.Item;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.movement.TraverseEvent;

public class CoaezUtility extends LoopingScript {
    private BotState botState = BotState.IDLE;
    private Random random = new Random();
    private static final int BURIAL_POWDER_SPRITE_ID = 52805;
    private boolean waitingForPreset = false;
    private boolean presetLoaded = false;
    private volatile boolean noBonesLeft = false;
    private ScriptConfig config;
    private Alchemy alchemy;
    private Disassembly disassembly;
    private static final List<String> SOIL_ITEMS = Arrays.asList(
            "Senntisten soil",
            "Ancient gravel",
            "Fiery brimstone",
            "Saltwater mud",
            "Aerated sediment",
            "Earthen clay",
            "Volcanic ash"
    );

    String[] itemNames = {
            "Bones", "Wolf bones", "Burnt bones", "Monkey bones", "Bat bones",
            "Big bones", "Jogre bones", "Zogre bones", "Shaikahan bones",
            "Baby dragon bones", "Wyvern bones", "Dragon bones", "Fayrg bones",
            "Raurg bones", "Dagannoth bones", "Airut bones", "Ourg bones",
            "Hardened dragon bones", "Dragonkin bones", "Dinosaur bones",
            "Frost dragon bones", "Reinforced dragon bones"
    };

    enum BotState {
        IDLE,
        POWDER_OF_BURIALS,
        SIFT_SOIL,
        SCREEN_MESH,
        ALCHEMY,
        DISASSEMBLY,
        GEM_CRAFTING,
        STOPPED
    }
    

    public CoaezUtility(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        subscribe(ChatMessageEvent.class, this::onChatMessage);
        this.alchemy = new Alchemy();
        this.disassembly = new Disassembly(this);   
        this.config = scriptConfig;
        this.sgc = new CoaezUtilityGUI(this.getConsole(), this);
    }

    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public ScriptConfig getConfig() {
        return config;
    }

        @Override
        public void onLoop () {
            LocalPlayer player = Client.getLocalPlayer();
            if (player == null) return;
            int currentRegion = Client.getLocalPlayer().getServerCoordinate().getRegionId();

            switch (botState) {
                case IDLE:
                    break;
                case ALCHEMY:
                    handleAlchemy();
                    break;
                case DISASSEMBLY:
                    handleDisassembly();
                    break;
                case POWDER_OF_BURIALS:
                    handlePowderOfBurials();
                    break;
                case SIFT_SOIL:
                    handleSiftSoil();
                    break;
                case SCREEN_MESH:
                    handleScreenMesh();
                    break;
                case GEM_CRAFTING:
                    handleGemCrafting();
                    break;
                case STOPPED:
                    stopScript();
                    break;
            }
            Execution.delay(random.nextInt(600, 1200));
        }

        private boolean logging() {
            int varbitValue = VarManager.getVarbitValue(3829);
            println("Varbit value: " + varbitValue);
            return varbitValue > 0;
        }

        private void handleAlchemy() {
            if (alchemy.hasItemsToAlchemize()) {
                alchemy.castAlchemy();
                Execution.delay(random.nextLong(300, 600));
            } else {
                Bank.loadLastPreset();
            }
         }
        
         private void handleDisassembly() {
            println("[handleDisassembly] Starting with botState: " + botState);
    
            if(Interfaces.isOpen(1251)) {
                println("[handleDisassembly] Interface 1251 open, waiting...");
                Execution.delayUntil(100000, () -> !Interfaces.isOpen(1251));
                return;
            }
    
            println("[handleDisassembly] Items in disassembly list: " + disassembly.getDisassemblyItems());
            boolean hasItems = disassembly.hasItemsToDisassemble();
            println("[handleDisassembly] Has items to disassemble: " + hasItems);
    
            List<Item> backpackItems = Backpack.getItems();
            println("[handleDisassembly] Current backpack items: " + backpackItems.stream()
                    .map(Item::getName)
                    .collect(Collectors.joining(", ")));
    
            if (hasItems) {
                println("[handleDisassembly] Casting disassembly");
                disassembly.castDisassembly();
                Execution.delay(random.nextLong(600, 1200));
            } else {
                println("[handleDisassembly] No items found, loading preset");
                Bank.loadLastPreset();
                Execution.delay(random.nextLong(1200, 2000));
            }
        }
        private void handlePowderOfBurials () {
            if (!isPowderOfBurialsActive()) {
                activatePowderOfBurials();
                return;
            }
    
            if (hasBonesToBury()) {
                buryBones();
            } else {
                Bank.loadLastPreset();
            }
        }    

        private boolean isPowderOfBurialsActive() {
            Execution.delay(random.nextLong(100, 200));
            Component powderOfBurials = ComponentQuery.newQuery(284)
                .spriteId(BURIAL_POWDER_SPRITE_ID)
                .results()
                .first();
            return powderOfBurials != null;
        }
        

    private void activatePowderOfBurials() {
        if (isPowderOfBurialsActive()) return;
        Execution.delay(random.nextLong(600, 800));

        if (inventoryInteract("Scatter", "Powder of burials")) {
            Execution.delayUntil(5000, this::isPowderOfBurialsActive);
        }
    }

    private boolean inventoryInteract(String option, String... items) {
        Pattern pattern = Pattern.compile(String.join("|", items), Pattern.CASE_INSENSITIVE);
        Item item = InventoryItemQuery.newQuery().name(pattern).results().first();

        if (item != null) {
            String itemName = item.getName();
            Component itemComponent = ComponentQuery.newQuery(1473).componentIndex(5).itemName(itemName).results().first();
            if (itemComponent != null) {
                return itemComponent.interact(option);
            }
        }
        return false;
    }

    private boolean hasBonesToBury() {
        for (String itemName : itemNames) {
            Component itemComponent = ComponentQuery.newQuery(1473).componentIndex(5).itemName(itemName).results().first();
            if (itemComponent != null) {
                Execution.delay(random.nextInt(100, 300));
                return true;
            }
        }
        return false;
    }

    private void buryBones() {
        noBonesLeft = false; 
        while (!noBonesLeft && isActive()) {
            boolean success = false;
            for (String itemName : itemNames) {
                success = ActionBar.useItem(itemName, 1);
                if (success) {
                    break;
                }
            }
            if (!success) {
                break; 
            }
            Execution.delay(random.nextInt(250, 300));
        }
        if (!isActive()) {
            println("Script stopped while burying bones.");
            return;
        }
        if (noBonesLeft) {
            println("No bones left to bury.");
            loadBankPreset();
        } else {
            println("Finished burying all available bones.");
        }
    }
    

    private void handleSiftSoil() {
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(1371)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(random.nextLong(1000, 2000));
            return;
        }

        if (BackpackContainsSoil()) {
            selectSoilSpell();
        } else {
            Bank.loadLastPreset();
        }
    }

    private void selectSoilSpell() {
        ActionBar.useAbility("Sift Soil");
        Execution.delayUntil(5000, () -> Interfaces.isOpen(1371));
    }

    private boolean BackpackContainsSoil() {
        ResultSet<Item> backpackItems = InventoryItemQuery.newQuery(93).results();
        for (Item item : backpackItems) {
            if (SOIL_ITEMS.contains(item.getName())) {
                return true;
            }
        }
        return false;
    }

    private void loadBankPreset() {
        SceneObject bankObj = SceneObjectQuery.newQuery().name("Bank chest", "Banker", "Bank booth").results().nearest();

        if (bankObj != null) {
            presetLoaded = false;
            waitingForPreset = true;
            Bank.loadLastPreset();

            Execution.delayUntil(random.nextInt(5000) + 5000, () -> presetLoaded);
            waitingForPreset = false;

            if (botState == BotState.POWDER_OF_BURIALS) {
                Execution.delayUntil(5000, this::hasBonesToBury);
            } else if (botState == BotState.SIFT_SOIL) {
                Execution.delayUntil(5000, this::BackpackContainsSoil);
            }
        }
    }

    private void stopScript() {
        setActive(false);
    }

    private void onChatMessage(ChatMessageEvent event) {
        String message = event.getMessage();
        if (message.contains("You don't have any left!")) {
            noBonesLeft = true;
        }
        if (waitingForPreset && message.contains("Your preset is being withdrawn")) {
            presetLoaded = true;
        }
    }

    public Alchemy getAlchemy() {
        return alchemy;
    }
    
    public Disassembly getDisassembly() {
        return disassembly;
    }
    
    private void handleScreenMesh() {
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (BackpackContainsSoil()) {
            SceneObject mesh = SceneObjectQuery.newQuery().name("Mesh").option("Screen").results().nearest();
            
            if (mesh != null && mesh.interact("Screen")) {
                Execution.delayUntil(15000, () -> Interfaces.isOpen(1370));
                
                Component screenOption = ComponentQuery.newQuery(1370)
                    .componentIndex(30)  
                    .results()
                    .first();
                
                if (screenOption != null && screenOption.interact(1)) {
                    Execution.delay(random.nextLong(1000, 2000));
                } else {
                    println("Could not find screening interface option");
                }
            } else {
                println("Could not find mesh object");
            }
        } else {
            waitingForPreset = true;
            presetLoaded = false;
            Bank.loadLastPreset();
            Execution.delayUntil(random.nextInt(5000) + 5000, () -> presetLoaded);
        }
    }

    private boolean moveTo(Coordinate location) {
        if (Client.getLocalPlayer() == null) {
            println("Movement failed: Player is null");
            return false;
        }
        
        println("Current position: " + Client.getLocalPlayer().getCoordinate());
        println("Distance to target: " + Client.getLocalPlayer().getCoordinate().distanceTo(location));
        
        TraverseEvent.State state = Movement.traverse(NavPath.resolve(location));
        println("Movement state: " + state);
        
        boolean result = state == TraverseEvent.State.FINISHED || 
                         state == TraverseEvent.State.CONTINUE || 
                         state == TraverseEvent.State.IDLE;
        
        println("Movement function returning: " + result);
        return result;
    }

    private void handleGemCrafting() {
        if (Interfaces.isOpen(1251)) {
            Execution.delayUntil(14000, () -> !Interfaces.isOpen(1251));
            return;
        }

        if (Interfaces.isOpen(1371)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            Execution.delay(random.nextLong(1000, 2000));
            return;
        }

        if (hasUncutGems()) {
            craftGems();
        } else {
            Bank.loadLastPreset();
        }
    }

    private boolean hasUncutGems() {
        ResultSet<Item> backpackItems = InventoryItemQuery.newQuery(93).results();
        for (Item item : backpackItems) {
            if (item.getName().contains("Uncut")) {
                return true;
            }
        }
        return false;
    }

    private void craftGems() {
        Item gem = InventoryItemQuery.newQuery(93).option("Craft").results().first();
        if (gem != null) {
            if (inventoryInteract("Craft", gem.getName())) {
                Execution.delayUntil(5000, () -> Interfaces.isOpen(1371));
            }
        }
    }
}