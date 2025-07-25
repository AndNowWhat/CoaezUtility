package net.botwithus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.botwithus.GuiStyling.ImGuiCol;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.quest.Quest;
import net.botwithus.rs3.imgui.BGList;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.tasks.BeachActivity;
import net.botwithus.tasks.BeachEventTask;
import net.botwithus.tasks.Ingredient;
import net.botwithus.tasks.MapNavigatorTask;
import net.botwithus.tasks.Portable;
import net.botwithus.tasks.PortableCrafter;
import net.botwithus.tasks.PortableSawmill;
import net.botwithus.tasks.PortableType;
import net.botwithus.tasks.PortableWorkbench;
import net.botwithus.tasks.Product;
import net.botwithus.tasks.QuestDialogFetcher;
import net.botwithus.tasks.QuestHelper;
import net.botwithus.tasks.SawmillPlank;
import net.botwithus.tasks.SimplePortable;
import net.botwithus.tasks.sorceressgarden.models.GardenType;

public class CoaezUtilityGUI extends ScriptGraphicsContext {
    private final CoaezUtility coaezUtility;
    private CoaezUtility.BotState lastBotState;
    private String alchemyInput = "";
    private String disassemblyInput = "";
    private String posdLootInput = "";
    private Set<String> preloadedAlchemyItems = new HashSet<>();
    private Set<String> preloadedDisassemblyItems = new HashSet<>();
    
    // Flipping GUI state
    private String flippingItemInput = "";
    private String flippingItemIdInput = "";
    private String flippingBuyLimitInput = "";
    private int selectedFlipHistoryIndex = -1;

    private int selectedPortableTypeIndex = 0;
    private final PortableType[] portableTypes = PortableType.values();
    
    // Workbench specific state
    private int selectedGroupIndex = 0;
    private List<Integer> currentGroupIds = new ArrayList<>();
    private List<String> currentGroupNames = new ArrayList<>();
    private int selectedWorkbenchProductIndex = 0;
    private List<Product> currentWorkbenchProducts = new ArrayList<>();

    // Crafter specific state
    private int selectedCrafterOptionIndex = 0;
    private int selectedCrafterGroupIndex = 0;
    private int selectedCrafterProductIndex = 0;
    private List<Product> currentCrafterProducts = new ArrayList<>();
    private List<Integer> currentCrafterGroupIds = new ArrayList<>();
    private List<String> currentCrafterGroupNames = new ArrayList<>();

    // Sawmill specific state
    private int selectedSawmillPlankIndex = 0;
    private final SawmillPlank[] sawmillPlanks = SawmillPlank.values();

    // Window dimensions
    private final int LISTBOX_HEIGHT = 150;
    
    // GUI Styling
    private final GuiStyling guiStyling = new GuiStyling();
    
    // Overlay transparency
    private float overlayTransparency = 0.85f;
    
    // Beach Event state
    private int selectedBeachActivityIndex = 0;
    private final BeachActivity[] beachActivities = BeachActivity.values();
    private boolean beachUseCocktails = false;
    private boolean beachFightClawdia = true;
    private boolean beachUseSpotlight = false;
    private boolean beachUseBattleship = false;
    private boolean beachIsWeekend = false;
    private String beachSpotlightHappyHour = "Hunter";
    private final String[] spotlightHappyHourOptions = {"Dung", "Strength", "Construction", "Hunter", "Ranged", "Cooking", "Farming"};
    private int selectedSpotlightHappyHourIndex = 3;
    
    // Map Navigator state
    private int selectedLocationIndex = 0;
    private String locationSearchText = "";
    private String[] currentLocationNames = new String[0];
    
    // Sorceress Garden state
    private boolean sorceressGardenActive = false;
    
    private boolean guiUsePinkFizz = false;
    private boolean guiUsePurpleLumbridge = false;
    private boolean guiUsePineappletini = false;
    private boolean guiUseLemonSour = false;
    private boolean guiUseFishermanssFriend = false;
    private boolean guiUseGeorgesPeachDelight = false;
    private boolean guiUseAHoleInOne = false;
    private boolean guiUsePalmerFarmer = false;
    private boolean guiUseUglyDuckling = false;
    
    private boolean isLoadingConfig = false;
    
    // Add fields to store window size and position
    private float consoleWindowWidth = 600;
    private float consoleWindowHeight = 300;
    private float consoleWindowPosX = 100;
    private float consoleWindowPosY = 100;

