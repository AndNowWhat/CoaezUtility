package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.tasks.PortableType;
import net.botwithus.tasks.PortableWorkbench;
import net.botwithus.tasks.Product;
import net.botwithus.tasks.Ingredient;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Window dimensions
    private final int LISTBOX_HEIGHT = 150;

    public CoaezUtilityGUI(ScriptConsole scriptConsole, CoaezUtility coaezUtility) {
        super(scriptConsole);
        this.coaezUtility = coaezUtility;
        ScriptConsole.println("[CoaezUtilityGUI] Constructor started.");
        if (this.coaezUtility != null) {
            lastBotState = this.coaezUtility.getBotState();
            loadConfig(); // This calls updateActivePortableType internally
            
            // Initialize GUI state based on current task state AFTER loadConfig
            // This section should align the GUI with whatever state was set by loadConfig
            // or the default state if loadConfig didn't set a portable.
            if (this.coaezUtility.getPortableTask() != null && this.coaezUtility.getPortableTask().getActivePortable() != null) {
                PortableType currentTaskType = this.coaezUtility.getPortableTask().getActivePortable().getType();
                 ScriptConsole.println("[CoaezUtilityGUI] Initializing GUI based on active portable type: " + currentTaskType);
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
                         ScriptConsole.println("[CoaezUtilityGUI] Workbench GUI state initialized. Group: " + (this.currentGroupNames.isEmpty() ? "None" : this.currentGroupNames.get(selectedGroupIndex)) + ", Product Index: " + selectedWorkbenchProductIndex);
                    } else {
                         ScriptConsole.println("[CoaezUtilityGUI] Workbench instance has no groups to display.");
                        this.currentGroupNames.clear();
                        this.currentWorkbenchProducts.clear();
                        selectedGroupIndex = 0;
                        selectedWorkbenchProductIndex = 0;
                    }
                }
            } else {
                 ScriptConsole.println("[CoaezUtilityGUI] No active portable task to initialize GUI from, or task is null.");
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
        ScriptConsole.println("[CoaezUtilityGUI] Constructor finished.");
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
                if (ImGui.BeginTabItem("Portables", 0)) {
                    renderPortablesTab();
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
        
        if (ImGui.Button("Start Fungal Bowstrings")) {
            coaezUtility.setBotState(CoaezUtility.BotState.FUNGAL_BOWSTRINGS);
        }

        if (ImGui.Button("Start Portables")) {
            coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
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
        
        ImGui.SeparatorText("Invention");
        if (ImGui.Button("Start invention")) {
            coaezUtility.setBotState(CoaezUtility.BotState.INVENTION);
        } 

        ImGui.SeparatorText("Enchanting bolts");
        if (ImGui.Button("Start enchanting")) {
            coaezUtility.setBotState(CoaezUtility.BotState.ENCHANTING);
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
                
                // -- Group Selection --
                // currentGroupIds and currentGroupNames are now class members, updated by updateActivePortableType
                // or by the initial sync in the constructor.
                // If the list of groups available from the workbench changes *after* initial load, we need to refresh.
                // However, PortableWorkbench loads its groups once in its constructor. So this should be stable unless
                // a new PortableWorkbench instance is created and set on the task.
                
                // Let's ensure the local GUI state for groups is synced if it's somehow stale.
                // This is more of a failsafe; updateActivePortableType should be the primary source of truth.
                List<Integer> liveGroupIds = currentWorkbench.getGroupEnumIds();
                if (!liveGroupIds.equals(this.currentGroupIds)) {
                    this.currentGroupIds = liveGroupIds;
                    this.currentGroupNames = this.currentGroupIds.stream()
                                                    .map(currentWorkbench::getGroupName)
                                                    .collect(Collectors.toList());
                    if(selectedGroupIndex >= this.currentGroupNames.size()) selectedGroupIndex = 0;
                    // Force product reload for the (potentially new) current group
                    if (!this.currentGroupIds.isEmpty()) {
                        // Ensure we create a mutable list
                        this.currentWorkbenchProducts = new ArrayList<>(currentWorkbench.getProductsForGroup(this.currentGroupIds.get(selectedGroupIndex)));
                        if(selectedWorkbenchProductIndex >= this.currentWorkbenchProducts.size()) selectedWorkbenchProductIndex = 0;
                    } else {
                        this.currentWorkbenchProducts.clear();
                        selectedWorkbenchProductIndex = 0;
                    }
                }


                if (!currentGroupNames.isEmpty()) {
                    String[] groupNameArray = currentGroupNames.toArray(String[]::new);
                    // Ensure selectedGroupIndex is valid before rendering Combo
                    if (selectedGroupIndex >= groupNameArray.length && groupNameArray.length > 0) selectedGroupIndex = 0;
                    else if (groupNameArray.length == 0) selectedGroupIndex = 0;


                    int newGroupIndex = ImGui.Combo("Select Group", selectedGroupIndex, groupNameArray);
                    if (newGroupIndex != selectedGroupIndex && newGroupIndex < currentGroupIds.size()) {
                        selectedGroupIndex = newGroupIndex;
                        int selectedGroupId = currentGroupIds.get(selectedGroupIndex);
                        // Ensure we create a mutable list
                        currentWorkbenchProducts = new ArrayList<>(currentWorkbench.getProductsForGroup(selectedGroupId));
                        selectedWorkbenchProductIndex = 0; 
                        
                        if (!currentWorkbenchProducts.isEmpty()) {
                             coaezUtility.getPortableTask().setSelectedProduct(currentWorkbenchProducts.get(0));
                        } else {
                             coaezUtility.getPortableTask().setSelectedProduct(null);
                        }
                        // Don't save config here, product selection below saves it.
                    }

                    // -- Product Selection (within selected group) --
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
        }

        if (ImGui.Button("Start Current Portable Task")) {
            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null &&
                (portableTypes[selectedPortableTypeIndex] != PortableType.WORKBENCH || 
                 (portableTypes[selectedPortableTypeIndex] == PortableType.WORKBENCH && coaezUtility.getPortableTask().getSelectedProduct() != null))) {
                coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
                saveConfig();
            } else {
                ScriptConsole.println("[GUI] Cannot start Portables: No active portable or no product selected for workbench.");
            }
        }

        if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
            ImGui.Text("Active Portable: " + coaezUtility.getPortableTask().getActivePortable().getType().getName());
            Product currentProd = coaezUtility.getPortableTask().getSelectedProduct();
            if (currentProd != null) {
                ImGui.Text("Selected Product: " + currentProd.getName());
                ImGui.Text("Required Ingredients:");
                if (currentProd.getIngredients() != null) {
                    for (net.botwithus.tasks.Ingredient ingredient : currentProd.getIngredients()) {
                        if (ingredient != null) {
                             ImGui.Text("- " + ingredient.getAmount() + " x " + ingredient.getDisplayName());
                        }
                    }
                } else {
                     ImGui.Text("- (No ingredient data)");
                }
            }
        } else {
            ImGui.Text("Active Portable: None");
        }
    }
    
    private void updateActivePortableType() {
        ScriptConsole.println("[GUI] updateActivePortableType called. New type index: " + selectedPortableTypeIndex);
        if (coaezUtility.getPortableTask() == null) {
            ScriptConsole.println("[GUI] PortableTask is null in updateActivePortableType. Cannot proceed.");
            return;
        }

        PortableType selectedType = portableTypes[selectedPortableTypeIndex];
        ScriptConsole.println("[GUI] Selected portable type: " + selectedType.getName());
            
        // Clear workbench specific state before switching, unless it's to workbench itself
        if (selectedType != PortableType.WORKBENCH) {
            currentGroupIds.clear();
            currentGroupNames.clear();
            currentWorkbenchProducts.clear();
            selectedGroupIndex = 0;
            selectedWorkbenchProductIndex = 0;
            coaezUtility.getPortableTask().setActivePortable(null); // Clear active portable if not workbench (or handle other types)
            coaezUtility.getPortableTask().setSelectedProduct(null);
        }

        switch (selectedType) {
            case WORKBENCH:
                ScriptConsole.println("[GUI] Setting up Workbench.");
                PortableWorkbench wb = new PortableWorkbench(coaezUtility); 
                coaezUtility.getPortableTask().setActivePortable(wb);
                
                this.currentGroupIds = wb.getGroupEnumIds();
                if (this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                    this.currentGroupNames = this.currentGroupIds.stream()
                                                .map(wb::getGroupName)
                                                .collect(Collectors.toList());
                    ScriptConsole.println("[GUI] Loaded " + this.currentGroupNames.size() + " groups for Workbench.");
                } else {
                    ScriptConsole.println("[GUI] Workbench loaded 0 groups.");
                    this.currentGroupNames.clear(); // Ensure it's empty
                }
                
                String savedProductIdStr = coaezUtility.getConfig().getProperty("workbenchProductId");
                int lastSelectedProductId = -1;
                boolean productRestored = false;

                if (savedProductIdStr != null && !savedProductIdStr.isEmpty()) {
                    try {
                        lastSelectedProductId = Integer.parseInt(savedProductIdStr);
                         ScriptConsole.println("[GUI] Attempting to restore saved product ID: " + lastSelectedProductId);
                    } catch (NumberFormatException e) {
                        ScriptConsole.println("[GUI] Error parsing saved workbenchProductId: " + savedProductIdStr);
                        lastSelectedProductId = -1;
                    }
                }

                if (lastSelectedProductId != -1 && this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                    for (int gIdx = 0; gIdx < this.currentGroupIds.size(); gIdx++) {
                        int groupId = this.currentGroupIds.get(gIdx);
                        List<Product> productsInGroup = wb.getProductsForGroup(groupId);
                        if (productsInGroup == null) continue; // Should not happen if wb is well-behaved

                        for (int pIdx = 0; pIdx < productsInGroup.size(); pIdx++) {
                            Product p = productsInGroup.get(pIdx);
                            if (p != null && p.getId() == lastSelectedProductId) {
                                selectedGroupIndex = gIdx;
                                selectedWorkbenchProductIndex = pIdx;
                                // Ensure we create a mutable list
                                currentWorkbenchProducts = new ArrayList<>(productsInGroup); 
                                coaezUtility.getPortableTask().setSelectedProduct(p);
                                productRestored = true;
                                ScriptConsole.println("[GUI] Restored WB Group: " + (this.currentGroupNames.isEmpty() ? "Unknown" : this.currentGroupNames.get(gIdx)) + " (Index: " + gIdx + "), Product: " + p.getName() + " (Index: " + pIdx +")");
                                break; 
                            }
                        }
                        if (productRestored) break; 
                    }
                }

                if (!productRestored) {
                    ScriptConsole.println("[GUI] Product not restored. Setting default selection.");
                    selectedGroupIndex = 0;
                    selectedWorkbenchProductIndex = 0;
                    if (this.currentGroupIds != null && !this.currentGroupIds.isEmpty()) {
                         int defaultGroupId = this.currentGroupIds.get(0);
                         // Ensure we create a mutable list
                         currentWorkbenchProducts = new ArrayList<>(wb.getProductsForGroup(defaultGroupId));
                         if (!currentWorkbenchProducts.isEmpty()) { // Check after creating mutable list
                             coaezUtility.getPortableTask().setSelectedProduct(currentWorkbenchProducts.get(0));
                              ScriptConsole.println("[GUI] Default product set to first in first group: " + currentWorkbenchProducts.get(0).getName());
                         } else {
                             coaezUtility.getPortableTask().setSelectedProduct(null);
                             currentWorkbenchProducts.clear(); // Now safe to clear the mutable list
                             ScriptConsole.println("[GUI] Default group is empty.");
                         }
                    } else {
                         coaezUtility.getPortableTask().setSelectedProduct(null);
                         currentWorkbenchProducts.clear(); // Safe to clear
                         if (this.currentGroupNames != null) this.currentGroupNames.clear(); else this.currentGroupNames = new ArrayList<>();
                         ScriptConsole.println("[GUI] No groups available for default selection.");
                    }
                }
                break; 

            case FLETCHER:
            case SAWMILL:
            case RANGE:
            case CRAFTER:
            case WELL:
            case BRAZIER:
                ScriptConsole.println("[GUI] Portable type " + selectedType + " selected but not yet implemented.");
                // coaezUtility.getPortableTask().setActivePortable(null); // Already handled above
                // currentWorkbenchProducts.clear(); // Already handled
                break;
            default:
                ScriptConsole.println("[GUI] Unknown portable type selected in update: " + selectedType);
                // coaezUtility.getPortableTask().setActivePortable(null); // Already handled
                // currentWorkbenchProducts.clear(); // Already handled
                break;
        }
        saveConfig();
    }
    
    
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
        
        // POSD settings - assuming POSD object can be null if not used
        if (coaezUtility.getPOSD() != null) {
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
        }
        
        if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
            config.addProperty("selectedPortableType", coaezUtility.getPortableTask().getActivePortable().getType().name());
            if (coaezUtility.getPortableTask().getActivePortable().getType() == PortableType.WORKBENCH && coaezUtility.getPortableTask().getSelectedProduct() != null) {
                config.addProperty("workbenchProductId", String.valueOf(coaezUtility.getPortableTask().getSelectedProduct().getId()));
            } else {
                config.removeProperty("workbenchProductId");
            }
        } else {
            config.removeProperty("selectedPortableType");
            config.removeProperty("workbenchProductId");
        }
        
        config.save();
        // ScriptConsole.println("[GUI] Config saved.");
    }
    
    private void loadConfig() {
        if (coaezUtility == null || coaezUtility.getConfig() == null) return;
        ScriptConfig config = coaezUtility.getConfig();
        
        config.load();
        ScriptConsole.println("[GUI] loadConfig started.");
        
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
        
        // Load POSD settings
        if (coaezUtility.getPOSD() != null) {
            // Simplified boolean parsing, add more robust checks if needed
            coaezUtility.getPOSD().setUseOverloads(Boolean.parseBoolean(config.getProperty("posdUseOverloads")));
            coaezUtility.getPOSD().setUsePrayerPots(Boolean.parseBoolean(config.getProperty("posdUsePrayerPots")));
            coaezUtility.getPOSD().setUseAggroPots(Boolean.parseBoolean(config.getProperty("posdUseAggroPots")));
            coaezUtility.getPOSD().setUseWeaponPoison(Boolean.parseBoolean(config.getProperty("posdUseWeaponPoison")));
            coaezUtility.getPOSD().setUseQuickPrayers(Boolean.parseBoolean(config.getProperty("posdUseQuickPrayers")));
            try {
                String quickPrayersNumStr = config.getProperty("posdQuickPrayersNumber");
                coaezUtility.getPOSD().setQuickPrayersNumber(quickPrayersNumStr != null ? Integer.parseInt(quickPrayersNumStr) : 1);
                
                String healthThresholdStr = config.getProperty("posdHealthThreshold");
                coaezUtility.getPOSD().setHealthPointsThreshold(healthThresholdStr != null ? Integer.parseInt(healthThresholdStr) : 50);
                
                String prayerThresholdStr = config.getProperty("posdPrayerThreshold");
                coaezUtility.getPOSD().setPrayerPointsThreshold(prayerThresholdStr != null ? Integer.parseInt(prayerThresholdStr) : 200);
            } catch (NumberFormatException e) {
                ScriptConsole.println("[GUI] Error parsing POSD numeric settings: " + e.getMessage());
            }
            coaezUtility.getPOSD().setUseLoot(Boolean.parseBoolean(config.getProperty("posdUseLoot")));
            coaezUtility.getPOSD().setInteractWithLootAll(Boolean.parseBoolean(config.getProperty("posdLootAll")));
            coaezUtility.getPOSD().setUseScrimshaws(Boolean.parseBoolean(config.getProperty("posdUseScrimshaws")));
            coaezUtility.getPOSD().setBankForFood(Boolean.parseBoolean(config.getProperty("posdBankForFood")));
            String targetItemsStr = config.getProperty("posdTargetItems");
            if (targetItemsStr != null && !targetItemsStr.isEmpty()) {
                Arrays.stream(targetItemsStr.split(","))
                        .forEach(item -> coaezUtility.getPOSD().addTargetItem(item));
            }
        }

        // Load selected portable type
        String selectedPortableTypeName = config.getProperty("selectedPortableType");
        ScriptConsole.println("[GUI] Config - selectedPortableType: " + selectedPortableTypeName);

        if (selectedPortableTypeName != null && coaezUtility.getPortableTask() != null) {
            try {
                PortableType type = PortableType.valueOf(selectedPortableTypeName);
                boolean typeSet = false;
                for (int i = 0; i < portableTypes.length; i++) {
                    if (portableTypes[i] == type) {
                        selectedPortableTypeIndex = i; // Set index for Combo
                        // updateActivePortableType will be called after this loop to actually create the portable
                        typeSet = true;
                        break;
                    }
                }
                if (typeSet) {
                    updateActivePortableType(); // Create and set the portable, this also loads products/groups
                } else {
                     ScriptConsole.println("[GUI] Saved portable type name " + selectedPortableTypeName + " not found in PortableType enum. Defaulting.");
                    selectedPortableTypeIndex = 0;
                    updateActivePortableType(); 
                }
            } catch (IllegalArgumentException e) {
                ScriptConsole.println("[GUI] Failed to load portable type from config: " + selectedPortableTypeName + ". " + e.getMessage());
                selectedPortableTypeIndex = 0; // Default to first type
                updateActivePortableType(); 
            }
        } else if (coaezUtility.getPortableTask() != null) {
             // No portable type saved, or task just created. Default to first type.
             ScriptConsole.println("[GUI] No portable type in config or task is new. Defaulting portable type.");
             selectedPortableTypeIndex = 0;
             updateActivePortableType();
        }
        ScriptConsole.println("[GUI] loadConfig finished.");
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
        // Any overlay text if needed
    }
}