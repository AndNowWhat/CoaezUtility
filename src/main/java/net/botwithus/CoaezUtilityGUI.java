package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.imgui.BGList;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.tasks.PortableType;
import net.botwithus.tasks.PortableWorkbench;
import net.botwithus.tasks.Product;
import net.botwithus.tasks.Ingredient;
import net.botwithus.tasks.Portable;
import net.botwithus.GuiStyling.ImGuiCol;
import net.botwithus.api.game.hud.Dialog;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.tasks.SimplePortable;
import net.botwithus.tasks.PortableCrafter;
import net.botwithus.tasks.SawmillPlank;
import net.botwithus.tasks.PortableSawmill;
import net.botwithus.rs3.game.quest.Quest;
import net.botwithus.tasks.QuestHelper;
import net.botwithus.tasks.QuestDialogFetcher;
import net.botwithus.rs3.game.Coordinate;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Optional;
import java.util.Map;

public class CoaezUtilityGUI extends ScriptGraphicsContext {
    private final CoaezUtility coaezUtility;
    private CoaezUtility.BotState lastBotState;
    private String alchemyInput = "";
    private String disassemblyInput = "";
    private String posdLootInput = "";
    private Set<String> preloadedAlchemyItems = new HashSet<>();
    private Set<String> preloadedDisassemblyItems = new HashSet<>();

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

    public CoaezUtilityGUI(ScriptConsole scriptConsole, CoaezUtility coaezUtility) {
        super(scriptConsole);
        this.coaezUtility = coaezUtility;
        
        if (this.coaezUtility != null) {
            lastBotState = this.coaezUtility.getBotState();
            loadConfig(); // This calls updateActivePortableType internally

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
             ScriptConsole.println("[CoaezUtilityGUI] CRITICAL: CoaezUtility instance is null in GUI constructor.");
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

        ImGui.Text("Prayer & Crafting Activities");
        
        if (ImGui.Button("Start Powder of Burials")) {
            coaezUtility.setBotState(CoaezUtility.BotState.POWDER_OF_BURIALS);
        }
        
        if (ImGui.Button("Start Sheep Shearing")) {
            coaezUtility.setBotState(CoaezUtility.BotState.SHEEP_SHEARING);
        }
        
        if (ImGui.Button("Start Gem Crafting")) {
            coaezUtility.setBotState(CoaezUtility.BotState.GEM_CRAFTING);
        }

        if (ImGui.Button("Start Fungal Bowstrings")) {
            coaezUtility.setBotState(CoaezUtility.BotState.FUNGAL_BOWSTRINGS);
        }

        if (ImGui.Button("Start Portables")) {
            coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
        }

/*         if (ImGui.Button("Start Smithing")) {
            coaezUtility.setBotState(CoaezUtility.BotState.SMITHING);
        } */
        
        ImGui.Separator();
        
        ImGui.Text("Archaeology Activities");
        
        if (ImGui.Button("Start Soil Sifting (Spell)")) {
            coaezUtility.setBotState(CoaezUtility.BotState.SIFT_SOIL);
        }
        
        if (ImGui.Button("Start Soil Screening (Mesh)")) {
            coaezUtility.setBotState(CoaezUtility.BotState.SCREEN_MESH);
        }
                
        ImGui.SeparatorText("Combat Activities");
        
        ImGui.SeparatorText("Invention");
        if (ImGui.Button("Start invention")) {
            coaezUtility.setBotState(CoaezUtility.BotState.INVENTION);
        }

        ImGui.SeparatorText("Enchanting bolts");
        if (ImGui.Button("Start enchanting")) {
            coaezUtility.setBotState(CoaezUtility.BotState.ENCHANTING);
        }

        ImGui.SeparatorText("Quest Helper");
        if (ImGui.Button("Start Quest Helper")) {
            coaezUtility.setBotState(CoaezUtility.BotState.QUESTS);
            if (coaezUtility.getQuestHelper() != null) {
                coaezUtility.getQuestHelper().initializeQuestDisplay();
            }
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
            preloadedAlchemyItems.add(alchemyInput); // Consider if this is still needed
            alchemyInput = "";
        }
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
                        ImGui.Text("No products available for this group: " + (currentGroupNames.isEmpty() ? "" : currentGroupNames.get(selectedGroupIndex)));
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
                        ImGui.Text("No products available for category: " + (currentCrafterGroupNames.isEmpty() ? "N/A" : currentCrafterGroupNames.get(selectedCrafterGroupIndex)));
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
                        ImGui.Text("No products available for action: " + (PortableCrafter.CRAFTER_OPTIONS.length > selectedCrafterOptionIndex ? PortableCrafter.CRAFTER_OPTIONS[selectedCrafterOptionIndex] : "Unknown Action"));
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

        if (ImGui.Button("Start Current Portable Task")) {
            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null &&
                (portableTypes[selectedPortableTypeIndex] != PortableType.WORKBENCH && 
                 portableTypes[selectedPortableTypeIndex] != PortableType.CRAFTER && 
                 portableTypes[selectedPortableTypeIndex] != PortableType.SAWMILL)) { // Non-configurable portables
                coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
                saveConfig();
            } else if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null && 
                       (portableTypes[selectedPortableTypeIndex] == PortableType.WORKBENCH || 
                        portableTypes[selectedPortableTypeIndex] == PortableType.CRAFTER || 
                        portableTypes[selectedPortableTypeIndex] == PortableType.SAWMILL)) {
                // Check if product/plank is selected for configurable portables
                boolean canStart = false;
                Portable currentPortable = coaezUtility.getPortableTask().getActivePortable();
                if (currentPortable instanceof PortableWorkbench && ((PortableWorkbench) currentPortable).getSelectedProduct() != null) {
                    canStart = true;
                } else if (currentPortable instanceof PortableCrafter && ((PortableCrafter) currentPortable).getSelectedProduct() != null) {
                    canStart = true;
                } else if (currentPortable instanceof PortableSawmill && ((PortableSawmill) currentPortable).getSelectedPlank() != null) {
                    canStart = true;
                }

                if (canStart) {
                    coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
                    saveConfig();
                } else {
                    ScriptConsole.println("[GUI] Cannot start Portables: No product/plank selected for the current portable type.");
                }
            } else {
                ScriptConsole.println("[GUI] Cannot start Portables: No active portable or no product/plank selected.");
            }
        }

