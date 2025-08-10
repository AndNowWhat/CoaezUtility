package net.botwithus;

import java.util.Random;

import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.model.Alchemy;
import net.botwithus.model.Disassembly;
import net.botwithus.model.POSD;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.tasks.AlchemyTask;
import net.botwithus.tasks.BeachEventTask;
import net.botwithus.tasks.BeerCraftingTask;
import net.botwithus.tasks.ClayUrnTask;
import net.botwithus.tasks.CreateSqirkJuiceTask;
import net.botwithus.tasks.DeployDummyTask;
import net.botwithus.tasks.DisassemblyTask;
import net.botwithus.tasks.DrinkPerfectPlusJujuTask;
import net.botwithus.tasks.EnchantingTask;
import net.botwithus.tasks.FungalBowstrings;
import net.botwithus.tasks.GemCraftingTask;
import net.botwithus.tasks.InventionTask;
import net.botwithus.tasks.LimestoneBrickTask;
import net.botwithus.tasks.LimestoneTask;
import net.botwithus.tasks.MapNavigatorTask;
import net.botwithus.tasks.NPCLoggerTask;
import net.botwithus.tasks.POSDTask;
import net.botwithus.tasks.PenguinTrackingTask;
import net.botwithus.tasks.PortableTask;
import net.botwithus.tasks.PowderOfBurialsTask;
import net.botwithus.tasks.QuestHelper;
import net.botwithus.tasks.SandyCluesTask;
import net.botwithus.tasks.ScreenMeshTask;
import net.botwithus.tasks.SheepShearingTask;
import net.botwithus.tasks.SiftSoilTask;
import net.botwithus.tasks.SoftClayTask;
import net.botwithus.tasks.SummerPinata;
import net.botwithus.tasks.TeleportToCamelot;
import net.botwithus.tasks.TurnInSqirkjuiceTask;
import net.botwithus.tasks.sorceressgarden.SorceressGardenTask;

