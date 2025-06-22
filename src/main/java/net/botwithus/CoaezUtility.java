package net.botwithus;

import java.util.List;

import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.tasks.*;
import net.botwithus.model.Alchemy;
import net.botwithus.model.Disassembly;
import net.botwithus.model.POSD;

import java.util.Random;

import net.botwithus.rs3.script.ScriptConsole;

public class CoaezUtility extends LoopingScript {
    private BotState botState = BotState.POWDER_OF_BURIALS;
    private Random random = new Random();
    private ScriptConfig config;
    private volatile boolean noBonesLeft = false;
    private boolean waitingForPreset = false;
    private boolean presetLoaded = false;
    
    // Model instances
    private final Alchemy alchemy;
    private final Disassembly disassembly;
    private final POSD posd;
    private final QuestHelper questHelper;
    
    // Task instances
    private final PowderOfBurialsTask powderOfBurialsTask;
    private final SiftSoilTask siftSoilTask;
    private final ScreenMeshTask screenMeshTask;
    private final AlchemyTask alchemyTask;
    private final DisassemblyTask disassemblyTask;
    private final GemCraftingTask gemCraftingTask;
    private final POSDTask posdTask;
    private final InventionTask inventionTask;
    private final EnchantingTask enchantingTask;
    private final DrinkPerfectPlusJujuTask drinkPerfectPlusJujuTask;
    private final FungalBowstrings fungalBowstringsTask;
    private final PortableTask portableTask;
    //private final SmithingTask smithingTask;
    private final SheepShearingTask sheepShearingTask;
    // GUI reference
    private CoaezUtilityGUI gui;

    public enum BotState {
        IDLE,
        POWDER_OF_BURIALS,
        SIFT_SOIL,
        SCREEN_MESH,
        ALCHEMY,
        DISASSEMBLY,
        GEM_CRAFTING,
        POSD,
        INVENTION,
        ENCHANTING,
        FUNGAL_BOWSTRINGS,
        PORTABLES,
        //SMITHING,
        SHEEP_SHEARING,
        QUESTS,
        STOPPED
    }

    public CoaezUtility(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.config = scriptConfig;
        
        this.alchemy = new Alchemy(this);
        this.disassembly = new Disassembly(this);
        this.posd = new POSD(this);
        this.questHelper = new QuestHelper(this);
        
        // Initialize tasks
        this.powderOfBurialsTask = new PowderOfBurialsTask(this);
        this.siftSoilTask = new SiftSoilTask(this);
        this.screenMeshTask = new ScreenMeshTask(this);
        this.alchemyTask = new AlchemyTask(this);
        this.disassemblyTask = new DisassemblyTask(this);
        this.gemCraftingTask = new GemCraftingTask(this);
        this.posdTask = new POSDTask(this);
        this.inventionTask = new InventionTask(this);
        this.enchantingTask = new EnchantingTask(this);
        this.drinkPerfectPlusJujuTask = new DrinkPerfectPlusJujuTask(this);
        this.fungalBowstringsTask = new FungalBowstrings(this);
        this.portableTask = new PortableTask(this);
        //this.smithingTask = new SmithingTask(this);
        this.sheepShearingTask = new SheepShearingTask(this);
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

    public Random getRandom() {
        return random;
    }
    
    public boolean isWaitingForPreset() {
        return waitingForPreset;
    }
    
    public void setWaitingForPreset(boolean waiting) {
        this.waitingForPreset = waiting;
    }
    
    public boolean isPresetLoaded() {
        return presetLoaded;
    }
    
    public void setPresetLoaded(boolean loaded) {
        this.presetLoaded = loaded;
    }
    
    public boolean isNoBonesLeft() {
        return noBonesLeft;
    }
    
    public void setNoBonesLeft(boolean noBonesLeft) {
        this.noBonesLeft = noBonesLeft;
    }

    @Override
    public void onActivation() {
        super.onActivation();
        ScriptConsole.println("CoaezUtility script activated!");
        ScriptConsole.println("Current bot state: " + botState);
        sgc.setOpen(true);
        subscribe(ChatMessageEvent.class, this::onChatMessage);
    }

    @Override
    public void onDeactivation() {
        super.onDeactivation();
        ScriptConsole.println("CoaezUtility script deactivated!");
        sgc.setOpen(false);
        unsubscribeAll();
    }

    @Override
    public void onLoop() {
        try {
            LocalPlayer player = Client.getLocalPlayer();
            ScriptConsole.println("Current bot state: " + botState);

            if (player == null) {
                ScriptConsole.println("Player is null, waiting...");
                Execution.delay(1200);
                return;
            }

            drinkPerfectPlusJujuTask.execute();

            switch (botState) {
                case IDLE -> ScriptConsole.println("Bot is idle");
                case ALCHEMY -> {
                    ScriptConsole.println("Executing alchemy task");
                    alchemyTask.execute();
                }
                case DISASSEMBLY -> {
                    ScriptConsole.println("Executing disassembly task");
                    disassemblyTask.execute();
                }
                case POWDER_OF_BURIALS -> {
                    ScriptConsole.println("Executing powder of burials task");
                    powderOfBurialsTask.execute();
                }
                case SIFT_SOIL -> {
                    ScriptConsole.println("Executing sift soil task");
                    siftSoilTask.execute();
                }
                case SCREEN_MESH -> {
                    ScriptConsole.println("Executing screen mesh task");
                    screenMeshTask.execute();
                }
                case GEM_CRAFTING -> {
                    ScriptConsole.println("Executing gem crafting task");
                    gemCraftingTask.execute();
                }
                case POSD -> {
                    ScriptConsole.println("Executing POSD task");
                    posdTask.execute();
                }
                case INVENTION -> {
                    ScriptConsole.println("Executing invention task");
                    inventionTask.execute();
                }
                case ENCHANTING -> {
                    ScriptConsole.println("Executing enchanting task");
                    enchantingTask.execute();
                }
                case FUNGAL_BOWSTRINGS -> {
                    ScriptConsole.println("Executing fungal bowstrings task");
                    fungalBowstringsTask.execute();
                }
                case PORTABLES -> {
                    ScriptConsole.println("Executing portables task");
                    portableTask.execute();
                }
                case SHEEP_SHEARING -> {
                    ScriptConsole.println("Executing sheep shearing task");
                    sheepShearingTask.execute();
                }
                case QUESTS -> {
                    ScriptConsole.println("Quest helper active - viewing only");
                }
                case STOPPED -> {
                    ScriptConsole.println("Stopping script");
                    stopScript();
                }
            }
            Execution.delay(random.nextInt(400, 800));
        } catch (Exception e) {
            ScriptConsole.println("Error in main loop: " + e.getMessage());
            e.printStackTrace();
            Execution.delay(100);
        }
    }

    private void stopScript() {
        setActive(false);
    }

    private void onChatMessage(ChatMessageEvent event) {
        String message = event.getMessage();
        if (message.contains("You don't have any left!") || message.contains("You don't have any bones")) {
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
    
    public POSD getPOSD() {
        return posd;
    }
    
    public QuestHelper getQuestHelper() {
        return questHelper;
    }

    public AlchemyTask getAlchemyTask() {
        return alchemyTask;
    }
    
    public DisassemblyTask getDisassemblyTask() {
        return disassemblyTask;
    }
    
    public POSDTask getPOSDTask() {
        return posdTask;
    }

    public PortableTask getPortableTask() {
        return portableTask;
    }

    /* public SmithingTask getSmithingTask() {
        return smithingTask;
    } */
}