        if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
            Portable currentPortable = coaezUtility.getPortableTask().getActivePortable();
            ImGui.Text("Active Portable: " + currentPortable.getType().getName());

            Product currentProd = null;
            SawmillPlank currentPlank = null;

            if (currentPortable instanceof PortableWorkbench) {
                currentProd = ((PortableWorkbench) currentPortable).getSelectedProduct();
            } else if (currentPortable instanceof PortableCrafter) {
                PortableCrafter pc = (PortableCrafter) currentPortable;
                currentProd = pc.getSelectedProduct();
                ImGui.Text("Selected Action: " + (pc.getInteractionOption() != null ? pc.getInteractionOption() : "None"));
                if (!pc.getGroupEnumIds().isEmpty() && pc.getSelectedGroupId() != -1) {
                     ImGui.Text("Selected Category: " + pc.getGroupName(pc.getSelectedGroupId()));
                }
            } else if (currentPortable instanceof PortableSawmill) {
                currentPlank = ((PortableSawmill) currentPortable).getSelectedPlank();
            }

            // Simplified Product Display
            if (currentProd != null) {
                ImGui.Text("Selected Product: " + currentProd.getName());

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
                ImGui.Text("Selected Plank: " + currentPlank.getDisplayName());
            } else {
                // Display only if it's a type that *should* have a product/plank
                if (currentPortable instanceof PortableWorkbench || currentPortable instanceof PortableCrafter || currentPortable instanceof PortableSawmill) {
                    ImGui.Text("Selected Product/Plank: None");
                }
            }

        } else {
            ImGui.Text("Active Portable: None");
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
                
                if (questHelper.areDialogsFetched()) {
                    ImGui.Text("Dialogs Status: Loaded");
                    Map<String, List<QuestDialogFetcher.DialogSequence>> dialogs = questHelper.getFetchedDialogs();
                    ImGui.Text("Dialog Sections: " + dialogs.size());
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
                        ScriptConsole.println("[GUI] Error parsing saved workbenchProductId: " + savedProductIdStr);
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
                // Clear sawmill state (selectedSawmillPlankIndex is fine as is, used by GUI)
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
                 ScriptConsole.println("[GUI] Unknown portable type selected in update: " + selectedType);
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
                 ScriptConsole.println("[GUI|UpdateActivePortableType|CRAFTER] CRITICAL EXCEPTION during PortableCrafter constructor: " + t.getMessage());
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
                    ScriptConsole.println("[GUI|UpdateActivePortableType|CRAFTER] CRAFTER_OPTIONS is not available. Cannot initialize default option.");
                    this.currentCrafterGroupIds.clear();
                    this.currentCrafterGroupNames.clear();
                    this.currentCrafterProducts.clear();
                    if(pc_instance != null) pc_instance.setSelectedProduct(null);
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

        config.save();
    }

    private void loadConfig() {
        if (coaezUtility == null || coaezUtility.getConfig() == null) return;
        ScriptConfig config = coaezUtility.getConfig();

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
                      ScriptConsole.println("[GUI] Saved portable type name " + selectedPortableTypeName + " not found in PortableType enum. Defaulting.");
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
                         ScriptConsole.println("[GUI Load] Set Crafter option to index: " + selectedCrafterOptionIndex + " (" + chosenOption + ")");
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
                            ScriptConsole.println("[GUI Load] Set Crafter group to index: " + selectedCrafterGroupIndex + " (ID: " + savedCrafterGroupId + ")");
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
                        ScriptConsole.println("[GUI Load] Restored Crafter product to index: " + selectedCrafterProductIndex + " (ID: " + savedCrafterProductId + ")");
                    } else {
                        // Select default product (index 0) if restoration failed or no ID saved
                        selectedCrafterProductIndex = 0;
                        if (currentCrafterProducts != null && !currentCrafterProducts.isEmpty()) {
                            crafterInstance.setSelectedProduct(currentCrafterProducts.get(0));
                             ScriptConsole.println("[GUI Load] Setting default Crafter product (index 0). ID: " + currentCrafterProducts.get(0).getId());
                        } else {
                            crafterInstance.setSelectedProduct(null); // No products available
                             ScriptConsole.println("[GUI Load] No Crafter products available for selected group/option.");
                        }
                    }
                } 
                // ** Crafter State Restoration END **

            } catch (IllegalArgumentException e) {
                 ScriptConsole.println("[GUI] Failed to load portable type from config: " + selectedPortableTypeName + ". " + e.getMessage());
                 selectedPortableTypeIndex = 0;
                 updateActivePortableType();
             }
         } else if (coaezUtility.getPortableTask() != null) {
               selectedPortableTypeIndex = 0;
               updateActivePortableType();
          }
        // TODO: Load SmithingTask selections from config and update GUI state
        // (selectedSmithingCategoryIndex, selectedSmithingProductIndex, and call smithingTask.setSelected...)
     }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
        
        // Draw quest dialog assistance overlay
        if (coaezUtility != null && coaezUtility.getQuestHelper() != null) {
            QuestHelper questHelper = coaezUtility.getQuestHelper();
                        
            if (questHelper.isDialogAssistanceActive()) {
                drawDialogAssistanceOverlay(questHelper);
            }
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
        
        // Show overlay if we have dialog assistance active, even without full dialog data
        boolean dialogsFetched = questHelper.areDialogsFetched();
        String recommendedOption = questHelper.getCurrentRecommendedOption();
        int recommendedIndex = questHelper.getCurrentRecommendedOptionIndex();
        
        // Show overlay if we have dialog assistance active, even without full dialog data
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
                    
                    // Draw quest name at the top with teal accent
                    ImGui.PushStyleColor(0, 0.25f, 0.78f, 0.71f, 1.0f); // Teal accent
                    ImGui.Text("Quest: " + selectedQuest.name());
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
                        ImGui.Text("Status: " + status);
                        ImGui.PopStyleColor();
                        
                    } catch (Exception e) {
                        ImGui.PushStyleColor(0, 0.8f, 0.4f, 0.4f, 1.0f); // Red
                        ImGui.Text("Progress: Unable to determine");
                        ImGui.PopStyleColor();
                    }
                    
                    ImGui.Separator();
                    
                    if (!dialogsFetched) {
                        // Show loading state with yellow accent
                        ImGui.PushStyleColor(0, 1.0f, 0.8f, 0.2f, 1.0f); // Warm yellow
                        ImGui.Text("Loading dialog data...");
                        ImGui.PopStyleColor();
                    } else {
                        // Show current dialog text if available
                        String currentDialogText = questHelper.getCurrentDialogText();
                        if (currentDialogText != null && !currentDialogText.trim().isEmpty()) {
                            ImGui.PushStyleColor(0, 0.7f, 0.85f, 1.0f, 1.0f); // Light blue
                            ImGui.Text("Current Dialog Options:");
                            ImGui.PopStyleColor();
                            
                            // Wrap long dialog text
                            ImGui.PushStyleColor(0, 0.9f, 0.9f, 0.9f, 1.0f); // Light gray
                            String wrappedText = currentDialogText.length() > 80 ? 
                                currentDialogText.substring(0, 80) + "..." : currentDialogText;
                            ImGui.Text("\"" + wrappedText + "\"");
                            ImGui.PopStyleColor();
                            
                            ImGui.Separator();
                        }
                        
                        if (recommendedOption != null && recommendedIndex >= 0) {
                            // Draw recommended option with teal highlighting
                            ImGui.PushStyleColor(0, 0.25f, 0.78f, 0.71f, 1.0f); // Teal accent
                            ImGui.Text("RECOMMENDED: Option " + (recommendedIndex + 1));
                            ImGui.PopStyleColor();
                            
                            // Draw the option text if available
                            if (!recommendedOption.trim().isEmpty()) {
                                ImGui.PushStyleColor(0, 0.4f, 0.9f, 0.7f, 1.0f); // Lighter teal
                                ImGui.Text("\"" + recommendedOption + "\"");
                                ImGui.PopStyleColor();
                            }
                            
                            // Show matching status
                            ImGui.PushStyleColor(0, 0.2f, 0.8f, 0.4f, 1.0f); // Green-teal
                            ImGui.Text("Match found in dialog");
                            ImGui.PopStyleColor();
                        } else {
                            // Show that dialog assistance is active but no recommendation available
                            ImGui.PushStyleColor(0, 1.0f, 0.6f, 0.2f, 1.0f); // Orange
                            ImGui.Text("Dialog assistance active");
                            ImGui.PopStyleColor();
                            
                            if (currentDialogText != null && !currentDialogText.trim().isEmpty()) {
                                ImGui.Text("No matching options found");
                            } else {
                                ImGui.Text("Waiting for dialog options...");
                            }
                        }
                    }
                    
                    ImGui.End();
                }
                
                // Pop the window background color
                ImGui.PopStyleColor();
                
            } catch (Exception e) {
                ScriptConsole.println("[CoaezUtilityGUI] Error drawing overlay: " + e.getMessage());
                // Don't rethrow - just log and continue
            } finally {
                // Reset styling
                guiStyling.resetCustomStyles();
                guiStyling.resetCustomColors();
            }
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
}