package net.botwithus;

import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.Client;
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
import net.botwithus.api.game.hud.inventories.Bank;

import java.util.Random;
import java.util.regex.Pattern;

public class CoaezPowderOfBurials extends LoopingScript {

    private BotState botState = BotState.IDLE;
    private Random random = new Random();
    private static final int BURIAL_POWDER_SPRITE_ID = 52805;
    private boolean waitingForPreset = false;
    private boolean presetLoaded = false;
    private volatile boolean noBonesLeft = false;

    String[] itemNames = {
        "Bones",
        "Wolf bones",
        "Burnt bones",
        "Monkey bones",
        "Bat bones",
        "Big bones",
        "Jogre bones",
        "Zogre bones",
        "Shaikahan bones",
        "Baby dragon bones",
        "Wyvern bones",
        "Dragon bones",
        "Fayrg bones",
        "Raurg bones",
        "Dagannoth bones",
        "Airut bones",
        "Ourg bones",
        "Hardened dragon bones",
        "Dragonkin bones",
        "Dinosaur bones",
        "Frost dragon bones",
        "Reinforced dragon bones"
    };

    enum BotState {
        IDLE,
        BURYING_BONES,
        LOAD_PRESET,
        STOPPED
    }

    public CoaezPowderOfBurials(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        subscribe(ChatMessageEvent.class, this::onChatMessage);
    }

    @Override
    public void onLoop() {
        if (!isActive()) {
            return;
        }

        this.loopDelay = 1000;
        LocalPlayer player = Client.getLocalPlayer();

        switch (botState) {
            case IDLE:
                println("Bot state: IDLE");
                if (!isPowderOfBurialsActive()) {
                    println("Activating Powder of Burials.");
                    activatePowderOfBurials();
                } else if (hasBonesToBury()) {
                    println("Burying bones.");
                    botState = BotState.BURYING_BONES;
                } else {
                    println("Loading bank preset.");
                    botState = BotState.LOAD_PRESET;
                }
                break;
            case BURYING_BONES:
                println("Bot state: BURYING_BONES");
                buryBones();
                botState = BotState.IDLE;
                break;
            case LOAD_PRESET:
                println("Bot state: LOAD_PRESET");
                loadBankPreset();
                botState = BotState.IDLE;
                break;
            case STOPPED:
                println("Bot state: STOPPED");
                stopScript();
                break;
        }
    }

    private boolean isPowderOfBurialsActive() {
        Component powderOfBurials = ComponentQuery.newQuery(284).spriteId(BURIAL_POWDER_SPRITE_ID).results().first();
        return powderOfBurials != null;
    }

    private void activatePowderOfBurials() {
        if (isPowderOfBurialsActive()) {
            println("Powder of Burials already active.");
            return;
        }
        if (inventoryInteract("Scatter", "Powder of burials")) {
            try {
                if (Execution.delayUntil(random.nextInt(5000), () -> isPowderOfBurialsActive())) {
                    println("Powder of Burials activated.");
                } else {
                    println("Failed to activate Powder of Burials.");
                }
            } catch (Exception e) {
                println("Error during delayUntil: " + e.getMessage());
            }
        } else {
            println("Powder of Burials not found in inventory.");
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
            Execution.delay(100);
        }
        if (!isActive()) {
            println("Script stopped while burying bones.");
            return;
        }
        if (noBonesLeft) {
            getConsole().println("No bones left to bury.");
            loadBankPreset();
        } else {
            getConsole().println("Finished burying all available bones.");
        }
    }
    private void loadBankPreset() {
        LocalPlayer player = Client.getLocalPlayer();
        SceneObject bankObj = SceneObjectQuery.newQuery().name("Bank chest", "Banker", "Bank booth").results().nearest();

        if (bankObj != null) {
            presetLoaded = false;
            waitingForPreset = true;
            Bank.loadLastPreset();

            try {
                boolean presetSuccess = Execution.delayUntil(random.nextInt(5000) + 5000, () -> presetLoaded);
                waitingForPreset = false;

                if (presetSuccess) {
                    println("Preset successfully loaded.");
                    
                    // Retry mechanism
                    int retryCount = 0;
                    int maxRetries = 5;
                    int delayBetweenRetries = 1000; // milliseconds
                    boolean bonesFound = false;

                    while (retryCount < maxRetries && !bonesFound) {
                        bonesFound = hasBonesToBury();
                        if (!bonesFound) {
                            retryCount++;
                            Execution.delay(delayBetweenRetries);
                        }
                    }

                    if (bonesFound) {
                        println("Bones found in inventory after loading preset.");
                    } else {
                        println("No bones found in inventory after loading preset. Stopping script.");
                        stopScript();
                    }
                } else {
                    println("Failed to load the preset.");
                    botState = BotState.STOPPED;
                }
            } catch (Exception e) {
                println("Error during delayUntil: " + e.getMessage());
                botState = BotState.STOPPED;
            }
        } else {
            println("No bank chest found.");
            botState = BotState.STOPPED;
        }
    }


    private void stopScript() {
        // Stop the script safely.
        println("Stopping script.");
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
}