public class CoaezUtility extends LoopingScript {
    private BotState botState = BotState.SORCERESS_GARDEN;
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
    private final PenguinTrackingTask penguinTrackingTask;
    private final BeachEventTask beachEventTask;
    private final SoftClayTask softClayTask;
    private final LimestoneTask limestoneTask;
    private final LimestoneBrickTask limestoneBrickTask;
    private final MapNavigatorTask mapNavigatorTask;
    private final DeployDummyTask deployDummyTask;
    private final SandyCluesTask sandyCluesTask;
    private final SummerPinata summerPinata;
    private final TeleportToCamelot southFeldipeHillsTeleportTask;
    private final SorceressGardenTask sorceressGardenTask;
    private final BeerCraftingTask beerCraftingTask;
    private final CreateSqirkJuiceTask winterSqirkjuiceTask;
    private final TurnInSqirkjuiceTask turnInSqirkjuiceTask;
    private final ClayUrnTask clayUrnTask;
    // GUI reference
    private CoaezUtilityGUI gui;
    private final NPCLoggerTask npcLoggerTask;

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
        PENGUIN_TRACKING,
        BEACH_EVENT,
        SOFTCLAY,
        LIMESTONE,
        LIMESTONE_BRICK,
        QUESTS,
        MAP_NAVIGATOR,
        DEPLOY_DUMMY,
        SANDY_CLUES,
        SUMMER_PINATA,
        SOUTH_FELDIPE_HILLS_TELEPORT,
        SORCERESS_GARDEN,
        NPC_LOGGER,
        STOPPED,
        BEER_CRAFTING,
        WINTER_SQIRKJUICE,
        TURN_IN_SQIRKJUICE,
        CLAY_URN
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
        this.penguinTrackingTask = new PenguinTrackingTask(this);
        this.beachEventTask = new BeachEventTask(this);
        this.softClayTask = new SoftClayTask(this);
        this.limestoneTask = new LimestoneTask(this);
        this.limestoneBrickTask = new LimestoneBrickTask(this);
        this.mapNavigatorTask = new MapNavigatorTask(this);
        this.deployDummyTask = new DeployDummyTask(this);
        this.sandyCluesTask = new SandyCluesTask(this);
        this.summerPinata = new SummerPinata(this);
        this.southFeldipeHillsTeleportTask = new TeleportToCamelot(this);
        this.sorceressGardenTask = new SorceressGardenTask(this);
        this.beerCraftingTask = new BeerCraftingTask(this);
        this.winterSqirkjuiceTask = new CreateSqirkJuiceTask(this);
        this.turnInSqirkjuiceTask = new TurnInSqirkjuiceTask(this);
        this.clayUrnTask = new ClayUrnTask(this);
        this.npcLoggerTask = new NPCLoggerTask(this);
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
                case PENGUIN_TRACKING -> {
                    ScriptConsole.println("Executing penguin tracking task");
                    penguinTrackingTask.execute();
                }
                case BEACH_EVENT -> {
                    ScriptConsole.println("Executing beach event task");
                    beachEventTask.execute();
                }
                case SOFTCLAY -> {
                    ScriptConsole.println("Executing soft clay task");
                    softClayTask.execute();
                }
                case LIMESTONE -> {
                    ScriptConsole.println("Executing limestone task");
                    limestoneTask.execute();
                }
                case LIMESTONE_BRICK -> {
                    ScriptConsole.println("Executing limestone brick task");
                    limestoneBrickTask.execute();
                }
                case QUESTS -> {
                    ScriptConsole.println("Executing quests helper");
                    questHelper.execute();
                }
                case MAP_NAVIGATOR -> {
                    ScriptConsole.println("Executing map navigator task");
                    mapNavigatorTask.execute();
                }
                case DEPLOY_DUMMY -> {
                    ScriptConsole.println("Executing deploy dummy task");
                    deployDummyTask.execute();
                }
                case SANDY_CLUES -> {
                    ScriptConsole.println("Executing sandy clues task");
                    sandyCluesTask.execute();
                }
                case SUMMER_PINATA -> {
                    ScriptConsole.println("Executing attack deploy task");
                    summerPinata.execute();
                }
                case SOUTH_FELDIPE_HILLS_TELEPORT -> {
                    ScriptConsole.println("Executing south Feldip Hills teleport task");
                    southFeldipeHillsTeleportTask.execute();
                }
                case SORCERESS_GARDEN -> {
                    ScriptConsole.println("Executing Sorceress Garden task");
                    sorceressGardenTask.execute();
                }
                case NPC_LOGGER -> {
                    ScriptConsole.println("Executing NPC Logger task");
                    npcLoggerTask.execute();
                }
                case BEER_CRAFTING -> {
                    ScriptConsole.println("Executing beer crafting task");
                    beerCraftingTask.execute();
                }
                case WINTER_SQIRKJUICE -> {
                    ScriptConsole.println("Executing Winter Sq'irkjuice task");
                    winterSqirkjuiceTask.execute();
                }
                case TURN_IN_SQIRKJUICE -> {
                    ScriptConsole.println("Executing Turn In Sq'irkjuice task");
                    turnInSqirkjuiceTask.execute();
                }
                case CLAY_URN -> {
                    ScriptConsole.println("Executing Clay Urn task");
                    clayUrnTask.execute();
                }
                case STOPPED -> stopScript();
                default -> ScriptConsole.println("Unknown bot state: " + botState);
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
        
        if (botState == BotState.BEACH_EVENT) {
            beachEventTask.handleBattleshipMessage(message);
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

    public PenguinTrackingTask getPenguinTrackingTask() {
        return penguinTrackingTask;
    }

    public BeachEventTask getBeachEventTask() {
        return beachEventTask;
    }

    public SoftClayTask getSoftClayTask() {
        return softClayTask;
    }

    public LimestoneTask getLimestoneTask() {
        return limestoneTask;
    }

    public LimestoneBrickTask getLimestoneBrickTask() {
        return limestoneBrickTask;
    }

    public MapNavigatorTask getMapNavigatorTask() {
        return mapNavigatorTask;
    }

    public DeployDummyTask getDeployDummyTask() {
        return deployDummyTask;
    }

    public SummerPinata getSummerPinata() {
        return summerPinata;
    }

    public TeleportToCamelot getSouthFeldipeHillsTeleportTask() {
        return southFeldipeHillsTeleportTask;
    }

    public SorceressGardenTask getSorceressGardenTask() {
        return sorceressGardenTask;
    }
    
    public NPCLoggerTask getNPCLoggerTask() {
        return npcLoggerTask;
    }
    
    public BeerCraftingTask getBeerCraftingTask() {
        return beerCraftingTask;
    }

    public CreateSqirkJuiceTask getWinterSqirkjuiceTask() {
        return winterSqirkjuiceTask;
    }

    public TurnInSqirkjuiceTask getTurnInSqirkjuiceTask() {
        return turnInSqirkjuiceTask;
    }

    public ClayUrnTask getClayUrnTask() {
        return clayUrnTask;
    }

    /* public SmithingTask getSmithingTask() {
        return smithingTask;
    } */
}
