package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoaezUtilityGUI extends ScriptGraphicsContext {
    private final CoaezUtility coaezUtility;
    private CoaezUtility.BotState lastBotState;
    private String alchemyInput = "";
    private String disassemblyInput = "";
    private String posdLootInput = "";
    private Set<String> preloadedAlchemyItems = new HashSet<>();
    private Set<String> preloadedDisassemblyItems = new HashSet<>();

    // Window dimensions
    private final int LISTBOX_HEIGHT = 150;

    public CoaezUtilityGUI(ScriptConsole scriptConsole, CoaezUtility coaezUtility) {
        super(scriptConsole);
        this.coaezUtility = coaezUtility;
        lastBotState = coaezUtility.getBotState();
        loadConfig();
    }

    public boolean hasStateChanged() {
        if (lastBotState != coaezUtility.getBotState()) {
            lastBotState = coaezUtility.getBotState();
            return true;
        }
        return false;
    }

    @Override
    public void drawSettings() {
        if (ImGui.Begin("Coaez Utility", ImGuiWindowFlag.None.getValue())) {
            ImGui.Text("Current State: " + coaezUtility.getBotState());
            ImGui.Separator();
            
            if (ImGui.BeginTabBar("MainTabBar", 0)) {
                if (ImGui.BeginTabItem("Activities", 0)) {
                    renderActivitiesTab();
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Alchemy", 0)) {
                    renderAlchemyTab();
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Disassembly", 0)) {
                    renderDisassemblyTab();
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("POSD", 0)) {
                    renderPOSDTab();
                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
            
            ImGui.Separator();
            
            if (ImGui.Button("Stop All Activities")) {
                coaezUtility.setBotState(CoaezUtility.BotState.IDLE);
            }
            
            if (hasStateChanged()) {
                saveConfig();
            }
             
            ImGui.End();
        }
    }
    
    private void renderActivitiesTab() {
        ImGui.Text("Configure and load a preset before starting any activity");
        ImGui.Separator();

        ImGui.Text("Prayer & Crafting Activities");
        
        if (ImGui.Button("Start Powder of Burials")) {
            coaezUtility.setBotState(CoaezUtility.BotState.POWDER_OF_BURIALS);
        }
        
        if (ImGui.Button("Start Gem Crafting")) {
            coaezUtility.setBotState(CoaezUtility.BotState.GEM_CRAFTING);
        }
        
        ImGui.Separator();
        
        ImGui.Text("Archaeology Activities");
        
        if (ImGui.Button("Start Soil Sifting (Spell)")) {
            coaezUtility.setBotState(CoaezUtility.BotState.SIFT_SOIL);
        }
        
        if (ImGui.Button("Start Soil Screening (Mesh)")) {
            coaezUtility.setBotState(CoaezUtility.BotState.SCREEN_MESH);
        }
                
        ImGui.SeparatorText("Combat Activities");
        
        if (ImGui.Button("Start Player Owned Dungeon")) {
            coaezUtility.setBotState(CoaezUtility.BotState.POSD);
        }

        ImGui.SeparatorText("Invention");
        if (ImGui.Button("Start invention")) {
            coaezUtility.setBotState(CoaezUtility.BotState.INVENTION);
        } 

        ImGui.SeparatorText("Enchanting");
        if (ImGui.Button("Start enchanting")) {
            coaezUtility.setBotState(CoaezUtility.BotState.ENCHANTING);
        }

    }
    
    private void renderAlchemyTab() {
        ImGui.Text("High Level Alchemy");
        
        String alchemyButtonText = coaezUtility.getBotState() == CoaezUtility.BotState.ALCHEMY ? 
            "Stop Alchemy" : "Start Alchemy";
        if (ImGui.Button(alchemyButtonText)) {
            coaezUtility.setBotState(coaezUtility.getBotState() == CoaezUtility.BotState.ALCHEMY ? 
                CoaezUtility.BotState.IDLE : CoaezUtility.BotState.ALCHEMY);
        }
        
        ImGui.Separator();
        
        ImGui.Text("Items to Alchemize");
        
        if (ImGui.ListBoxHeader("##AlchemyItems", 300, LISTBOX_HEIGHT)) {
            List<String> alchemyItems = coaezUtility.getAlchemy().getAlchemyItems();
            for (int i = 0; i < alchemyItems.size(); i++) {
                String itemName = alchemyItems.get(i);
                ImGui.PushID("alch_" + i);
                ImGui.Text(itemName);
                ImGui.SameLine();
                if (ImGui.Button("Remove")) {
                    coaezUtility.getAlchemy().removeAlchemyItem(itemName);
                }
                ImGui.PopID();
            }
            ImGui.ListBoxFooter();
        }
        
        alchemyInput = ImGui.InputText("Item Name##Alch", alchemyInput);
        if (ImGui.Button("Add##Alch") && !alchemyInput.isEmpty()) {
            coaezUtility.getAlchemy().addAlchemyItem(alchemyInput);
            preloadedAlchemyItems.add(alchemyInput);
            alchemyInput = "";
        }
    }
    
    private void renderDisassemblyTab() {
        ImGui.Text("Disassembly");
        
        String disassemblyButtonText = coaezUtility.getBotState() == CoaezUtility.BotState.DISASSEMBLY ? 
            "Stop Disassembly" : "Start Disassembly";
        if (ImGui.Button(disassemblyButtonText)) {
            coaezUtility.setBotState(coaezUtility.getBotState() == CoaezUtility.BotState.DISASSEMBLY ? 
                CoaezUtility.BotState.IDLE : CoaezUtility.BotState.DISASSEMBLY);
        }
        
        ImGui.Separator();
        
        ImGui.Text("Items to Disassemble");
        
        if (ImGui.ListBoxHeader("##DisassemblyItems", 300, LISTBOX_HEIGHT)) {
            List<String> disassemblyItems = coaezUtility.getDisassembly().getDisassemblyItems();
            for (int i = 0; i < disassemblyItems.size(); i++) {
                String itemName = disassemblyItems.get(i);
                ImGui.PushID("disassembly_" + i);
                ImGui.Text(itemName);
                ImGui.SameLine();
                if (ImGui.Button("Remove")) {
                    coaezUtility.getDisassembly().removeDisassemblyItem(itemName);
                }
                ImGui.PopID();
            }
            ImGui.ListBoxFooter();
        }
        
        disassemblyInput = ImGui.InputText("Item Name##Disassembly", disassemblyInput);
        if (ImGui.Button("Add##Disassembly") && !disassemblyInput.isEmpty()) {
            coaezUtility.getDisassembly().addDisassemblyItem(disassemblyInput);
            disassemblyInput = "";
        }
    }
    
    private void renderPOSDTab() {
        ImGui.Text("Player Owned Dungeon Settings");
        ImGui.Separator();
        

        // Checkboxes for options
        boolean useOverloads = coaezUtility.getPOSD().isUseOverloads();
        if (ImGui.Checkbox("Use Overloads", useOverloads)) {
            coaezUtility.getPOSD().setUseOverloads(!useOverloads);
            saveConfig();
        }
        
        boolean usePrayerPots = coaezUtility.getPOSD().isUsePrayerPots();
        if (ImGui.Checkbox("Use Prayer Potions", usePrayerPots)) {
            coaezUtility.getPOSD().setUsePrayerPots(!usePrayerPots);
            saveConfig();
        }
        
        boolean useAggroPots = coaezUtility.getPOSD().isUseAggroPots();
        if (ImGui.Checkbox("Use Aggression Potions", useAggroPots)) {
            coaezUtility.getPOSD().setUseAggroPots(!useAggroPots);
            saveConfig();
        }
        
        boolean useWeaponPoison = coaezUtility.getPOSD().isUseWeaponPoison();
        if (ImGui.Checkbox("Use Weapon Poison", useWeaponPoison)) {
            coaezUtility.getPOSD().setUseWeaponPoison(!useWeaponPoison);
            saveConfig();
        }
        
        boolean useQuickPrayers = coaezUtility.getPOSD().isUseQuickPrayers();
        if (ImGui.Checkbox("Use Quick Prayers", useQuickPrayers)) {
            coaezUtility.getPOSD().setUseQuickPrayers(!useQuickPrayers);
            saveConfig();
        }
        
        // Sliders for thresholds
        int quickPrayersNumber = coaezUtility.getPOSD().getQuickPrayersNumber();
        int newQuickPrayersNumber = ImGui.Slider("Quick Prayers Number", quickPrayersNumber, 1, 10, 0);
        if (newQuickPrayersNumber != quickPrayersNumber) {
            coaezUtility.getPOSD().setQuickPrayersNumber(newQuickPrayersNumber);
            saveConfig();
        }

        int healthThreshold = coaezUtility.getPOSD().getHealthPointsThreshold();
        int newHealthThreshold = ImGui.Slider("Health Threshold (%)", healthThreshold, 10, 90, 0);
        if (newHealthThreshold != healthThreshold) {
            coaezUtility.getPOSD().setHealthPointsThreshold(newHealthThreshold);
            saveConfig();
        }

        int prayerThreshold = coaezUtility.getPOSD().getPrayerPointsThreshold();
        int newPrayerThreshold = ImGui.Slider("Prayer Threshold", prayerThreshold, 100, 10000, 0);
        if (newPrayerThreshold != prayerThreshold) {
            coaezUtility.getPOSD().setPrayerPointsThreshold(newPrayerThreshold);
            saveConfig();
        }
        
        // More checkboxes
        boolean useLoot = coaezUtility.getPOSD().isUseLoot();
        if (ImGui.Checkbox("Use Loot", useLoot)) {
            coaezUtility.getPOSD().setUseLoot(!useLoot);
            saveConfig();
        }
        
        boolean lootAll = coaezUtility.getPOSD().isInteractWithLootAll();
        if (ImGui.Checkbox("Loot All", lootAll)) {
            coaezUtility.getPOSD().setInteractWithLootAll(!lootAll);
            saveConfig();
        }
        
        boolean useScrimshaws = coaezUtility.getPOSD().isUseScrimshaws();
        if (ImGui.Checkbox("Use Scrimshaws", useScrimshaws)) {
            coaezUtility.getPOSD().setUseScrimshaws(!useScrimshaws);
            saveConfig();
        }
        
        boolean bankForFood = coaezUtility.getPOSD().isBankForFood();
        if (ImGui.Checkbox("Bank For Food", bankForFood)) {
            coaezUtility.getPOSD().setBankForFood(!bankForFood);
            saveConfig();
        }
        
        // Target items input
        ImGui.Separator();
        ImGui.Text("Target Items for Looting");

        posdLootInput = ImGui.InputText("Item Name##POSD", posdLootInput);
        if (ImGui.Button("Add##POSD") && !posdLootInput.isEmpty()) {
            coaezUtility.getPOSD().addTargetItem(posdLootInput);
            posdLootInput = "";
            saveConfig();
        }

        ImGui.SameLine();
        if (ImGui.Button("Clear All##POSD")) {
            coaezUtility.getPOSD().clearTargetItems();
            saveConfig();
        }

        // Display target items in a listbox
        if (ImGui.ListBoxHeader("##POSDItems", 300, LISTBOX_HEIGHT)) {
            List<String> targetItems = coaezUtility.getPOSD().getTargetItemNames();
            for (int i = 0; i < targetItems.size(); i++) {
                String itemName = targetItems.get(i);
                ImGui.PushID("posd_" + i);
                ImGui.Text(itemName);
                ImGui.SameLine();
                if (ImGui.Button("Remove")) {
                    coaezUtility.getPOSD().removeTargetItem(itemName);
                    saveConfig();
                }
                ImGui.PopID();
            }
            ImGui.ListBoxFooter();
        }
        
        ImGui.Separator();
        
        if (ImGui.Button("Start POSD")) {
            coaezUtility.setBotState(CoaezUtility.BotState.POSD);
            saveConfig();
        }
    }
    
    private void saveConfig() {
        ScriptConfig config = coaezUtility.getConfig();
        if (config != null) {
            config.addProperty("botState", coaezUtility.getBotState().name());
            
            List<String> alchemyItems = coaezUtility.getAlchemy().getAlchemyItems();
            config.addProperty("alchemyItems", String.join(",", alchemyItems));
            
            List<String> disassemblyItems = coaezUtility.getDisassembly().getDisassemblyItems();
            config.addProperty("disassemblyItems", String.join(",", disassemblyItems));
            
            config.addProperty("posdUseOverloads", String.valueOf(coaezUtility.getPOSD().isUseOverloads()));
            config.addProperty("posdUsePrayerPots", String.valueOf(coaezUtility.getPOSD().isUsePrayerPots()));
            config.addProperty("posdUseAggroPots", String.valueOf(coaezUtility.getPOSD().isUseAggroPots()));
            config.addProperty("posdUseWeaponPoison", String.valueOf(coaezUtility.getPOSD().isUseWeaponPoison()));
            config.addProperty("posdUseQuickPrayers", String.valueOf(coaezUtility.getPOSD().isUseQuickPrayers()));
            config.addProperty("posdQuickPrayersNumber", String.valueOf(coaezUtility.getPOSD().getQuickPrayersNumber()));
            config.addProperty("posdHealthThreshold", String.valueOf(coaezUtility.getPOSD().getHealthPointsThreshold()));
            config.addProperty("posdPrayerThreshold", String.valueOf(coaezUtility.getPOSD().getPrayerPointsThreshold()));
            config.addProperty("posdUseLoot", String.valueOf(coaezUtility.getPOSD().isUseLoot()));
            config.addProperty("posdLootAll", String.valueOf(coaezUtility.getPOSD().isInteractWithLootAll()));
            config.addProperty("posdUseScrimshaws", String.valueOf(coaezUtility.getPOSD().isUseScrimshaws()));
            config.addProperty("posdBankForFood", String.valueOf(coaezUtility.getPOSD().isBankForFood() ));
            
            List<String> targetItems = coaezUtility.getPOSD().getTargetItemNames();
            config.addProperty("posdTargetItems", String.join(",", targetItems));
            
            config.save();
        }
    }
    
    private void loadConfig() {
        ScriptConfig config = coaezUtility.getConfig();
        if (config != null) {
            config.load();
            
            String botStateValue = config.getProperty("botState");
            if (botStateValue != null) {
                try {
                    coaezUtility.setBotState(CoaezUtility.BotState.valueOf(botStateValue));
                } catch (IllegalArgumentException e) {
                    coaezUtility.setBotState(CoaezUtility.BotState.IDLE);
                }
            }

            // Load Alchemy settings
            String alchemyItems = config.getProperty("alchemyItems");
            if (alchemyItems != null && !alchemyItems.isEmpty()) {
                Arrays.stream(alchemyItems.split(","))
                        .forEach(item -> coaezUtility.getAlchemy().addAlchemyItem(item));
            }
            
            // Load Disassembly settings
            String disassemblyItems = config.getProperty("disassemblyItems");
            if (disassemblyItems != null && !disassemblyItems.isEmpty()) {
                Arrays.stream(disassemblyItems.split(","))
                        .forEach(item -> coaezUtility.getDisassembly().addDisassemblyItem(item));
            }
            
            // Load POSD settings
            String useOverloads = config.getProperty("posdUseOverloads");
            if (useOverloads != null) {
                coaezUtility.getPOSD().setUseOverloads(Boolean.parseBoolean(useOverloads));
            }
            
            String usePrayerPots = config.getProperty("posdUsePrayerPots");
            if (usePrayerPots != null) {
                coaezUtility.getPOSD().setUsePrayerPots(Boolean.parseBoolean(usePrayerPots));
            }
            
            String useAggroPots = config.getProperty("posdUseAggroPots");
            if (useAggroPots != null) {
                coaezUtility.getPOSD().setUseAggroPots(Boolean.parseBoolean(useAggroPots));
            }
            
            String useWeaponPoison = config.getProperty("posdUseWeaponPoison");
            if (useWeaponPoison != null) {
                coaezUtility.getPOSD().setUseWeaponPoison(Boolean.parseBoolean(useWeaponPoison));
            }
            
            String useQuickPrayers = config.getProperty("posdUseQuickPrayers");
            if (useQuickPrayers != null) {
                coaezUtility.getPOSD().setUseQuickPrayers(Boolean.parseBoolean(useQuickPrayers));
            }
            
            String quickPrayersNumber = config.getProperty("posdQuickPrayersNumber");
            if (quickPrayersNumber != null) {
                coaezUtility.getPOSD().setQuickPrayersNumber(Integer.parseInt(quickPrayersNumber));
            }
            
            String healthThreshold = config.getProperty("posdHealthThreshold");
            if (healthThreshold != null) {
                coaezUtility.getPOSD().setHealthPointsThreshold(Integer.parseInt(healthThreshold));
            }
            
            String prayerThreshold = config.getProperty("posdPrayerThreshold");
            if (prayerThreshold != null) {
                coaezUtility.getPOSD().setPrayerPointsThreshold(Integer.parseInt(prayerThreshold));
            }
            
            String useLoot = config.getProperty("posdUseLoot");
            if (useLoot != null) {
                coaezUtility.getPOSD().setUseLoot(Boolean.parseBoolean(useLoot));
            }
            
            String lootAll = config.getProperty("posdLootAll");
            if (lootAll != null) {
                coaezUtility.getPOSD().setInteractWithLootAll(Boolean.parseBoolean(lootAll));
            }
            
            String useScrimshaws = config.getProperty("posdUseScrimshaws");
            if (useScrimshaws != null) {
                coaezUtility.getPOSD().setUseScrimshaws(Boolean.parseBoolean(useScrimshaws));
            }
            
            String bankForFood = config.getProperty("posdBankForFood");
            if (bankForFood != null) {
                coaezUtility.getPOSD().setBankForFood(Boolean.parseBoolean(bankForFood));
            }
            
            String targetItems = config.getProperty("posdTargetItems");
            if (targetItems != null && !targetItems.isEmpty()) {
                Arrays.stream(targetItems.split(","))
                        .forEach(item -> coaezUtility.getPOSD().addTargetItem(item));
            }
        }
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}