package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;

import static net.botwithus.rs3.script.ScriptConsole.println;

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


    public CoaezUtilityGUI(ScriptConsole scriptConsole, CoaezUtility coaezUtility) {
        super(scriptConsole);
        this.coaezUtility = coaezUtility;

        loadConfig();
        lastBotState = coaezUtility.getBotState();
    }

    public boolean hasStateChanged() {
        if (lastBotState != coaezUtility.getBotState()) {
            lastBotState = coaezUtility.getBotState();
            return true;
        }
        return false;
    }

    public void drawSettings() {
        if (ImGui.Begin("Coaez Utility Settings", ImGuiWindowFlag.AlwaysAutoResize.getValue())) {
            ImGui.Text("Activity Selection");
            ImGui.Separator();
    
            if (ImGui.BeginTabBar("MainTabBar", 0)) {
                if (ImGui.BeginTabItem("Main Activities", 0)) {
                    renderMainActivities();
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Alchemy & Disassembly", 0)) {
                    renderAlchemyAndDisassembly();
                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
    
            if (ImGui.Button("Stop Script")) {
                coaezUtility.setBotState(CoaezUtility.BotState.STOPPED);
            }
    
            ImGui.Text("Current State: " + coaezUtility.getBotState().toString());
    
            if (hasStateChanged()) {
                saveConfig();
            }
    
            ImGui.End();
        }
    }
    
    private void renderMainActivities() {
    ImGui.Text("Configure and load a preset before starting");

    if (ImGui.Button("Start Powder of Burials")) {
        coaezUtility.setBotState(CoaezUtility.BotState.POWDER_OF_BURIALS);
    }

    ImGui.Separator();

    if (ImGui.Button("Start Soil Sifting")) {
        coaezUtility.setBotState(CoaezUtility.BotState.SIFT_SOIL);
    }
}

private void renderAlchemyAndDisassembly() {
    // Alchemy Section
    ImGui.Text("High Alchemy");
    if (ImGui.Button(coaezUtility.getBotState() == CoaezUtility.BotState.ALCHEMY ? 
        "Stop Alchemy" : "Start Alchemy")) {
        coaezUtility.setBotState(coaezUtility.getBotState() == CoaezUtility.BotState.ALCHEMY ? 
            CoaezUtility.BotState.IDLE : CoaezUtility.BotState.ALCHEMY);
    }

    ImGui.Text("Items to Alchemize");
    if (ImGui.ListBoxHeader("##AlchemyItems", 300, 150)) {
        List<String> alchemyItems = coaezUtility.getAlchemy().getAlchemyItems();
        for (int i = 0; i < alchemyItems.size(); i++) {
            String itemName = alchemyItems.get(i);
            ImGui.Text(itemName);
            ImGui.SameLine();
            if (ImGui.Button("Remove##Alch" + i)) {
                coaezUtility.getAlchemy().removeAlchemyItem(itemName);
            }
        }
        ImGui.ListBoxFooter();
    }

    alchemyInput = ImGui.InputText("Item Name##Alch", alchemyInput);
    if (ImGui.Button("Add##Alch") && !alchemyInput.isEmpty()) {
        coaezUtility.getAlchemy().addAlchemyItem(alchemyInput);
        preloadedAlchemyItems.add(alchemyInput);
        alchemyInput = "";
    }

    ImGui.Separator();

    // Disassembly Section
    ImGui.Text("Disassembly");
    if (ImGui.Button(coaezUtility.getBotState() == CoaezUtility.BotState.DISASSEMBLY ? 
        "Stop Disassembly" : "Start Disassembly")) {
        coaezUtility.setBotState(coaezUtility.getBotState() == CoaezUtility.BotState.DISASSEMBLY ? 
            CoaezUtility.BotState.IDLE : CoaezUtility.BotState.DISASSEMBLY);
    }

    ImGui.Text("Items to Disassemble");
    if (ImGui.ListBoxHeader("##DisassemblyItems", 300, 150)) {
        List<String> disassemblyItems = coaezUtility.getDisassembly().getDisassemblyItems();
        for (int i = 0; i < disassemblyItems.size(); i++) {
            String itemName = disassemblyItems.get(i);
            ImGui.Text(itemName);
            ImGui.SameLine();
            if (ImGui.Button("Remove##Dis" + i)) {
                coaezUtility.getDisassembly().removeDisassemblyItem(itemName);
            }
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
    }
}