    public CoaezUtilityGUI(ScriptConsole scriptConsole, CoaezUtility coaezUtility) {
        super(scriptConsole);
        this.coaezUtility = coaezUtility;
        
        if (this.coaezUtility != null) {
            lastBotState = this.coaezUtility.getBotState();
            loadConfig(); // This calls updateActivePortableType internally
            updateBeachEventSettings(); // Apply loaded beach event settings to task

            // Initialize GUI state based on current task state AFTER loadConfig
            // This section should align the GUI with whatever state was set by loadConfig
            // or the default state if loadConfig didn't set a portable.
            if (this.coaezUtility.getPortableTask() != null && this.coaezUtility.getPortableTask().getActivePortable() != null) {
                PortableType currentTaskType = this.coaezUtility.getPortableTask().getActivePortable().getType();
                 
                for (int i = 0; i < portableTypes.length; i++) {
                    if (portableTypes[i] == currentTaskType) {
                        selectedPortableTypeIndex = i;
                        break;
                    }
                }

                if (currentTaskType == PortableType.WORKBENCH && this.coaezUtility.getPortableTask().getActivePortable() instanceof PortableWorkbench) {
                    PortableWorkbench wbInstance = (PortableWorkbench) this.coaezUtility.getPortableTask().getActivePortable();

                    // Update group lists from the workbench instance
                    this.currentGroupIds = wbInstance.getGroupEnumIds();
                    if (this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                        this.currentGroupNames = this.currentGroupIds.stream()
                                                           .map(wbInstance::getGroupName)
                                                           .collect(Collectors.toList());
                        // selectedGroupIndex would have been set by loadConfig or defaults to 0
                        if(selectedGroupIndex >= this.currentGroupIds.size()) selectedGroupIndex = 0;

                        // Update product list for the current group
                        int activeGroupId = this.currentGroupIds.get(selectedGroupIndex);
                        this.currentWorkbenchProducts = wbInstance.getProductsForGroup(activeGroupId);

                        Product taskSelectedProduct = this.coaezUtility.getPortableTask().getSelectedProduct();
                        if (taskSelectedProduct != null && !this.currentWorkbenchProducts.isEmpty()) {
                            boolean found = false;
                            for (int i = 0; i < this.currentWorkbenchProducts.size(); i++) {
                                if (this.currentWorkbenchProducts.get(i).getId() == taskSelectedProduct.getId()) {
                                    selectedWorkbenchProductIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) selectedWorkbenchProductIndex = 0; // Default to first if not found in current list
                        } else {
                            selectedWorkbenchProductIndex = 0;
                        }
                         
                    } else {
                         
                        this.currentGroupNames.clear();
                        this.currentWorkbenchProducts.clear();
                        selectedGroupIndex = 0;
                        selectedWorkbenchProductIndex = 0;
                    }
                }
            } else {
                 
                 // Reset workbench specific state if no active portable
                this.currentGroupIds.clear();
                this.currentGroupNames.clear();
                this.currentWorkbenchProducts.clear();
                selectedGroupIndex = 0;
                selectedWorkbenchProductIndex = 0;
            }
        } else {
             // ScriptConsole.println("[CoaezUtilityGUI] CRITICAL: CoaezUtility instance is null in GUI constructor.");
        }
        
    }

    public boolean hasStateChanged() {
        if (coaezUtility == null) return false; // Guard
        if (lastBotState != coaezUtility.getBotState()) {
            lastBotState = coaezUtility.getBotState();
            return true;
        }
        return false;
    }

    @Override
    public void drawSettings() {
        if (coaezUtility == null) {
            ImGui.Text("Error: CoaezUtility not available.");
            return;
        }
        
        // Apply custom styling
        guiStyling.applyCustomColors();
        guiStyling.applyCustomStyles();
        
        try {
            if (ImGui.Begin("Coaez Utility", ImGuiWindowFlag.None.getValue())) {
                ImGui.Text("Current State: ");
                ImGui.SameLine();
                ImGui.PushStyleColor(0, 0.25f, 0.78f, 0.71f, 1.0f); // Teal accent color
                ImGui.Text(coaezUtility.getBotState().name());
                ImGui.PopStyleColor();
                
                // Show active task details if available
                if (coaezUtility.getBotState() != CoaezUtility.BotState.IDLE) {
                    ImGui.Text("Active Task Details:");
                    ImGui.PushStyleColor(0, 0.4f, 0.9f, 0.7f, 1.0f); // Lighter teal
                    
                    switch (coaezUtility.getBotState()) {
                        case PORTABLES:
                            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
                                Portable portable = coaezUtility.getPortableTask().getActivePortable();
                                ImGui.Text("  Portable: " + portable.getType().getName());
                                if (portable instanceof PortableWorkbench && ((PortableWorkbench) portable).getSelectedProduct() != null) {
                                    ImGui.Text("  Product: " + ((PortableWorkbench) portable).getSelectedProduct().getName());
                                } else if (portable instanceof PortableCrafter && ((PortableCrafter) portable).getSelectedProduct() != null) {
                                    ImGui.Text("  Product: " + ((PortableCrafter) portable).getSelectedProduct().getName());
                                } else if (portable instanceof PortableSawmill && ((PortableSawmill) portable).getSelectedPlank() != null) {
                                    ImGui.Text("  Plank: " + ((PortableSawmill) portable).getSelectedPlank().getDisplayName());
                                }
                            }
                            break;
                        case QUESTS:
                            if (coaezUtility.getQuestHelper() != null && coaezUtility.getQuestHelper().getSelectedQuest() != null) {
                                ImGui.Text("  Quest: " + coaezUtility.getQuestHelper().getSelectedQuest().name());
                            }
                            break;
                        case BEACH_EVENT:
                            if (coaezUtility.getBeachEventTask() != null) {
                                ImGui.Text("  Activity: " + coaezUtility.getBeachEventTask().getSelectedActivity().getName());
                            }
                            break;
                        case SORCERESS_GARDEN:
                            if (coaezUtility.getSorceressGardenTask() != null) {
                                Set<GardenType> gardens = coaezUtility.getSorceressGardenTask().getSelectedGardens();
                                if (!gardens.isEmpty()) {
                                    ImGui.Text("  Gardens: " + gardens.stream().map(GardenType::name).collect(Collectors.joining(", ")));
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    
                    ImGui.PopStyleColor();
                }
                
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
                    if (ImGui.BeginTabItem("Portables", 0)) {
                        renderPortablesTab();
                        ImGui.EndTabItem();
                    }
                    if (ImGui.BeginTabItem("Quests", 0)) {
                        renderQuestsTab();
                        ImGui.EndTabItem();
                    }
                    if (ImGui.BeginTabItem("Beach Event", 0)) {
                        renderBeachEventTab();
                        ImGui.EndTabItem();
                    }
                    if (ImGui.BeginTabItem("Map Navigator", 0)) {
                        renderMapNavigatorTab();
                        ImGui.EndTabItem();
                    }
                    if (ImGui.BeginTabItem("Sorceress Garden", 0)) {
                        renderSorceressGardenTab();
                        ImGui.EndTabItem();
                    }
                    /* if (ImGui.BeginTabItem("Smithing", 0)) {
                        renderSmithingTab();
                        ImGui.EndTabItem();
                    } */
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
        } finally {
            // Reset custom styling
            guiStyling.resetCustomStyles();
            guiStyling.resetCustomColors();
        }
    }
    
    private void renderActivitiesTab() {
        ImGui.Text("Configure and load a preset before starting any activity");
        ImGui.Separator();

        // Create a child window for better button alignment
        if (ImGui.BeginChild("ActivitiesChild", 0, 0, true, 0)) {
            
            // Setup table with 3 columns
            if (ImGui.BeginTable("ActivitiesTable", 3, 0)) {
                ImGui.TableSetupColumn("Prayer & Crafting", 0);
                ImGui.TableSetupColumn("Archaeology & Combat", 0);
                ImGui.TableSetupColumn("Training & Misc", 0);
                ImGui.TableHeadersRow();
                
                // Row 1
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Powder of Burials")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.POWDER_OF_BURIALS);
                }
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Soil Sifting (Spell)")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SIFT_SOIL);
                }
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Deploy Dummy")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.DEPLOY_DUMMY);
                }
                
                // Row 2
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Sheep Shearing")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SHEEP_SHEARING);
                }
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Soil Screening (Mesh)")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SCREEN_MESH);
                }
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Invention")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.INVENTION);
                }
                
                // Row 3
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Gem Crafting")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.GEM_CRAFTING);
                }
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Attack/Deploy Pinata")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SUMMER_PINATA);
                }
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Enchanting")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.ENCHANTING);
                }
                
                // Row 4
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Fungal Bowstrings")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.FUNGAL_BOWSTRINGS);
                }
                ImGui.TableNextColumn();
                // Empty cell
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Sandy Clues")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SANDY_CLUES);
                }
                
                // Row 5
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Soft Clay")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SOFTCLAY);
                }
                ImGui.TableNextColumn();
                // Empty cell
                ImGui.TableNextColumn();
                if (ImGui.Button("Start NPC Logger")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.NPC_LOGGER);
                }
                
                // Row 6
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Limestone")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.LIMESTONE);
                }
                ImGui.TableNextColumn();
                // Empty cell
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Winter Sq'irkjuice")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.WINTER_SQIRKJUICE);
                }
                
                // Row 7
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Limestone Brick")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.LIMESTONE_BRICK);
                }
                ImGui.TableNextColumn();
                // Empty cell
                ImGui.TableNextColumn();
                if (ImGui.Button("Turn In Sq'irkjuice")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.TURN_IN_SQIRKJUICE);
                }
                
                // Row 8
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Teleport to Camelot")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.SOUTH_FELDIPE_HILLS_TELEPORT);
                }
                ImGui.TableNextColumn();
                // Empty cell
                ImGui.TableNextColumn();
                // Empty cell
                
                // Row 9
                ImGui.TableNextRow();
                ImGui.TableNextColumn();
                if (ImGui.Button("Start Beer Crafting")) {
                    coaezUtility.setBotState(CoaezUtility.BotState.BEER_CRAFTING);
                }
                ImGui.TableNextColumn();
                // Empty cell
                ImGui.TableNextColumn();
                // Empty cell
                
                ImGui.EndTable();
            }
            
            ImGui.EndChild();
        }
    }
    
    private void renderAlchemyTab() {
        ImGui.Text("High Level Alchemy");
        
        String alchemyButtonText = (coaezUtility.getBotState() == CoaezUtility.BotState.ALCHEMY) ? 
            "Stop Alchemy" : "Start Alchemy";
        if (ImGui.Button(alchemyButtonText)) {
            coaezUtility.setBotState((coaezUtility.getBotState() == CoaezUtility.BotState.ALCHEMY) ? 
                CoaezUtility.BotState.IDLE : CoaezUtility.BotState.ALCHEMY);
        }
        
        ImGui.Separator();
        
        ImGui.Text("Items to Alchemize");
        
        if (ImGui.ListBoxHeader("##AlchemyItems", 300, LISTBOX_HEIGHT)) {
            if (coaezUtility.getAlchemy() != null) {
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
            }
            ImGui.ListBoxFooter();
        }
        
        alchemyInput = ImGui.InputText("Item Name##Alch", alchemyInput);
        if (ImGui.Button("Add##Alch") && !alchemyInput.isEmpty() && coaezUtility.getAlchemy() != null) {
            coaezUtility.getAlchemy().addAlchemyItem(alchemyInput);
            preloadedAlchemyItems.add(alchemyInput);
            alchemyInput = "";
        }
        
        ImGui.Separator();
        
    }
    
    private void renderDisassemblyTab() {
        ImGui.Text("Disassembly");
        
        String disassemblyButtonText = (coaezUtility.getBotState() == CoaezUtility.BotState.DISASSEMBLY) ? 
            "Stop Disassembly" : "Start Disassembly";
        if (ImGui.Button(disassemblyButtonText)) {
            coaezUtility.setBotState((coaezUtility.getBotState() == CoaezUtility.BotState.DISASSEMBLY) ? 
                CoaezUtility.BotState.IDLE : CoaezUtility.BotState.DISASSEMBLY);
        }
        
        ImGui.Separator();
        
        ImGui.Text("Items to Disassemble");
        
        if (ImGui.ListBoxHeader("##DisassemblyItems", 300, LISTBOX_HEIGHT)) {
            if (coaezUtility.getDisassembly() != null) {
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
            }
            ImGui.ListBoxFooter();
        }
        
        disassemblyInput = ImGui.InputText("Item Name##Disassembly", disassemblyInput);
        if (ImGui.Button("Add##Disassembly") && !disassemblyInput.isEmpty() && coaezUtility.getDisassembly() != null) {
            coaezUtility.getDisassembly().addDisassemblyItem(disassemblyInput);
            disassemblyInput = "";
        }
        
        ImGui.Separator();
        
        // Quick preset buttons for common disassembly items
        ImGui.Text("Quick Presets:");
        if (ImGui.Button("Add Magic Shortbow")) {
            if (coaezUtility.getDisassembly() != null) {
                coaezUtility.getDisassembly().addDisassemblyItem("Magic shortbow");
            }
        }
        ImGui.SameLine();
        if (ImGui.Button("Add Black D'hide Body")) {
            if (coaezUtility.getDisassembly() != null) {
                coaezUtility.getDisassembly().addDisassemblyItem("Black d'hide body");
            }
        }
        ImGui.SameLine();
        if (ImGui.Button("Add Rune Full Helm")) {
            if (coaezUtility.getDisassembly() != null) {
                coaezUtility.getDisassembly().addDisassemblyItem("Rune full helm");
            }
        }
    }
    
    private void renderPortablesTab() {
        ImGui.Text("Portable Skilling Stations");
        ImGui.Separator();

        String[] portableTypeNames = Arrays.stream(portableTypes).map(PortableType::getName).toArray(String[]::new);
        
        int newPortableTypeIndex = ImGui.Combo("Select Portable Type", selectedPortableTypeIndex, portableTypeNames);
        if (newPortableTypeIndex != selectedPortableTypeIndex) {
            selectedPortableTypeIndex = newPortableTypeIndex;
            updateActivePortableType(); // This will also handle workbench product/group loading
        }

        if (portableTypes[selectedPortableTypeIndex] == PortableType.WORKBENCH) {
            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() instanceof PortableWorkbench) {
                PortableWorkbench currentWorkbench = (PortableWorkbench) coaezUtility.getPortableTask().getActivePortable();
                List<Integer> liveGroupIds = currentWorkbench.getGroupEnumIds();
                if (!liveGroupIds.equals(this.currentGroupIds)) {
                    this.currentGroupIds = liveGroupIds;
                    this.currentGroupNames = this.currentGroupIds.stream()
                                                    .map(currentWorkbench::getGroupName)
                                                    .collect(Collectors.toList());
                    if(selectedGroupIndex >= this.currentGroupIds.size()) selectedGroupIndex = 0;
                    if (!this.currentGroupIds.isEmpty()) {
                        this.currentWorkbenchProducts = new ArrayList<>(currentWorkbench.getProductsForGroup(this.currentGroupIds.get(selectedGroupIndex)));
                        if(selectedWorkbenchProductIndex >= this.currentWorkbenchProducts.size()) selectedWorkbenchProductIndex = 0;
                    } else {
                        this.currentWorkbenchProducts.clear();
                        selectedWorkbenchProductIndex = 0;
                    }
                }


                if (!currentGroupNames.isEmpty()) {
                    String[] groupNameArray = currentGroupNames.toArray(String[]::new);
                    if (selectedGroupIndex >= groupNameArray.length && groupNameArray.length > 0) selectedGroupIndex = 0;
                    else if (groupNameArray.length == 0) selectedGroupIndex = 0;


                    int newGroupIndex = ImGui.Combo("Select Group", selectedGroupIndex, groupNameArray);
                    if (newGroupIndex != selectedGroupIndex && newGroupIndex < currentGroupIds.size()) {
                        selectedGroupIndex = newGroupIndex;
                        int selectedGroupId = currentGroupIds.get(selectedGroupIndex);
                        currentWorkbenchProducts = new ArrayList<>(currentWorkbench.getProductsForGroup(selectedGroupId));
                        selectedWorkbenchProductIndex = 0;

                        if (!currentWorkbenchProducts.isEmpty()) {
                             coaezUtility.getPortableTask().setSelectedProduct(currentWorkbenchProducts.get(0));
                        } else {
                             coaezUtility.getPortableTask().setSelectedProduct(null);
                        }
                    }

                    if (!currentWorkbenchProducts.isEmpty()) {
                        String[] productNames = currentWorkbenchProducts.stream()
                                                                    .map(p -> (p != null && p.getName() != null) ? p.getName() : "Unnamed Product")
                                                                    .toArray(String[]::new);

                        if (selectedWorkbenchProductIndex >= productNames.length && productNames.length > 0) selectedWorkbenchProductIndex = 0;
                        else if (productNames.length == 0) selectedWorkbenchProductIndex = 0;

                        int newProductIndex = ImGui.Combo("Select Product", selectedWorkbenchProductIndex, productNames);
                        if (newProductIndex != selectedWorkbenchProductIndex && newProductIndex < currentWorkbenchProducts.size()) {
                            selectedWorkbenchProductIndex = newProductIndex;
                            Product chosenProduct = currentWorkbenchProducts.get(selectedWorkbenchProductIndex);
                            coaezUtility.getPortableTask().setSelectedProduct(chosenProduct);
                            saveConfig();
                        }
                    } else {
                        ImGui.Text("No products available for this group: " + (currentGroupNames.isEmpty() ? "" : currentGroupNames.get(selectedGroupIndex)).replace("%", "%%"));
                    }
                } else {
                     ImGui.Text("No groups loaded for this workbench.");
                }

            } else {
                 ImGui.Text("Workbench selected, but no active workbench task found or task is of wrong type.");
            }
        } else if (portableTypes[selectedPortableTypeIndex] == PortableType.CRAFTER) {
            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() instanceof PortableCrafter) {
                PortableCrafter currentCrafter = (PortableCrafter) coaezUtility.getPortableTask().getActivePortable();

                // Crafter Option Dropdown (Cut Gems, Craft, etc.)
                
                int newCrafterOptionIndex = ImGui.Combo("Select Action", selectedCrafterOptionIndex, PortableCrafter.CRAFTER_OPTIONS);
                
                if (newCrafterOptionIndex != selectedCrafterOptionIndex) {
                    selectedCrafterOptionIndex = newCrafterOptionIndex;
                    String chosenOption = PortableCrafter.CRAFTER_OPTIONS[selectedCrafterOptionIndex];
                    
                    currentCrafter.setSelectedInteractionOption(chosenOption); // Loads groups/products
                    
                    currentCrafterGroupIds = currentCrafter.getGroupEnumIds();
                    
                    if (!currentCrafterGroupIds.isEmpty()) { // Grouped Mode
                        currentCrafterGroupNames = currentCrafterGroupIds.stream()
                                                        .map(currentCrafter::getGroupName)
                                                        .collect(Collectors.toList());
                        selectedCrafterGroupIndex = 0; // Default to first group
                        int defaultGroupId = currentCrafterGroupIds.get(0);
                        currentCrafter.setSelectedGroupId(defaultGroupId);
                        currentCrafterProducts = new ArrayList<>(currentCrafter.getProductsForGroup(defaultGroupId));
                        selectedCrafterProductIndex = 0;
                    } else { // Direct Product Mode (or option doesn't use groups by design)
                        currentCrafterGroupNames.clear();
                        selectedCrafterGroupIndex = 0;
                        currentCrafter.setSelectedGroupId(-1); // Indicate no group is selected for direct mode
                        currentCrafterProducts = new ArrayList<>(currentCrafter.getDirectProducts());
                        selectedCrafterProductIndex = 0;
                    }

                    // Select default product in crafter instance
                    if (!currentCrafterProducts.isEmpty()) {
                        currentCrafter.setSelectedProduct(currentCrafterProducts.get(0));
                    } else {
                        currentCrafter.setSelectedProduct(null);
                    }
                    saveConfig();
                }

                boolean usesGroups = !currentCrafterGroupIds.isEmpty(); 

                if (usesGroups) {
                    // --- Render Grouped Mode UI for Crafter (e.g., Cut Gems) ---
                    if (currentCrafterGroupNames.isEmpty() && !currentCrafterGroupIds.isEmpty()) {
                        // Safety refresh if names are somehow out of sync but IDs exist
                        currentCrafterGroupNames = currentCrafterGroupIds.stream()
                                                        .map(currentCrafter::getGroupName)
                                                        .collect(Collectors.toList());
                        
                    }

                    if (!currentCrafterGroupNames.isEmpty()) {
                        String[] groupNameArray = currentCrafterGroupNames.toArray(String[]::new);
                        if(selectedCrafterGroupIndex >= groupNameArray.length && groupNameArray.length > 0) selectedCrafterGroupIndex = 0;
                        else if (groupNameArray.length == 0) selectedCrafterGroupIndex = 0;

                        
                        int newGroupIndex = ImGui.Combo("Select Category", selectedCrafterGroupIndex, groupNameArray);
                        if (newGroupIndex != selectedCrafterGroupIndex && newGroupIndex < currentCrafterGroupIds.size()) {
                            selectedCrafterGroupIndex = newGroupIndex;
                            int selectedGroupId = currentCrafterGroupIds.get(selectedCrafterGroupIndex);
                            currentCrafter.setSelectedGroupId(selectedGroupId); // Set group in crafter instance

                            currentCrafterProducts = new ArrayList<>(currentCrafter.getProductsForGroup(selectedGroupId));
                            selectedCrafterProductIndex = 0; // Reset product selection
                            

                            if (!currentCrafterProducts.isEmpty()) {
                                currentCrafter.setSelectedProduct(currentCrafterProducts.get(0));
                            } else {
                                currentCrafter.setSelectedProduct(null);
                            }
                            saveConfig();
                        }
                    } else {
                        ImGui.Text("No categories loaded for this Crafter option.");
                        
                    }

                    // Product Dropdown (for selected group)
                    // Safety check/refresh: Ensure currentCrafterProducts matches selectedCrafterGroupIndex
                    if (!currentCrafterGroupIds.isEmpty() && selectedCrafterGroupIndex < currentCrafterGroupIds.size()) {
                         int currentSelectedGroupId = currentCrafterGroupIds.get(selectedCrafterGroupIndex);
                         // Only refresh if list seems out of sync (e.g., empty when it's not)
                         // Let's just update it based on the current selected group ID before drawing product list
                         currentCrafterProducts = new ArrayList<>(currentCrafter.getProductsForGroup(currentSelectedGroupId));
                         
                    } else {
                         
                         if (currentCrafterProducts.isEmpty()) { // Ensure it's clear if no group selected/valid
                              ImGui.Text("Select a valid category first.");
                              // Don't proceed to render empty product combo
                              
                         }
                    }

                    
                    if (!currentCrafterProducts.isEmpty()) {
                        String[] productNames = currentCrafterProducts.stream()
                                                                     .map(p -> (p != null && p.getName() != null) ? p.getName() : "Unnamed")
                                                                     .toArray(String[]::new);
                        if (selectedCrafterProductIndex >= productNames.length && productNames.length > 0) selectedCrafterProductIndex = 0;
                        else if (productNames.length == 0) selectedCrafterProductIndex = 0;

                        int newProductIndex = ImGui.Combo("Select Product", selectedCrafterProductIndex, productNames);
                        if (newProductIndex != selectedCrafterProductIndex && newProductIndex < currentCrafterProducts.size()) {
                            selectedCrafterProductIndex = newProductIndex;
                            Product chosenProduct = currentCrafterProducts.get(selectedCrafterProductIndex);
                            currentCrafter.setSelectedProduct(chosenProduct);
                            
                            saveConfig();
                        }
                    } else {
                        ImGui.Text("No products available for category: " + (currentCrafterGroupNames.isEmpty() ? "N/A" : currentCrafterGroupNames.get(selectedCrafterGroupIndex)).replace("%", "%%"));
                    }
                } else {
                     // --- Render Direct Product Mode UI for Crafter (e.g., Tan Leather IF it was direct) ---
                     // Now that Tan Leather is also faked as grouped, this path might be less used unless a true direct option exists.
                    

                    if (!currentCrafterProducts.isEmpty()) {
                        String[] productNames = currentCrafterProducts.stream()
                                                                     .map(p -> (p != null && p.getName() != null) ? p.getName() : "Unnamed")
                                                                     .toArray(String[]::new);
                        if (selectedCrafterProductIndex >= productNames.length && productNames.length > 0) selectedCrafterProductIndex = 0;
                        else if (productNames.length == 0) selectedCrafterProductIndex = 0;

                        int newProductIndex = ImGui.Combo("Select Product", selectedCrafterProductIndex, productNames);
                        if (newProductIndex != selectedCrafterProductIndex && newProductIndex < currentCrafterProducts.size()) {
                            selectedCrafterProductIndex = newProductIndex;
                            Product chosenProduct = currentCrafterProducts.get(selectedCrafterProductIndex);
                            currentCrafter.setSelectedProduct(chosenProduct);
                            
                            saveConfig();
                        }
                    } else {
                        ImGui.Text("No products available for action: " + (PortableCrafter.CRAFTER_OPTIONS.length > selectedCrafterOptionIndex ? PortableCrafter.CRAFTER_OPTIONS[selectedCrafterOptionIndex] : "Unknown Action").replace("%", "%%"));
                    }
                }
            } else {
                 ImGui.Text("Crafter selected, but no active crafter task found or task is of wrong type.");
            }
        } else if (portableTypes[selectedPortableTypeIndex] == PortableType.SAWMILL) {
            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() instanceof PortableSawmill) {
                PortableSawmill currentSawmill = (PortableSawmill) coaezUtility.getPortableTask().getActivePortable();
                String[] plankNames = Arrays.stream(sawmillPlanks).map(SawmillPlank::getDisplayName).toArray(String[]::new);

                if (selectedSawmillPlankIndex >= plankNames.length && plankNames.length > 0) selectedSawmillPlankIndex = 0;
                else if (plankNames.length == 0) selectedSawmillPlankIndex = 0; // Should not happen with enum

                int newPlankIndex = ImGui.Combo("Select Plank", selectedSawmillPlankIndex, plankNames);
                if (newPlankIndex != selectedSawmillPlankIndex && newPlankIndex < sawmillPlanks.length) {
                    selectedSawmillPlankIndex = newPlankIndex;
                    currentSawmill.setSelectedPlank(sawmillPlanks[selectedSawmillPlankIndex]);
                    saveConfig(); // Save selection
                }
            } else {
                ImGui.Text("Sawmill selected, but no active sawmill task found or task is of wrong type.");
            }
        } else {
            ImGui.Text("Portable selected, but no active portable task found or task is of wrong type.");
        }



        if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
            Portable currentPortable = coaezUtility.getPortableTask().getActivePortable();
            ImGui.Text("Active Portable: " + currentPortable.getType().getName().replace("%", "%%"));

            Product currentProd = null;
            SawmillPlank currentPlank = null;

            if (currentPortable instanceof PortableWorkbench) {
                currentProd = ((PortableWorkbench) currentPortable).getSelectedProduct();
            } else if (currentPortable instanceof PortableCrafter) {
                PortableCrafter pc = (PortableCrafter) currentPortable;
                currentProd = pc.getSelectedProduct();
                ImGui.Text("Selected Action: " + (pc.getInteractionOption() != null ? pc.getInteractionOption() : "None").replace("%", "%%"));
                if (!pc.getGroupEnumIds().isEmpty() && pc.getSelectedGroupId() != -1) {
                     ImGui.Text("Selected Category: " + pc.getGroupName(pc.getSelectedGroupId()).replace("%", "%%"));
                }
            } else if (currentPortable instanceof PortableSawmill) {
                currentPlank = ((PortableSawmill) currentPortable).getSelectedPlank();
            }

            // Simplified Product Display
            if (currentProd != null) {
                ImGui.Text("Selected Product: " + currentProd.getName().replace("%", "%%"));

                // Restore Ingredient Display
                ImGui.Text("Required Ingredients:");
                List<Ingredient> ingredients = currentProd.getIngredients(); // Get ingredients
                
                if (ingredients != null && !ingredients.isEmpty()) {
                    for (Ingredient ingredient : ingredients) {
                         if (ingredient != null) { // Added null check for safety
                            ImGui.Text(String.format("- %d x %s", ingredient.getAmount(), ingredient.getDisplayName())); // Fixed: Use getDisplayName()
                         } else {
                            ImGui.Text("- (null ingredient)");
                         }
                    }
                } else {
                     ImGui.Text("- (No ingredient data found)");
                }
            } else if (currentPlank != null) {
                ImGui.Text("Selected Plank: " + currentPlank.getDisplayName().replace("%", "%%"));
            } else {
                // Display only if it's a type that *should* have a product/plank
                if (currentPortable instanceof PortableWorkbench || currentPortable instanceof PortableCrafter || currentPortable instanceof PortableSawmill) {
                    ImGui.Text("Selected Product/Plank: None");
                }
            }

        } else {
            ImGui.Text("Active Portable: None");
        }

        ImGui.Separator();
        
        // Start/Stop Portables button
        String portablesButtonText = (coaezUtility.getBotState() == CoaezUtility.BotState.PORTABLES) ? 
            "Stop Portables" : "Start Portables";
        if (ImGui.Button(portablesButtonText)) {
            if (coaezUtility.getBotState() == CoaezUtility.BotState.PORTABLES) {
                coaezUtility.setBotState(CoaezUtility.BotState.IDLE);
            } else if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
                coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
                saveConfig();
            }
        }
    }

    private void renderQuestsTab() {
        ImGui.Text("Quest Helper");
        ImGui.Separator();
        
        if (coaezUtility.getQuestHelper() == null) {
            ImGui.Text("Quest Helper not available.");
            return;
        }
        
        QuestHelper questHelper = coaezUtility.getQuestHelper();
        
        // Initialize quest display names if needed
        questHelper.initializeQuestDisplay();
        
        // Quest filtering options
        ImGui.Text("Filter Quests:");
        boolean filterChanged = false;
        
        boolean newShowCompleted = ImGui.Checkbox("Show Completed", questHelper.isShowCompletedQuests());
        if (newShowCompleted != questHelper.isShowCompletedQuests()) {
            questHelper.setShowCompletedQuests(newShowCompleted);
        }
        
        ImGui.SameLine();
        boolean newShowInProgress = ImGui.Checkbox("Show In Progress", questHelper.isShowInProgressQuests());
        if (newShowInProgress != questHelper.isShowInProgressQuests()) {
            questHelper.setShowInProgressQuests(newShowInProgress);
        }
        
        ImGui.SameLine();
        boolean newShowNotStarted = ImGui.Checkbox("Show Not Started", questHelper.isShowNotStartedQuests());
        if (newShowNotStarted != questHelper.isShowNotStartedQuests()) {
            questHelper.setShowNotStartedQuests(newShowNotStarted);
        }
        
        ImGui.SameLine();
        boolean newShowFreeToPlay = ImGui.Checkbox("Show Free-to-Play", questHelper.isShowFreeToPlayQuests());
        if (newShowFreeToPlay != questHelper.isShowFreeToPlayQuests()) {
            questHelper.setShowFreeToPlayQuests(newShowFreeToPlay);
        }
        
        ImGui.SameLine();
        boolean newShowMembers = ImGui.Checkbox("Show Members", questHelper.isShowMembersQuests());
        if (newShowMembers != questHelper.isShowMembersQuests()) {
            questHelper.setShowMembersQuests(newShowMembers);
        }
        
        // Quest search functionality
        ImGui.Separator();
        ImGui.Text("Search Quests:");
        String newQuestSearchText = ImGui.InputText("##QuestSearch", questHelper.getQuestSearchText());
        if (!newQuestSearchText.equals(questHelper.getQuestSearchText())) {
            questHelper.setQuestSearchText(newQuestSearchText);
        }
        
        ImGui.Separator();
        
        List<String> questDisplayNames = questHelper.getQuestDisplayNames();
        if (!questDisplayNames.isEmpty()) {
            String[] questNames = questDisplayNames.toArray(new String[0]);
            
            int selectedQuestIndex = questHelper.getSelectedQuestIndex();
            if (selectedQuestIndex >= questNames.length) {
                selectedQuestIndex = 0;
            }
            
            if (questNames.length == 1 && questHelper.getSelectedQuest() == null) {
                questHelper.selectQuestByIndex(0);
                selectedQuestIndex = 0;
            }
            
            int newSelectedQuestIndex = ImGui.Combo("Select Quest", selectedQuestIndex, questNames);
            if (newSelectedQuestIndex != selectedQuestIndex) {
                questHelper.selectQuestByIndex(newSelectedQuestIndex);
            }
        } else {
            ImGui.Text("No quests available with current filters.");
        }
        
        Quest selectedQuest = questHelper.getSelectedQuest();
        if (selectedQuest != null) {
            if (ImGui.Button("Show Quest ID Lookup")) {
                ScriptConsole.println("=== QUEST ID LOOKUP DEBUG ===");
                ScriptConsole.println("Selected quest: " + selectedQuest.name());
                questHelper.setSelectedQuest(selectedQuest);
                ScriptConsole.println("=== END QUEST ID LOOKUP DEBUG ===");
            }
            
            ImGui.SameLine();
            if (ImGui.Button("Fetch Dialog Options")) {
                questHelper.fetchAndPrintQuestDialogs();
            }
            
            ImGui.SameLine();
            if (questHelper.isDialogAssistanceActive()) {
                if (ImGui.Button("Disable Dialog Assistance")) {
                    questHelper.disableDialogAssistance();
                }
            } else {
                if (ImGui.Button("Enable Dialog Assistance")) {
                    questHelper.enableDialogAssistance();
                }
            }
            
            // Auto-interaction toggle
            if (questHelper.isDialogAssistanceActive()) {
                ImGui.SameLine();
                boolean newAutoInteract = ImGui.Checkbox("Auto-interact", questHelper.isAutoInteractWithDialogs());
                if (newAutoInteract != questHelper.isAutoInteractWithDialogs()) {
                    questHelper.setAutoInteractWithDialogs(newAutoInteract);
                }
            }
            
            // Navigation button
            ImGui.SameLine();
            if (questHelper.isNavigatingToQuestStart()) {
                if (ImGui.Button("Stop Navigation")) {
                    questHelper.stopNavigationToQuestStart();
                }
            } else {
                if (ImGui.Button("Navigate to Quest Start")) {
                    questHelper.startNavigationToQuestStart();
                }
            }
        }
        
        ImGui.Separator();
        
        // Quest information display
        Quest selectedQuestInfo = questHelper.getSelectedQuest();
        if (selectedQuestInfo != null) {
            // Dialog assistance status
            if (questHelper.isDialogAssistanceActive()) {
                ImGui.PushStyleColor(0, 0.0f, 1.0f, 0.0f, 1.0f); // Green color
                ImGui.Text("Dialog Assistance: ACTIVE");
                ImGui.PopStyleColor();
                
                if (questHelper.isAutoInteractWithDialogs()) {
                    ImGui.PushStyleColor(0, 1.0f, 0.5f, 0.0f, 1.0f); // Orange color
                    ImGui.Text("Auto-Interaction: ENABLED");
                    ImGui.PopStyleColor();
                } else {
                    ImGui.Text("Auto-Interaction: Disabled");
                }
                
                if (questHelper.areDialogsFetched()) {
                    ImGui.Text("Dialogs Status: Loaded");
                    List<String> dialogs = questHelper.getFetchedDialogs();
                    ImGui.Text("Dialog Options: " + dialogs.size());
                } else {
                    ImGui.Text("Dialogs Status: Loading...");
                }
            } else {
                ImGui.Text("Dialog Assistance: Inactive");
            }
            
            // Navigation status
            if (questHelper.isNavigatingToQuestStart()) {
                ImGui.PushStyleColor(0, 0.0f, 0.8f, 1.0f, 1.0f); // Blue color
                ImGui.Text("Navigation: ACTIVE");
                ImGui.PopStyleColor();
                
                Coordinate startCoord = questHelper.getQuestStartCoordinate();
                if (startCoord != null) {
                    ImGui.Text("Target Location: (" + startCoord.getX() + ", " + startCoord.getY() + ", " + startCoord.getZ() + ")");
                }
            } else {
                ImGui.Text("Navigation: Inactive");
            }
            
            ImGui.Separator();
            
            // Use comprehensive quest information that handles missing data gracefully
            String comprehensiveInfo = questHelper.getComprehensiveQuestInfo();
            String[] lines = comprehensiveInfo.split("\n");
            for (String line : lines) {
                ImGui.Text(line);
            }
        } else {
            ImGui.Text("No quest selected.");
        }
        
        ImGui.Separator();
        
        // Start/Stop Quest Helper button
        String questButtonText = (coaezUtility.getBotState() == CoaezUtility.BotState.QUESTS) ? 
            "Stop Quest Helper" : "Start Quest Helper";
        if (ImGui.Button(questButtonText)) {
            if (coaezUtility.getBotState() == CoaezUtility.BotState.QUESTS) {
                coaezUtility.setBotState(CoaezUtility.BotState.IDLE);
            } else {
                coaezUtility.setBotState(CoaezUtility.BotState.QUESTS);
                if (coaezUtility.getQuestHelper() != null) {
                    coaezUtility.getQuestHelper().initializeQuestDisplay();
                }
            }
        }
    }

    private void updateActivePortableType() {
        
        if (coaezUtility.getPortableTask() == null) {
            
            return;
        }

        PortableType selectedType = portableTypes[selectedPortableTypeIndex];
        

        if (selectedType != PortableType.WORKBENCH && selectedType != PortableType.CRAFTER && selectedType != PortableType.SAWMILL) {
            currentGroupIds.clear();
            currentGroupNames.clear();
            currentWorkbenchProducts.clear();
            selectedGroupIndex = 0;
            selectedWorkbenchProductIndex = 0;
            coaezUtility.getPortableTask().setActivePortable(null);
            coaezUtility.getPortableTask().setSelectedProduct(null);
        }

        switch (selectedType) {
            case WORKBENCH:
                
                PortableWorkbench wb = new PortableWorkbench(coaezUtility);
                coaezUtility.getPortableTask().setActivePortable(wb);

                this.currentGroupIds = wb.getGroupEnumIds();
                if (this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                    this.currentGroupNames = this.currentGroupIds.stream()
                                                .map(wb::getGroupName)
                                                .collect(Collectors.toList());
                    
                } else {
                    
                    this.currentGroupNames.clear();
                }

                String savedProductIdStr = coaezUtility.getConfig().getProperty("workbenchProductId");
                int lastSelectedProductId = -1;
                boolean productRestored = false;

                if (savedProductIdStr != null && !savedProductIdStr.isEmpty()) {
                    try {
                        lastSelectedProductId = Integer.parseInt(savedProductIdStr);
                         
                    } catch (NumberFormatException e) {
                        // ScriptConsole.println("[GUI] Error parsing saved workbenchProductId: " + savedProductIdStr);
                        lastSelectedProductId = -1;
                    }
                }

                if (lastSelectedProductId != -1 && this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                    for (int gIdx = 0; gIdx < this.currentGroupIds.size(); gIdx++) {
                        int groupId = this.currentGroupIds.get(gIdx);
                        List<Product> productsInGroup = wb.getProductsForGroup(groupId);
                        if (productsInGroup == null) continue;

                        for (int pIdx = 0; pIdx < productsInGroup.size(); pIdx++) {
                            Product p = productsInGroup.get(pIdx);
                            if (p != null && p.getId() == lastSelectedProductId) {
                                selectedGroupIndex = gIdx;
                                selectedWorkbenchProductIndex = pIdx;
                                currentWorkbenchProducts = new ArrayList<>(productsInGroup);
                                coaezUtility.getPortableTask().setSelectedProduct(p);
                                productRestored = true;
                                
                                break;
                            }
                        }
                        if (productRestored) break;
                    }
                }

                if (!productRestored) {
                    
                    selectedGroupIndex = 0;
                    selectedWorkbenchProductIndex = 0;
                    if (this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                         int defaultGroupId = this.currentGroupIds.get(0);
                         currentWorkbenchProducts = new ArrayList<>(wb.getProductsForGroup(defaultGroupId));
                         if (!currentWorkbenchProducts.isEmpty()) {
                             coaezUtility.getPortableTask().setSelectedProduct(currentWorkbenchProducts.get(0));
                              
                         } else {
                             coaezUtility.getPortableTask().setSelectedProduct(null);
                             currentWorkbenchProducts.clear();
                              
                         }
                     } else {
                          coaezUtility.getPortableTask().setSelectedProduct(null);
                          currentWorkbenchProducts.clear(); // Safe to clear
                          if (this.currentGroupNames != null) this.currentGroupNames.clear(); else this.currentGroupNames = new ArrayList<>();
                          
                     }
                 }
                 break;

             case BRAZIER:
             case FLETCHER:
             case RANGE:
             case WELL:
                 Portable simplePortable = new SimplePortable(coaezUtility, selectedType);
                 coaezUtility.getPortableTask().setActivePortable(simplePortable);
                 coaezUtility.getPortableTask().setSelectedProduct(null);
                 // Clear workbench state
                currentGroupIds.clear();
                currentGroupNames.clear();
                currentWorkbenchProducts.clear();
                selectedGroupIndex = 0;
                selectedWorkbenchProductIndex = 0;
                // Clear crafter state
                currentCrafterGroupIds.clear();
                currentCrafterGroupNames.clear();
                currentCrafterProducts.clear();
                selectedCrafterOptionIndex = 0;
                selectedCrafterGroupIndex = 0;
                selectedCrafterProductIndex = 0;
                // Clear sawmill state (selectedSawmillPlankIndex is fine, used by GUI)
                 break;
            case SAWMILL:
                PortableSawmill sawmill = new PortableSawmill(coaezUtility);
                coaezUtility.getPortableTask().setActivePortable(sawmill);
                coaezUtility.getPortableTask().setSelectedProduct(null); // Sawmill doesn't use general Product

                // Clear workbench state
                currentGroupIds.clear();
                currentGroupNames.clear();
                currentWorkbenchProducts.clear();
                selectedGroupIndex = 0;
                selectedWorkbenchProductIndex = 0;
                // Clear crafter state
                currentCrafterGroupIds.clear();
                currentCrafterGroupNames.clear();
                currentCrafterProducts.clear();
                selectedCrafterOptionIndex = 0;
                selectedCrafterGroupIndex = 0;
                selectedCrafterProductIndex = 0;
                // selectedSawmillPlankIndex will be updated by loadConfig or user interaction
                break;
            default:
                 // ScriptConsole.println("[GUI] Unknown portable type selected in update: " + selectedType);
                  coaezUtility.getPortableTask().setActivePortable(null);
                  coaezUtility.getPortableTask().setSelectedProduct(null);
                break;
        }

        // Separate logic block for CRAFTER type
        if (selectedType == PortableType.CRAFTER) {
            PortableCrafter pc_instance = null;
            try {
                pc_instance = new PortableCrafter(coaezUtility);
            } catch (Throwable t) {
                // ScriptConsole.println("[GUI|UpdateActivePortableType|CRAFTER] CRITICAL EXCEPTION during PortableCrafter constructor: " + t.getMessage());
                t.printStackTrace();
                this.currentCrafterGroupIds.clear();
                this.currentCrafterGroupNames.clear();
                this.currentCrafterProducts.clear();
                this.selectedCrafterOptionIndex = 0;
                this.selectedCrafterGroupIndex = 0;
                this.selectedCrafterProductIndex = 0;
                if (coaezUtility.getPortableTask() != null) {
                      coaezUtility.getPortableTask().setActivePortable(null);
                      coaezUtility.getPortableTask().setSelectedProduct(null);
                 }
                 // Clear workbench state if crafter fails
                currentGroupIds.clear();
                currentGroupNames.clear();
                currentWorkbenchProducts.clear();
                selectedGroupIndex = 0;
                selectedWorkbenchProductIndex = 0;
            } // End catch

            if (pc_instance != null && coaezUtility.getPortableTask() != null) {
                coaezUtility.getPortableTask().setActivePortable(pc_instance);
                this.selectedCrafterOptionIndex = 0; 
                if (PortableCrafter.CRAFTER_OPTIONS != null && PortableCrafter.CRAFTER_OPTIONS.length > 0) {
                    String defaultOptionName = PortableCrafter.CRAFTER_OPTIONS[this.selectedCrafterOptionIndex];
                    pc_instance.setSelectedInteractionOption(defaultOptionName); 
                    this.currentCrafterGroupIds = pc_instance.getGroupEnumIds();
                    if (!this.currentCrafterGroupIds.isEmpty()) { 
                        this.selectedCrafterGroupIndex = 0;
                        if (!this.currentCrafterGroupIds.isEmpty()) { 
                            int defaultGroupId = this.currentCrafterGroupIds.get(this.selectedCrafterGroupIndex);
                            pc_instance.setSelectedGroupId(defaultGroupId); 
                            this.currentCrafterGroupNames = this.currentCrafterGroupIds.stream()
                                                                .map(pc_instance::getGroupName)
                                                                .collect(Collectors.toList());
                            this.currentCrafterProducts = new ArrayList<>(pc_instance.getProductsForGroup(defaultGroupId));
                            this.selectedCrafterProductIndex = 0;
                        } else { 
                             this.currentCrafterGroupNames.clear();
                             this.currentCrafterProducts.clear();
                             this.selectedCrafterProductIndex = 0;
                        }
                    } else { 
                        this.currentCrafterGroupNames.clear();
                        this.selectedCrafterGroupIndex = 0;
                        pc_instance.setSelectedGroupId(-1); 
                        this.currentCrafterProducts = new ArrayList<>(pc_instance.getDirectProducts());
                        this.selectedCrafterProductIndex = 0;
                    }
                    if (!this.currentCrafterProducts.isEmpty() && this.selectedCrafterProductIndex < this.currentCrafterProducts.size()) {
                        pc_instance.setSelectedProduct(this.currentCrafterProducts.get(this.selectedCrafterProductIndex));
                    } else {
                        pc_instance.setSelectedProduct(null);
                        this.selectedCrafterProductIndex = 0; 
                    }
                } else {
                    // ScriptConsole.println("[GUI|UpdateActivePortableType|CRAFTER] CRAFTER_OPTIONS is not available. Cannot initialize default option.");
                }
                 // Clear workbench state when Crafter is selected
                currentGroupIds.clear();
                currentGroupNames.clear();
                currentWorkbenchProducts.clear();
                selectedGroupIndex = 0;
                selectedWorkbenchProductIndex = 0;
            } // End if pc_instance != null
        } // End if selectedType == PortableType.CRAFTER

        saveConfig(); // This should be the last call before the method ends
    } // End of updateActivePortableType method


    private void saveConfig() {
        if (coaezUtility == null || coaezUtility.getConfig() == null) return;
        if (isLoadingConfig) return;
        ScriptConfig config = coaezUtility.getConfig();

        config.addProperty("botState", coaezUtility.getBotState().name());

        if (coaezUtility.getAlchemy() != null) {
            List<String> alchemyItems = coaezUtility.getAlchemy().getAlchemyItems();
            config.addProperty("alchemyItems", String.join(",", alchemyItems));
        }

        if (coaezUtility.getDisassembly() != null) {
            List<String> disassemblyItems = coaezUtility.getDisassembly().getDisassemblyItems();
            config.addProperty("disassemblyItems", String.join(",", disassemblyItems));
        }

        if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
            Portable currentPortable = coaezUtility.getPortableTask().getActivePortable();
            config.addProperty("selectedPortableType", currentPortable.getType().name());

            if (currentPortable instanceof PortableWorkbench) {
                PortableWorkbench wb = (PortableWorkbench) currentPortable;
                if (wb.getSelectedProduct() != null) {
                    config.addProperty("workbenchProductId", String.valueOf(wb.getSelectedProduct().getId()));
                }
                // Removed saving workbenchSelectedGroupId - product ID restoration handles this implicitly.
                /* if (wb.getSelectedGroupId() != -1 && selectedGroupIndex < currentGroupIds.size() && selectedGroupIndex >= 0) { 
                    config.addProperty("workbenchSelectedGroupId", String.valueOf(currentGroupIds.get(selectedGroupIndex)));
                } */
            } else if (currentPortable instanceof PortableCrafter) {
                PortableCrafter pc = (PortableCrafter) currentPortable;
                // Save the actual selected option string for robustness
                config.addProperty("crafterOptionString", pc.getSelectedGuiInteractionOption()); 
                
                if (pc.getSelectedProduct() != null) {
                    config.addProperty("crafterProductId", String.valueOf(pc.getSelectedProduct().getId()));
                }
                // Save the group ID directly from the PortableCrafter if a valid group is selected
                int groupIdToSave = pc.getSelectedGroupId();
                if (groupIdToSave != -1) {
                     config.addProperty("crafterSelectedGroupId", String.valueOf(groupIdToSave));
                } else {
                    // If no group selected (-1), remove the property to avoid loading issues
                    config.removeProperty("crafterSelectedGroupId"); 
                }
            }
        }
        // TODO: Save SmithingTask selections (selectedCategoryEnum.name() and selectedProduct.getId())

        // Save Quest filter settings into QuestHelper
        if (coaezUtility.getQuestHelper() != null) {
            QuestHelper questHelper = coaezUtility.getQuestHelper();
            config.addProperty("showCompletedQuests", String.valueOf(questHelper.isShowCompletedQuests()));
            config.addProperty("showInProgressQuests", String.valueOf(questHelper.isShowInProgressQuests()));
            config.addProperty("showNotStartedQuests", String.valueOf(questHelper.isShowNotStartedQuests()));
            config.addProperty("showFreeToPlayQuests", String.valueOf(questHelper.isShowFreeToPlayQuests()));
            config.addProperty("showMembersQuests", String.valueOf(questHelper.isShowMembersQuests()));
        }

        // Save selected quest
        if (coaezUtility.getQuestHelper() != null && coaezUtility.getQuestHelper().getSelectedQuest() != null) {
            Quest selectedQuest = coaezUtility.getQuestHelper().getSelectedQuest();
            int questId = getQuestId(selectedQuest);
            if (questId != -1) {
                config.addProperty("selectedQuestId", String.valueOf(questId));
            }
        }
        
        // Save Beach Event settings
        config.addProperty("selectedBeachActivityIndex", String.valueOf(selectedBeachActivityIndex));
        config.addProperty("beachUseCocktails", String.valueOf(beachUseCocktails));
        config.addProperty("beachFightClawdia", String.valueOf(beachFightClawdia));
        config.addProperty("beachUseSpotlight", String.valueOf(beachUseSpotlight));
        config.addProperty("beachUseBattleship", String.valueOf(beachUseBattleship));
        config.addProperty("beachIsWeekend", String.valueOf(beachIsWeekend));
        config.addProperty("beachSpotlightHappyHour", beachSpotlightHappyHour);
        config.addProperty("selectedSpotlightHappyHourIndex", String.valueOf(selectedSpotlightHappyHourIndex));
        
        // Save individual cocktail settings
        config.addProperty("guiUsePinkFizz", String.valueOf(guiUsePinkFizz));
        config.addProperty("guiUsePurpleLumbridge", String.valueOf(guiUsePurpleLumbridge));
        config.addProperty("guiUsePineappletini", String.valueOf(guiUsePineappletini));
        config.addProperty("guiUseLemonSour", String.valueOf(guiUseLemonSour));
        config.addProperty("guiUseFishermanssFriend", String.valueOf(guiUseFishermanssFriend));
        config.addProperty("guiUseGeorgesPeachDelight", String.valueOf(guiUseGeorgesPeachDelight));
        config.addProperty("guiUseAHoleInOne", String.valueOf(guiUseAHoleInOne));
        config.addProperty("guiUsePalmerFarmer", String.valueOf(guiUsePalmerFarmer));
        config.addProperty("guiUseUglyDuckling", String.valueOf(guiUseUglyDuckling));

        // Save Sorceress Garden selection
        Set<GardenType> sgSelected = coaezUtility.getSorceressGardenTask() != null ? coaezUtility.getSorceressGardenTask().getSelectedGardens() : new HashSet<>();
        config.addProperty("sg_winterGardenSelected", String.valueOf(sgSelected.contains(GardenType.WINTER)));
        config.addProperty("sg_springGardenSelected", String.valueOf(sgSelected.contains(GardenType.SPRING)));
        config.addProperty("sg_summerGardenSelected", String.valueOf(sgSelected.contains(GardenType.SUMMER)));
        config.addProperty("sg_autumnGardenSelected", String.valueOf(sgSelected.contains(GardenType.AUTUMN)));

        config.save();
    }

    private void loadConfig() {
        if (coaezUtility == null || coaezUtility.getConfig() == null) return;
        ScriptConfig config = coaezUtility.getConfig();

        isLoadingConfig = true;
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
        if (coaezUtility.getAlchemy() != null) {
            String alchemyItemsStr = config.getProperty("alchemyItems");
            if (alchemyItemsStr != null && !alchemyItemsStr.isEmpty()) {
                Arrays.stream(alchemyItemsStr.split(","))
                        .forEach(item -> coaezUtility.getAlchemy().addAlchemyItem(item));
            }
        }

        // Load Disassembly settings
        if (coaezUtility.getDisassembly() != null) {
            String disassemblyItemsStr = config.getProperty("disassemblyItems");
            if (disassemblyItemsStr != null && !disassemblyItemsStr.isEmpty()) {
                Arrays.stream(disassemblyItemsStr.split(","))
                        .forEach(item -> coaezUtility.getDisassembly().addDisassemblyItem(item));
            }
        }
        
        // Load Quest filter settings into QuestHelper
        if (coaezUtility.getQuestHelper() != null) {
            QuestHelper questHelper = coaezUtility.getQuestHelper();
            
            String showCompletedStr = config.getProperty("showCompletedQuests");
            if (showCompletedStr != null) {
                questHelper.setShowCompletedQuests(Boolean.parseBoolean(showCompletedStr));
            }
            
            String showInProgressStr = config.getProperty("showInProgressQuests");
            if (showInProgressStr != null) {
                questHelper.setShowInProgressQuests(Boolean.parseBoolean(showInProgressStr));
            }
            
            String showNotStartedStr = config.getProperty("showNotStartedQuests");
            if (showNotStartedStr != null) {
                questHelper.setShowNotStartedQuests(Boolean.parseBoolean(showNotStartedStr));
            }
            
            String showFreeToPlayStr = config.getProperty("showFreeToPlayQuests");
            if (showFreeToPlayStr != null) {
                questHelper.setShowFreeToPlayQuests(Boolean.parseBoolean(showFreeToPlayStr));
            }
            
            String showMembersStr = config.getProperty("showMembersQuests");
            if (showMembersStr != null) {
                questHelper.setShowMembersQuests(Boolean.parseBoolean(showMembersStr));
            }
        }

        String selectedPortableTypeName = config.getProperty("selectedPortableType");
        

        if (selectedPortableTypeName != null && coaezUtility.getPortableTask() != null) {
            try {
                PortableType type = PortableType.valueOf(selectedPortableTypeName);
                boolean typeSet = false;
                for (int i = 0; i < portableTypes.length; i++) {
                    if (portableTypes[i] == type) {
                        selectedPortableTypeIndex = i;
                        typeSet = true;
                        break;
                    }
                }
                if (typeSet) {
                    updateActivePortableType();
                } else {
                      // ScriptConsole.println("[GUI] Saved portable type name " + selectedPortableTypeName + " not found in PortableType enum. Defaulting.");
                     selectedPortableTypeIndex = 0;
                     updateActivePortableType();
                 }

                // ** Crafter State Restoration START **
                if (portableTypes[selectedPortableTypeIndex] == PortableType.CRAFTER && coaezUtility.getPortableTask().getActivePortable() instanceof PortableCrafter) {
                    PortableCrafter crafterInstance = (PortableCrafter) coaezUtility.getPortableTask().getActivePortable();
                    
                    // 1. Restore Crafter Option Index
                    String savedCrafterOptionIndexStr = config.getProperty("crafterOptionIndex");
                    int savedCrafterOptionIndex = 0; // Default to 0
                    if (savedCrafterOptionIndexStr != null) {
                        try { savedCrafterOptionIndex = Integer.parseInt(savedCrafterOptionIndexStr); } catch (NumberFormatException e) { /* Use default */ }
                    }
                    // Ensure index is valid
                    if (savedCrafterOptionIndex < 0 || savedCrafterOptionIndex >= PortableCrafter.CRAFTER_OPTIONS.length) {
                        savedCrafterOptionIndex = 0;
                    }

                    // Only update if the saved option is different from the one set by updateActivePortableType (which defaults to 0)
                    if (savedCrafterOptionIndex != selectedCrafterOptionIndex) {
                         selectedCrafterOptionIndex = savedCrafterOptionIndex;
                         // Set the option on the crafter instance to load correct groups/products
                         String chosenOption = PortableCrafter.CRAFTER_OPTIONS[selectedCrafterOptionIndex];
                         crafterInstance.setSelectedInteractionOption(chosenOption); 
                         // ScriptConsole.println("[GUI Load] Set Crafter option to index: " + selectedCrafterOptionIndex + " (" + chosenOption + ")");
                         // Refresh GUI lists based on the loaded option
                         currentCrafterGroupIds = crafterInstance.getGroupEnumIds();
                         currentCrafterGroupNames = currentCrafterGroupIds.stream().map(crafterInstance::getGroupName).collect(Collectors.toList());
                         // Determine if groups are used *after* setting the option
                         boolean usesGroupsNow = !currentCrafterGroupIds.isEmpty();
                         if(usesGroupsNow) {
                            currentCrafterProducts.clear(); // Clear products, will be loaded based on group
                         } else {
                            currentCrafterProducts = new ArrayList<>(crafterInstance.getDirectProducts()); // Load direct products
                         }
                         selectedCrafterGroupIndex = 0; // Reset group/product indices for the new option
                         selectedCrafterProductIndex = 0;
                    }
                    
                    // 2. Restore Crafter Group Index (only if the selected option uses groups)
                    currentCrafterGroupIds = crafterInstance.getGroupEnumIds(); // Re-fetch just in case
                    boolean usesGroups = !currentCrafterGroupIds.isEmpty();
                    int savedCrafterGroupId = -1;
                    int savedCrafterGroupIndex = -1; // The index within the *current* list

                    if (usesGroups) {
                        String savedCrafterGroupIdStr = config.getProperty("crafterSelectedGroupId");
                        if (savedCrafterGroupIdStr != null) {
                            try { savedCrafterGroupId = Integer.parseInt(savedCrafterGroupIdStr); } catch (NumberFormatException e) { savedCrafterGroupId = -1; }
                        }
                        
                        // Find the index of the saved Group ID in the current list for this option
                        for (int i = 0; i < currentCrafterGroupIds.size(); i++) {
                            if (currentCrafterGroupIds.get(i) == savedCrafterGroupId) {
                                savedCrafterGroupIndex = i;
                                break;
                            }
                        }
                        
                        if (savedCrafterGroupIndex != -1) {
                            selectedCrafterGroupIndex = savedCrafterGroupIndex;
                            crafterInstance.setSelectedGroupId(savedCrafterGroupId);
                            // ScriptConsole.println("[GUI Load] Set Crafter group to index: " + selectedCrafterGroupIndex + " (ID: " + savedCrafterGroupId + ")");
                            // Load products for the restored group
                            currentCrafterProducts = new ArrayList<>(crafterInstance.getProductsForGroup(savedCrafterGroupId));
                            selectedCrafterProductIndex = 0; // Reset product index for new group
                        } else {
                            // Saved group ID not found in current option's groups, use default group (index 0)
                            selectedCrafterGroupIndex = 0;
                            if (!currentCrafterGroupIds.isEmpty()) {
                                int defaultGroupId = currentCrafterGroupIds.get(0);
                                crafterInstance.setSelectedGroupId(defaultGroupId);
                                currentCrafterProducts = new ArrayList<>(crafterInstance.getProductsForGroup(defaultGroupId));
                            } else {
                                currentCrafterProducts.clear(); // Should not happen if usesGroups is true
                            }
                             selectedCrafterProductIndex = 0;
                        }
                    } else {
                        // Option does not use groups, ensure products are loaded if not already
                        if (currentCrafterProducts.isEmpty()) {
                            currentCrafterProducts = new ArrayList<>(crafterInstance.getDirectProducts());
                        }
                        selectedCrafterGroupIndex = 0; // Or -1, consistent with GUI rendering
                        selectedCrafterProductIndex = 0;
                    }

                    // 3. Restore Crafter Product Index
                    String savedCrafterProductIdStr = config.getProperty("crafterProductId");
                    int savedCrafterProductId = -1;
                    if (savedCrafterProductIdStr != null) {
                        try { savedCrafterProductId = Integer.parseInt(savedCrafterProductIdStr); } catch (NumberFormatException e) { /* Use default */ }
                    }
                    
                    int restoredProductIndex = -1;
                    if (savedCrafterProductId != -1 && currentCrafterProducts != null && !currentCrafterProducts.isEmpty()) {
                        for (int i = 0; i < currentCrafterProducts.size(); i++) {
                            Product p = currentCrafterProducts.get(i);
                            if (p != null && p.getId() == savedCrafterProductId) {
                                restoredProductIndex = i;
                                break;
                            }
                        }
                    }

                    if (restoredProductIndex != -1) {
                        selectedCrafterProductIndex = restoredProductIndex;
                        crafterInstance.setSelectedProduct(currentCrafterProducts.get(selectedCrafterProductIndex));
                        // ScriptConsole.println("[GUI Load] Restored Crafter product to index: " + selectedCrafterProductIndex + " (ID: " + savedCrafterProductId + ")");
                    } else {
                        // Select default product (index 0) if restoration failed or no ID saved
                        selectedCrafterProductIndex = 0;
                        if (currentCrafterProducts != null && !currentCrafterProducts.isEmpty()) {
                            crafterInstance.setSelectedProduct(currentCrafterProducts.get(0));
                             // ScriptConsole.println("[GUI Load] Setting default Crafter product (index 0). ID: " + currentCrafterProducts.get(0).getId());
                        } else {
                            crafterInstance.setSelectedProduct(null); // No products available
                             // ScriptConsole.println("[GUI Load] No Crafter products available for selected group/option.");
                        }
                    }
                } 
                // ** Crafter State Restoration END **

            } catch (IllegalArgumentException e) {
                 // ScriptConsole.println("[GUI] Failed to load portable type from config: " + selectedPortableTypeName + ". " + e.getMessage());
                 selectedPortableTypeIndex = 0;
                 updateActivePortableType();
             }
         } else if (coaezUtility.getPortableTask() != null) {
               selectedPortableTypeIndex = 0;
               updateActivePortableType();
          }
        // TODO: Load SmithingTask selections from config and update GUI state
        // (selectedSmithingCategoryIndex, selectedSmithingProductIndex, and call smithingTask.setSelected...)
        
        // Load Beach Event settings
        String selectedBeachActivityIndexStr = config.getProperty("selectedBeachActivityIndex");
        if (selectedBeachActivityIndexStr != null) {
            try {
                int index = Integer.parseInt(selectedBeachActivityIndexStr);
                if (index >= 0 && index < beachActivities.length) {
                    selectedBeachActivityIndex = index;
                }
            } catch (NumberFormatException e) {
                selectedBeachActivityIndex = 0;
            }
        }
        
        String beachUseCocktailsStr = config.getProperty("beachUseCocktails");
        if (beachUseCocktailsStr != null) {
            beachUseCocktails = Boolean.parseBoolean(beachUseCocktailsStr);
        }
        
        String beachFightClawdiaStr = config.getProperty("beachFightClawdia");
        if (beachFightClawdiaStr != null) {
            beachFightClawdia = Boolean.parseBoolean(beachFightClawdiaStr);
        }
        
        String beachUseSpotlightStr = config.getProperty("beachUseSpotlight");
        if (beachUseSpotlightStr != null) {
            beachUseSpotlight = Boolean.parseBoolean(beachUseSpotlightStr);
        }
        
        String beachUseBattleshipStr = config.getProperty("beachUseBattleship");
        if (beachUseBattleshipStr != null) {
            beachUseBattleship = Boolean.parseBoolean(beachUseBattleshipStr);
        }
        
        String beachIsWeekendStr = config.getProperty("beachIsWeekend");
        if (beachIsWeekendStr != null) {
            beachIsWeekend = Boolean.parseBoolean(beachIsWeekendStr);
        }

        String beachSpotlightHappyHourStr = config.getProperty("beachSpotlightHappyHour");
        if (beachSpotlightHappyHourStr != null) {
            beachSpotlightHappyHour = beachSpotlightHappyHourStr;
            // Update the index to match the loaded value
            for (int i = 0; i < spotlightHappyHourOptions.length; i++) {
                if (spotlightHappyHourOptions[i].equals(beachSpotlightHappyHour)) {
                    selectedSpotlightHappyHourIndex = i;
                    break;
                }
            }
        }
        
        String selectedSpotlightHappyHourIndexStr = config.getProperty("selectedSpotlightHappyHourIndex");
        if (selectedSpotlightHappyHourIndexStr != null) {
            try {
                int index = Integer.parseInt(selectedSpotlightHappyHourIndexStr);
                if (index >= 0 && index < spotlightHappyHourOptions.length) {
                    selectedSpotlightHappyHourIndex = index;
                    beachSpotlightHappyHour = spotlightHappyHourOptions[selectedSpotlightHappyHourIndex];
                }
            } catch (NumberFormatException e) {
                selectedSpotlightHappyHourIndex = 3; // Default to "Hunter"
            }
        }
        
        // Load individual cocktail settings
        String guiUsePinkFizzStr = config.getProperty("guiUsePinkFizz");
        if (guiUsePinkFizzStr != null) {
            guiUsePinkFizz = Boolean.parseBoolean(guiUsePinkFizzStr);
        }
        
        String guiUsePurpleLumbridgeStr = config.getProperty("guiUsePurpleLumbridge");
        if (guiUsePurpleLumbridgeStr != null) {
            guiUsePurpleLumbridge = Boolean.parseBoolean(guiUsePurpleLumbridgeStr);
        }
        
        String guiUsePineappletiniStr = config.getProperty("guiUsePineappletini");
        if (guiUsePineappletiniStr != null) {
            guiUsePineappletini = Boolean.parseBoolean(guiUsePineappletiniStr);
        }
        
        String guiUseLemonSourStr = config.getProperty("guiUseLemonSour");
        if (guiUseLemonSourStr != null) {
            guiUseLemonSour = Boolean.parseBoolean(guiUseLemonSourStr);
        }
        
        String guiUseFishermanssFriendStr = config.getProperty("guiUseFishermanssFriend");
        if (guiUseFishermanssFriendStr != null) {
            guiUseFishermanssFriend = Boolean.parseBoolean(guiUseFishermanssFriendStr);
        }
        
        String guiUseGeorgesPeachDelightStr = config.getProperty("guiUseGeorgesPeachDelight");
        if (guiUseGeorgesPeachDelightStr != null) {
            guiUseGeorgesPeachDelight = Boolean.parseBoolean(guiUseGeorgesPeachDelightStr);
        }
        
        String guiUseAHoleInOneStr = config.getProperty("guiUseAHoleInOne");
        if (guiUseAHoleInOneStr != null) {
            guiUseAHoleInOne = Boolean.parseBoolean(guiUseAHoleInOneStr);
        }
        
        String guiUsePalmerFarmerStr = config.getProperty("guiUsePalmerFarmer");
        if (guiUsePalmerFarmerStr != null) {
            guiUsePalmerFarmer = Boolean.parseBoolean(guiUsePalmerFarmerStr);
        }
        
        String guiUseUglyDucklingStr = config.getProperty("guiUseUglyDuckling");
        if (guiUseUglyDucklingStr != null) {
            guiUseUglyDuckling = Boolean.parseBoolean(guiUseUglyDucklingStr);
        }
        
        updateBeachEventSettings();
        
        // Load Sorceress Garden selection
        Set<GardenType> loadedSG = new HashSet<>();
        String sgWinter = config.getProperty("sg_winterGardenSelected");
        if (sgWinter != null && Boolean.parseBoolean(sgWinter)) loadedSG.add(GardenType.WINTER);
        String sgSpring = config.getProperty("sg_springGardenSelected");
        if (sgSpring != null && Boolean.parseBoolean(sgSpring)) loadedSG.add(GardenType.SPRING);
        String sgSummer = config.getProperty("sg_summerGardenSelected");
        if (sgSummer != null && Boolean.parseBoolean(sgSummer)) loadedSG.add(GardenType.SUMMER);
        String sgAutumn = config.getProperty("sg_autumnGardenSelected");
        if (sgAutumn != null && Boolean.parseBoolean(sgAutumn)) loadedSG.add(GardenType.AUTUMN);
        if (coaezUtility.getSorceressGardenTask() != null) coaezUtility.getSorceressGardenTask().setSelectedGardens(loadedSG);
        
        isLoadingConfig = false;
     }

    @Override
    public void drawOverlay() {
        if (coaezUtility.getBotState() == CoaezUtility.BotState.QUESTS) {
            QuestHelper questHelper = coaezUtility.getQuestHelper();
            if (questHelper != null) {
                drawDialogAssistanceOverlay(questHelper);
                
                if (questHelper.hasValidOverlayCoordinates()) {
                    drawDialogOptionHighlight(questHelper);
                }
                
                drawMarkNextStepWindow(questHelper);
            }
        }
    }
    
    /**
     * Draws a simple window containing only the "Mark Next Step Complete" button.
     * This window can be easily positioned anywhere on screen.
     */
    private void drawMarkNextStepWindow(QuestHelper questHelper) {
        if (!questHelper.isDialogAssistanceActive() || questHelper.getSelectedQuest() == null) {
            return;
        }
        
        try {
            guiStyling.applyCustomColors();
            guiStyling.applyCustomStyles();
            
            if (ImGui.Begin("Quick Actions", 0)) {
                if (ImGui.Button("Mark Next Step Complete")) {
                    int[] firstIncompleteStep = questHelper.getFirstIncompleteStepIndex();
                    if (firstIncompleteStep != null && firstIncompleteStep.length == 2) {
                        int sectionIndex = firstIncompleteStep[0];
                        int stepIndex = firstIncompleteStep[1];
                        
                        questHelper.setStepCompleted(sectionIndex, stepIndex, true);
                        
                        // ScriptConsole.println("[QuestHelper] Marked step " + sectionIndex + ":" + stepIndex + " as completed");
                    } else {
                        // ScriptConsole.println("[QuestHelper] No incomplete steps found or all steps completed");
                    }
                }
                
                if (ImGui.Button("Undo Last Step")) {
                    QuestDialogFetcher.QuestGuide currentGuide = questHelper.getCurrentQuestGuide();
                    if (currentGuide != null && !currentGuide.getSections().isEmpty()) {
                        boolean foundCompletedStep = false;
                        
                        for (int sIdx = currentGuide.getSections().size() - 1; sIdx >= 0 && !foundCompletedStep; sIdx--) {
                            QuestDialogFetcher.QuestSection section = currentGuide.getSections().get(sIdx);
                            
                            for (int stIdx = section.getSteps().size() - 1; stIdx >= 0; stIdx--) {
                                if (questHelper.isStepCompleted(sIdx, stIdx)) {
                                    questHelper.setStepCompleted(sIdx, stIdx, false);
                                    // ScriptConsole.println("[QuestHelper] Undid step " + sIdx + ":" + stIdx);
                                    foundCompletedStep = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!foundCompletedStep) {
                            // ScriptConsole.println("[QuestHelper] No completed steps to undo");
                        }
                    } else {
                        // ScriptConsole.println("[QuestHelper] No quest guide available");
                    }
                }
                
                ImGui.End();
            }
        } finally {
            guiStyling.resetCustomStyles();
            guiStyling.resetCustomColors();
        }
    }
    
    /**
     * Draws the dialog assistance overlay when active.
     */
    private void drawDialogAssistanceOverlay(QuestHelper questHelper) {
        // ScriptConsole.println("[CoaezUtilityGUI] drawDialogAssistanceOverlay called");
        
        Quest selectedQuest = questHelper.getSelectedQuest();
        if (selectedQuest == null) {
            return;
        }
        
        // Get the current quest guide
        QuestDialogFetcher.QuestGuide currentGuide = questHelper.getCurrentQuestGuide();
        
        // Show overlay if we have dialog assistance active
        if (questHelper.isDialogAssistanceActive()) {
            try {
                // Apply the same styling as main GUI for consistency
                guiStyling.applyCustomColors();
                guiStyling.applyCustomStyles();
                
                // Apply window background transparency (override the styling)
                ImGui.PushStyleColor(ImGuiCol.WindowBg, 0.0f, 0.0f, 0.0f, overlayTransparency);
                                
                // Window flags: Allow moving and resizing
                int windowFlags = ImGuiWindowFlag.NoCollapse.getValue() |
                                 ImGuiWindowFlag.NoScrollbar.getValue() |
                                 ImGuiWindowFlag.NoScrollWithMouse.getValue() |
                                 ImGuiWindowFlag.NoFocusOnAppearing.getValue() |
                                 ImGuiWindowFlag.NoBringToFrontOnFocus.getValue();
                
                if (ImGui.Begin("Quest Dialog Assistant", windowFlags)) {
                    // Transparency slider
                    int transparencyPercent = (int)(overlayTransparency * 100);
                    int newTransparencyPercent = ImGui.Slider("Transparency", transparencyPercent, 10, 100, 1);
                    
                    // Update transparency if slider changed
                    if (newTransparencyPercent != transparencyPercent) {
                        overlayTransparency = newTransparencyPercent / 100.0f;
                        // Update the window background color immediately
                        ImGui.PopStyleColor(); // Pop the old window background
                        ImGui.PushStyleColor(ImGuiCol.WindowBg, 0.0f, 0.0f, 0.0f, overlayTransparency);
                    }
                    
                    ImGui.Separator();
                    
                    ImGui.PushStyleColor(0, 0.25f, 0.78f, 0.71f, 1.0f); // Teal accent
                    ImGui.Text("Quest: " + selectedQuest.name().replace("%", "%%"));
                    ImGui.PopStyleColor();
                    
                    try {
                        int questProgress = selectedQuest.progress();
                        ImGui.PushStyleColor(0, 0.4f, 0.9f, 0.7f, 1.0f); // Lighter teal
                        ImGui.Text("Progress: " + questProgress);
                        ImGui.PopStyleColor();
                        
                        String status;
                        float[] statusColor;
                        if (selectedQuest.isComplete()) {
                            status = "Completed";
                            statusColor = new float[]{0.2f, 0.8f, 0.4f, 1.0f}; // Green
                        } else if (selectedQuest.isStarted()) {
                            status = "In Progress";
                            statusColor = new float[]{1.0f, 0.8f, 0.2f, 1.0f}; // Yellow
                        } else {
                            status = "Not Started";
                            statusColor = new float[]{0.8f, 0.4f, 0.2f, 1.0f}; // Orange
                        }
                        
                        ImGui.PushStyleColor(0, statusColor[0], statusColor[1], statusColor[2], statusColor[3]);
                        ImGui.Text("Status: " + status.replace("%", "%%"));
                        ImGui.PopStyleColor();
                        
                    } catch (Exception e) {
                        ImGui.PushStyleColor(0, 0.8f, 0.4f, 0.4f, 1.0f); // Red
                        ImGui.Text("Progress: Unable to determine");
                        ImGui.PopStyleColor();
                    }
                    
                    ImGui.Separator();
                    
                    // Display quest guide sections and steps
                    if (currentGuide != null && !currentGuide.getSections().isEmpty()) {
                        ImGui.PushStyleColor(0, 0.25f, 0.78f, 0.71f, 1.0f); // Teal accent
                        ImGui.Text("Quest Guide Progress: " + currentGuide.getCompletedSteps() + "/" + currentGuide.getTotalSteps() + " steps");
                        ImGui.PopStyleColor();
                        
                        // Add control buttons
                        if (ImGui.Button("Reset All Steps")) {
                            questHelper.resetAllSteps();
                        }
                        
                        ImGui.SameLine();
                        if (ImGui.Button("Mark All Complete")) {
                            for (int sIdx = 0; sIdx < currentGuide.getSections().size(); sIdx++) {
                                QuestDialogFetcher.QuestSection section = currentGuide.getSections().get(sIdx);
                                for (int stIdx = 0; stIdx < section.getSteps().size(); stIdx++) {
                                    questHelper.setStepCompleted(sIdx, stIdx, true);
                                }
                            }
                        }
                        
                        ImGui.Separator();
                        
                        float baseWindowHeight = 800.0f; // Increased base window size assumption
                        float headerHeight = 140.0f; // Space for quest info, buttons, and progress
                        float windowPadding = 30.0f; // Account for window padding and margins
                        float recommendationHeight = 80.0f; // Space reserved for recommendation text below
                        float availableHeight = Math.max(300.0f, baseWindowHeight - headerHeight - windowPadding - recommendationHeight);
                        
                        if (ImGui.ListBoxHeader("Quest Steps", 0, (int)availableHeight)) {
                            for (int sectionIndex = 0; sectionIndex < currentGuide.getSections().size(); sectionIndex++) {
                                QuestDialogFetcher.QuestSection section = currentGuide.getSections().get(sectionIndex);
                                
                                ImGui.PushStyleColor(0, 0.4f, 0.9f, 0.7f, 1.0f); // Lighter teal
                                ImGui.Text("=== " + section.getSectionName().replace("%", "%%") + " (" + section.getCompletedSteps() + "/" + section.getTotalSteps() + ") ===");
                                ImGui.PopStyleColor();
                                
                                for (int stepIndex = 0; stepIndex < section.getSteps().size(); stepIndex++) {
                                    QuestDialogFetcher.QuestStep step = section.getSteps().get(stepIndex);
                                    
                                    String stepId = sectionIndex + ":" + stepIndex;
                                    boolean isCompleted = questHelper.isStepCompleted(sectionIndex, stepIndex);
                                    
                                    ImGui.PushID(stepId);
                                    boolean newCompletionStatus = ImGui.Checkbox("", isCompleted);
                                    if (newCompletionStatus != isCompleted) {
                                        questHelper.setStepCompleted(sectionIndex, stepIndex, newCompletionStatus);
                                    }
                                    ImGui.PopID();
                                    
                                    ImGui.SameLine();
                                    
                                    if (isCompleted) {
                                        ImGui.PushStyleColor(0, 0.2f, 0.8f, 0.4f, 1.0f); // Green-teal for completed
                                        ImGui.Text(step.getCleanStepText().replace("%", "%%"));
                                        ImGui.PopStyleColor();
                                    } else {
                                        ImGui.PushStyleColor(0, 1.0f, 0.8f, 0.2f, 1.0f); // Yellow for incomplete
                                        ImGui.Text(step.getCleanStepText().replace("%", "%%"));
                                        ImGui.PopStyleColor();
                                    }
                                    
                                    if (!isCompleted && step.hasDialogs()) {
                                        for (QuestDialogFetcher.DialogSequence sequence : step.getDialogs()) {
                                            ImGui.PushStyleColor(0, 0.7f, 0.7f, 0.7f, 1.0f); // Light gray
                                            ImGui.Text("    Dialog: " + sequence.getContext().replace("%", "%%"));
                                            ImGui.PopStyleColor();
                                            
                                            for (QuestDialogFetcher.DialogOption option : sequence.getOptions()) {
                                                ImGui.PushStyleColor(0, 0.8f, 0.9f, 1.0f, 1.0f); // Light blue
                                                ImGui.Text("      " + option.getOptionNumber() + ": " + option.getOptionText().replace("%", "%%"));
                                                ImGui.PopStyleColor();
                                            }
                                        }
                                    }
                                }
                                
                                if (sectionIndex < currentGuide.getSections().size() - 1) {
                                    ImGui.Separator();
                                }
                            }
                            
                            ImGui.ListBoxFooter();
                        }
                        
                    } else {
                        ImGui.PushStyleColor(0, 1.0f, 0.8f, 0.2f, 1.0f);
                        ImGui.Text("Loading quest guide...");
                        ImGui.PopStyleColor();
                    }
                    
                    ImGui.End();
                }
                
                ImGui.PopStyleColor();
                
            } catch (Exception e) {
                // ScriptConsole.println("[CoaezUtilityGUI] Error drawing overlay: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Reset styling
                guiStyling.resetCustomStyles();
                guiStyling.resetCustomColors();
            }
        }
    }
    
    /**
     * Draws a visual highlight overlay for the recommended dialog option.
     * @param questHelper The QuestHelper instance providing coordinates and option data
     */
    private void drawDialogOptionHighlight(QuestHelper questHelper) {
        try {
            int[] coordinates = questHelper.getDialogOverlayCoordinates();
            if (coordinates == null || coordinates.length != 4) {
                return;
            }
            
            int x = coordinates[0];
            int y = coordinates[1];
            int width = coordinates[2];
            int height = coordinates[3];
            String optionText = questHelper.getRecommendedOptionText();
            
            if (optionText == null) {
                return;
            }
            
            if (width <= 0) width = 300;
            if (height <= 0) height = 20;
            
            // Use different colors when auto-interaction is enabled
            int highlightColor;
            int borderColor;
            
            if (questHelper.isAutoInteractWithDialogs()) {
                // Green highlight when auto-interaction is enabled
                highlightColor = 0xCC00FF00;  // Semi-transparent green
                borderColor = 0xFF00CC00;      // Solid darker green
            } else {
                // Gold highlight when manual interaction is needed
                highlightColor = 0xCCFFD700;  // Semi-transparent gold
                borderColor = 0xFFFF8C00;      // Solid orange
            }
            
            BGList.DrawRect(x, y, x + width, y + height, highlightColor, 4.0f, 0, 0.0f);
            
            BGList.DrawRect(x, y, x + width, y + height, borderColor, 4.0f, 0, 2.0f);
            
            if (questHelper.isAutoInteractWithDialogs()) {
                int textX = x + width - 40;
                int textY = y + 2;
                BGList.DrawText("AUTO", (float)textX, (float)textY, 0xFF00FF00);
            }
            
        } catch (Exception e) {
            // ScriptConsole.println("[CoaezUtilityGUI] Error processing dialog option highlight: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to get a quest ID.
     * This is inefficient but necessary since Quest doesn't expose ID directly.
     */
    private int getQuestId(Quest quest) {
        for (int i = 0; i < 509; i++) {
            Optional<Quest> testQuest = Quest.byId(i);
            if (testQuest.isPresent() && testQuest.get().name().equals(quest.name())) {
                return i;
            }
        }
        return -1;
    }

    private void updateBeachEventSettings() {
        if (coaezUtility != null && coaezUtility.getBeachEventTask() != null) {
            BeachEventTask beachTask = coaezUtility.getBeachEventTask();
            
            // Apply loaded GUI settings to the task
            beachTask.setSelectedActivity(beachActivities[selectedBeachActivityIndex]);
            beachTask.setUseCocktails(beachUseCocktails);
            beachTask.setFightClawdia(beachFightClawdia);
            beachTask.setUseSpotlight(beachUseSpotlight);
            beachTask.setUseBattleship(beachUseBattleship);
            beachTask.setIsWeekend(beachIsWeekend);
            beachTask.setSpotlightHappyHour(beachSpotlightHappyHour);
            
            // Apply individual cocktail settings
            beachTask.setUsePinkFizz(guiUsePinkFizz);
            beachTask.setUsePurpleLumbridge(guiUsePurpleLumbridge);
            beachTask.setUsePineappletini(guiUsePineappletini);
            beachTask.setUseLemonSour(guiUseLemonSour);
            beachTask.setUseFishermanssFriend(guiUseFishermanssFriend);
            beachTask.setUseGeorgesPeachDelight(guiUseGeorgesPeachDelight);
            beachTask.setUseAHoleInOne(guiUseAHoleInOne);
            beachTask.setUsePalmerFarmer(guiUsePalmerFarmer);
            beachTask.setUseUglyDuckling(guiUseUglyDuckling);
            
            ScriptConsole.println("[GUI] Applied Beach Event settings from config - Activity: " + 
                beachActivities[selectedBeachActivityIndex].getName() + 
                ", Cocktails: " + beachUseCocktails + 
                ", Clawdia: " + beachFightClawdia + 
                ", Spotlight: " + beachUseSpotlight + 
                ", Battleship: " + beachUseBattleship + 
                ", Happy Hour: " + beachSpotlightHappyHour);
        }
    }

    private void renderBeachEventTab() {
        ImGui.Text("Beach Event Configuration");
        ImGui.Separator();
        
        // Beach Activity Selection
        String[] beachActivityNames = Arrays.stream(beachActivities)
            .map(activity -> activity.getName())
            .toArray(String[]::new);


        int newBeachActivityIndex = ImGui.Combo("Select Beach Activity", selectedBeachActivityIndex, beachActivityNames);
        if (newBeachActivityIndex != selectedBeachActivityIndex) {
            selectedBeachActivityIndex = newBeachActivityIndex;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        ImGui.Separator();
        
        ImGui.Text("General Options:");
        
        boolean newBeachFightClawdia = ImGui.Checkbox("Fight Clawdia", beachFightClawdia);
        if (newBeachFightClawdia != beachFightClawdia) {
            beachFightClawdia = newBeachFightClawdia;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newBeachUseSpotlight = ImGui.Checkbox("Follow Spotlight", beachUseSpotlight);
        if (newBeachUseSpotlight != beachUseSpotlight) {
            beachUseSpotlight = newBeachUseSpotlight;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newBeachUseBattleship = ImGui.Checkbox("Use Battleship", beachUseBattleship);
        if (newBeachUseBattleship != beachUseBattleship) {
            beachUseBattleship = newBeachUseBattleship;
            updateBeachEventSettings(); 
            saveConfig();
        }

        boolean newBeachIsWeekend = ImGui.Checkbox("Is Weekend", beachIsWeekend);
        if (newBeachIsWeekend != beachIsWeekend) {
            beachIsWeekend = newBeachIsWeekend;
            updateBeachEventSettings();
            saveConfig();
        }

        if (beachUseSpotlight) {
            int newSpotlightHappyHourIndex = ImGui.Combo("Happy Hour Preference", selectedSpotlightHappyHourIndex, spotlightHappyHourOptions);
            if (newSpotlightHappyHourIndex != selectedSpotlightHappyHourIndex) {
                selectedSpotlightHappyHourIndex = newSpotlightHappyHourIndex;
                beachSpotlightHappyHour = spotlightHappyHourOptions[selectedSpotlightHappyHourIndex];
                updateBeachEventSettings(); 
                saveConfig();
            }
        }
        
        ImGui.Separator();
        
        ImGui.Text("Cocktail Selection:");
        
        boolean newGuiUsePinkFizz = ImGui.Checkbox("Pink Fizz (Strength)", guiUsePinkFizz);
        if (newGuiUsePinkFizz != guiUsePinkFizz) {
            guiUsePinkFizz = newGuiUsePinkFizz;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUsePurpleLumbridge = ImGui.Checkbox("Purple Lumbridge (Construction/Cooking)", guiUsePurpleLumbridge);
        if (newGuiUsePurpleLumbridge != guiUsePurpleLumbridge) {
            guiUsePurpleLumbridge = newGuiUsePurpleLumbridge;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUsePineappletini = ImGui.Checkbox("Pineappletini (Hunter/Farming/Fishing)", guiUsePineappletini);
        if (newGuiUsePineappletini != guiUsePineappletini) {
            guiUsePineappletini = newGuiUsePineappletini;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUseLemonSour = ImGui.Checkbox("Lemon Sour (Dungeoneering)", guiUseLemonSour);
        if (newGuiUseLemonSour != guiUseLemonSour) {
            guiUseLemonSour = newGuiUseLemonSour;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUseFishermanssFriend = ImGui.Checkbox("Fisherman's Friend (Fishing)", guiUseFishermanssFriend);
        if (newGuiUseFishermanssFriend != guiUseFishermanssFriend) {
            guiUseFishermanssFriend = newGuiUseFishermanssFriend;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUseGeorgesPeachDelight = ImGui.Checkbox("George's Peach Delight (Construction)", guiUseGeorgesPeachDelight);
        if (newGuiUseGeorgesPeachDelight != guiUseGeorgesPeachDelight) {
            guiUseGeorgesPeachDelight = newGuiUseGeorgesPeachDelight;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUseAHoleInOne = ImGui.Checkbox("A Hole in One (Dungeoneering)", guiUseAHoleInOne);
        if (newGuiUseAHoleInOne != guiUseAHoleInOne) {
            guiUseAHoleInOne = newGuiUseAHoleInOne;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUsePalmerFarmer = ImGui.Checkbox("Palmer Farmer (Farming)", guiUsePalmerFarmer);
        if (newGuiUsePalmerFarmer != guiUsePalmerFarmer) {
            guiUsePalmerFarmer = newGuiUsePalmerFarmer;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        boolean newGuiUseUglyDuckling = ImGui.Checkbox("Ugly Duckling (Hunter)", guiUseUglyDuckling);
        if (newGuiUseUglyDuckling != guiUseUglyDuckling) {
            guiUseUglyDuckling = newGuiUseUglyDuckling;
            updateBeachEventSettings(); 
            saveConfig();
        }
        
        ImGui.Separator();
        
        if (ImGui.Button("Start Beach Event")) {
            if (coaezUtility.getBeachEventTask() != null) {
                coaezUtility.setBotState(CoaezUtility.BotState.BEACH_EVENT);
            } else {
                ScriptConsole.println("[GUI] Beach Event Task not available.");
            }
        }
        
        // Display current beach event configuration
        ImGui.Separator();
        ImGui.Text("Current Configuration:");
        ImGui.Text("Activity: " + beachActivities[selectedBeachActivityIndex].getName().replace("%", "%%"));
        
        // Display selected cocktails
        StringBuilder cocktailsText = new StringBuilder("Cocktails: ");
        boolean hasSelectedCocktails = false;
        if (guiUsePinkFizz) { cocktailsText.append("Pink Fizz, "); hasSelectedCocktails = true; }
        if (guiUsePurpleLumbridge) { cocktailsText.append("Purple Lumbridge, "); hasSelectedCocktails = true; }
        if (guiUsePineappletini) { cocktailsText.append("Pineappletini, "); hasSelectedCocktails = true; }
        if (guiUseLemonSour) { cocktailsText.append("Lemon Sour, "); hasSelectedCocktails = true; }
        if (guiUseFishermanssFriend) { cocktailsText.append("Fisherman's Friend, "); hasSelectedCocktails = true; }
        if (guiUseGeorgesPeachDelight) { cocktailsText.append("George's Peach Delight, "); hasSelectedCocktails = true; }
        if (guiUseAHoleInOne) { cocktailsText.append("A Hole in One, "); hasSelectedCocktails = true; }
        if (guiUsePalmerFarmer) { cocktailsText.append("Palmer Farmer, "); hasSelectedCocktails = true; }
        if (guiUseUglyDuckling) { cocktailsText.append("Ugly Duckling, "); hasSelectedCocktails = true; }
        
        if (hasSelectedCocktails) {
            // Remove the trailing ", "
            String cocktailsList = cocktailsText.toString();
            cocktailsList = cocktailsList.substring(0, cocktailsList.length() - 2);
            ImGui.Text(cocktailsList.replace("%", "%%"));
        } else {
            ImGui.Text("Cocktails: None");
        }
        
        ImGui.Text("Fight Clawdia: " + (beachFightClawdia ? "Yes" : "No"));
        ImGui.Text("Follow Spotlight: " + (beachUseSpotlight ? "Yes" : "No"));
        if (beachUseSpotlight) {
            ImGui.Text("Happy Hour Preference: " + beachSpotlightHappyHour.replace("%", "%%"));
        }
        ImGui.Text("Use Battleship: " + (beachUseBattleship ? "Yes" : "No"));
    }

    private void renderMapNavigatorTab() {
        if (coaezUtility == null) return;
        
        MapNavigatorTask mapTask = coaezUtility.getMapNavigatorTask();
        if (mapTask == null) return;
        
        ImGui.Text("Location Navigator");
        ImGui.Separator();
        
        // Get available locations
        if (mapTask.getLocationManager().isInitialized()) {
            String[] availableLocations = mapTask.getAvailableLocationNames();
            
            if (availableLocations.length > 0) {
                // Location selection combo
                ImGui.Text("Select Location:");
                int newLocationIndex = ImGui.Combo("##LocationCombo", selectedLocationIndex, availableLocations);
                if (newLocationIndex != selectedLocationIndex) {
                    selectedLocationIndex = newLocationIndex;
                    ScriptConsole.println("Location selected: " + availableLocations[selectedLocationIndex]);
                }
                
                // Show coordinates for selected location
                if (selectedLocationIndex >= 0 && selectedLocationIndex < availableLocations.length) {
                    String selectedLocationName = availableLocations[selectedLocationIndex];
                    var locationInfo = mapTask.getLocationInfo(selectedLocationName);
                    if (locationInfo != null) {
                        Coordinate coord = locationInfo.getCoordinate();
                        ImGui.Text("Coordinates: " + coord.getX() + ", " + coord.getY() + ", " + coord.getZ());
                        
                        // Navigate button
                        if (ImGui.Button("Navigate to Location")) {
                            mapTask.navigateToLocation(selectedLocationName);
                            coaezUtility.setBotState(CoaezUtility.BotState.MAP_NAVIGATOR);
                        }
                        
                        ImGui.SameLine();
                        
                        // Start/Stop Map Navigator button
                        String mapButtonText = (coaezUtility.getBotState() == CoaezUtility.BotState.MAP_NAVIGATOR) ? 
                            "Stop Map Navigator" : "Start Map Navigator";
                        if (ImGui.Button(mapButtonText)) {
                            if (coaezUtility.getBotState() == CoaezUtility.BotState.MAP_NAVIGATOR) {
                                coaezUtility.setBotState(CoaezUtility.BotState.IDLE);
                            } else {
                                coaezUtility.setBotState(CoaezUtility.BotState.MAP_NAVIGATOR);
                            }
                        }
                    }
                }
            } else {
                ImGui.Text("Loading locations...");
            }
        } else {
            ImGui.Text("Initializing location manager...");
        }
    }
    
    private void renderSorceressGardenTab() {
        if (coaezUtility == null) return;
        
        ImGui.Text("Sorceress's Garden Minigame");
        ImGui.Separator();
        
        // Garden selection checkboxes
        ImGui.Text("Select Gardens to Play:");
        ImGui.Separator();
        
        try {
            Set<GardenType> selectedGardens = coaezUtility.getSorceressGardenTask() != null ? coaezUtility.getSorceressGardenTask().getSelectedGardens() : new HashSet<>();
            boolean winterSelected = selectedGardens.contains(GardenType.WINTER);
            boolean springSelected = selectedGardens.contains(GardenType.SPRING);
            boolean summerSelected = selectedGardens.contains(GardenType.SUMMER);
            boolean autumnSelected = selectedGardens.contains(GardenType.AUTUMN);

            boolean newWinterSelected = ImGui.Checkbox("Winter Garden (Level 1 Thieving)", winterSelected);
            if (newWinterSelected != winterSelected) {
                Set<GardenType> newSet = new HashSet<>(selectedGardens);
                if (newWinterSelected) newSet.add(GardenType.WINTER); else newSet.remove(GardenType.WINTER);
                if (coaezUtility.getSorceressGardenTask() != null) coaezUtility.getSorceressGardenTask().setSelectedGardens(newSet);
                saveConfig();
            }
            boolean newSpringSelected = ImGui.Checkbox("Spring Garden (Level 25 Thieving)", springSelected);
            if (newSpringSelected != springSelected) {
                Set<GardenType> newSet = new HashSet<>(selectedGardens);
                if (newSpringSelected) newSet.add(GardenType.SPRING); else newSet.remove(GardenType.SPRING);
                if (coaezUtility.getSorceressGardenTask() != null) coaezUtility.getSorceressGardenTask().setSelectedGardens(newSet);
                saveConfig();
            }
            boolean newAutumnSelected = ImGui.Checkbox("Autumn Garden (Level 45 Thieving)", autumnSelected);
            if (newAutumnSelected != autumnSelected) {
                Set<GardenType> newSet = new HashSet<>(selectedGardens);
                if (newAutumnSelected) newSet.add(GardenType.AUTUMN); else newSet.remove(GardenType.AUTUMN);
                if (coaezUtility.getSorceressGardenTask() != null) coaezUtility.getSorceressGardenTask().setSelectedGardens(newSet);
                saveConfig();
            }
            boolean newSummerSelected = ImGui.Checkbox("Summer Garden (Level 65 Thieving)", summerSelected);
            if (newSummerSelected != summerSelected) {
                Set<GardenType> newSet = new HashSet<>(selectedGardens);
                if (newSummerSelected) newSet.add(GardenType.SUMMER); else newSet.remove(GardenType.SUMMER);
                if (coaezUtility.getSorceressGardenTask() != null) coaezUtility.getSorceressGardenTask().setSelectedGardens(newSet);
                saveConfig();
            }
        } catch (Exception e) {
            // Silently handle any errors to prevent GUI issues
        }
        
        ImGui.Separator();
        
        // Start/Stop button
        if (ImGui.Button(sorceressGardenActive ? "Stop Sorceress Garden" : "Start Sorceress Garden")) {
            sorceressGardenActive = !sorceressGardenActive;
            if (sorceressGardenActive) {
                coaezUtility.setBotState(CoaezUtility.BotState.SORCERESS_GARDEN);
            } else {
                coaezUtility.setBotState(CoaezUtility.BotState.IDLE);
            }
        }
        
    }
    
    @Override
    public void drawScriptConsole() {
        String windowId = "Script Console##CoaezUtility";

        if (ImGui.Begin(windowId, ImGuiWindowFlag.None.getValue())) {
            if (ImGui.Button("Clear")) {
                coaezUtility.getConsole().clear();
            }

            ImGui.SameLine();
            coaezUtility.getConsole().setScrollToBottom(ImGui.Checkbox("Scroll to bottom", coaezUtility.getConsole().isScrollToBottom()));

            if (ImGui.BeginChild("##console_lines", -1.0F, -1.0F, true, 0)) {
                for (int i = 0; i < 200; ++i) {
                    int lineIndex = (coaezUtility.getConsole().getLineIndex() + i) % 200;
                    if (coaezUtility.getConsole().getConsoleLines()[lineIndex] != null) {
                        ImGui.Text("%s", coaezUtility.getConsole().getConsoleLines()[lineIndex]);
                    }
                }

                if (coaezUtility.getConsole().isScrollToBottom()) {
                    ImGui.SetScrollHereY(1.0F);
                }

                ImGui.EndChild();
            }
        }

        ImGui.End();
    }
}

