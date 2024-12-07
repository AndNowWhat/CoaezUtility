package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;

import static net.botwithus.rs3.script.ScriptConsole.println;

public class CoaezUtilityGUI extends ScriptGraphicsContext {
    private final CoaezUtility coaezUtility;
    private CoaezUtility.BotState lastBotState;

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

    public void saveConfig() {
        ScriptConfig config = coaezUtility.getConfig();
        if (config != null) {
            config.addProperty("botState", coaezUtility.getBotState().toString());
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
        }
    }

    public void drawSettings() {
        if (ImGui.Begin("Coaez Utility Settings", ImGuiWindowFlag.AlwaysAutoResize.getValue())) {
            ImGui.Text("Activity Selection");
            ImGui.Separator();
            ImGui.Text("Configure and load a preset before starting");

            if (ImGui.Button("Start Powder of Burials")) {
                coaezUtility.setBotState(CoaezUtility.BotState.POWDER_OF_BURIALS);
            }

            ImGui.Separator();

            if (ImGui.Button("Start Soil Sifting")) {
                coaezUtility.setBotState(CoaezUtility.BotState.SIFT_SOIL);
            }

            ImGui.Separator();

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

    @Override
    public void drawOverlay() {
    }
}