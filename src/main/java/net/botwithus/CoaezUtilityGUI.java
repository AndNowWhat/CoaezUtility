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
    private Set<String> preloadedAlchemyItems = new HashSet<>();
    private Set<String> preloadedDisassemblyItems = new HashSet<>();

    // Window dimensions
    private final int LISTBOX_HEIGHT = 150;

    public CoaezUtilityGUI(ScriptConsole scriptConsole, CoaezUtility coaezUtility) {
        super(scriptConsole);
        this.coaezUtility = coaezUtility;
        lastBotState = coaezUtility.getBotState();
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
        ImGui.Text("Invention Disassembly");
        
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
                ImGui.PushID("dis_" + i);
                ImGui.Text(itemName);
                ImGui.SameLine();
                if (ImGui.Button("Remove")) {
                    coaezUtility.getDisassembly().removeDisassemblyItem(itemName);
                }
                ImGui.PopID();
            }
            ImGui.ListBoxFooter();
        }
        
        disassemblyInput = ImGui.InputText("Item Name##Dis", disassemblyInput);
        if (ImGui.Button("Add##Dis") && !disassemblyInput.isEmpty()) {
            coaezUtility.getDisassembly().addDisassemblyItem(disassemblyInput);
            preloadedDisassemblyItems.add(disassemblyInput);
            disassemblyInput = "";
        }
    }

    public void saveConfig() {
        ScriptConfig config = coaezUtility.getConfig();
        if (config != null) {
            config.addProperty("botState", coaezUtility.getBotState().toString());
            List<String> alchemyItems = coaezUtility.getAlchemy().getAlchemyItems();
            config.addProperty("alchemyItems", String.join(",", alchemyItems));
            
            List<String> disassemblyItems = coaezUtility.getDisassembly().getDisassemblyItems();
            config.addProperty("disassemblyItems", String.join(",", disassemblyItems));
            
            config.save();
        }
    }
    
    public void loadConfig() {
        ScriptConfig config = coaezUtility.getConfig();
        if (config != null) {
            config.load();
            
            String botStateValue = config.getProperty("botState");
            if (botStateValue != null) {
                coaezUtility.setBotState(CoaezUtility.BotState.valueOf(botStateValue));
            }

            String alchemyItems = config.getProperty("alchemyItems");
            if (alchemyItems != null && !alchemyItems.isEmpty()) {
                Arrays.stream(alchemyItems.split(","))
                        .forEach(item -> coaezUtility.getAlchemy().addAlchemyItem(item));
            }
            
            String disassemblyItems = config.getProperty("disassemblyItems");
            if (disassemblyItems != null && !disassemblyItems.isEmpty()) {
                Arrays.stream(disassemblyItems.split(","))
                        .forEach(item -> coaezUtility.getDisassembly().addDisassemblyItem(item));
            }
        }
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}