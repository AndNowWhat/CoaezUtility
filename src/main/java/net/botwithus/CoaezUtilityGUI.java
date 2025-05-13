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
import net.botwithus.tasks.Portable;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.tasks.SimplePortable;
import net.botwithus.tasks.PortableCrafter;

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

    // Crafter specific state
    private int selectedCrafterOptionIndex = 0;
    private int selectedCrafterGroupIndex = 0;
    private int selectedCrafterProductIndex = 0;
    private List<Product> currentCrafterProducts = new ArrayList<>();
    private List<Integer> currentCrafterGroupIds = new ArrayList<>();
    private List<String> currentCrafterGroupNames = new ArrayList<>();

    // Window dimensions
    private final int LISTBOX_HEIGHT = 150;

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
                List<Integer> liveGroupIds = currentWorkbench.getGroupEnumIds();
                if (!liveGroupIds.equals(this.currentGroupIds)) {
                    this.currentGroupIds = liveGroupIds;
                    this.currentGroupNames = this.currentGroupIds.stream()
                                                    .map(currentWorkbench::getGroupName)
                                                    .collect(Collectors.toList());
                    if(selectedGroupIndex >= this.currentGroupNames.size()) selectedGroupIndex = 0;
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

                // Determine if the *current* state of the crafter (after any changes) uses groups
                // This must be checked *after* the option change logic has populated currentCrafterGroupIds
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
        } else {
            ImGui.Text("Portable selected, but no active portable task found or task is of wrong type.");
        }

        if (ImGui.Button("Start Current Portable Task")) {
            if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null &&
                (portableTypes[selectedPortableTypeIndex] != PortableType.WORKBENCH && portableTypes[selectedPortableTypeIndex] != PortableType.CRAFTER)) {
                coaezUtility.setBotState(CoaezUtility.BotState.PORTABLES);
                saveConfig();
            } else {
                ScriptConsole.println("[GUI] Cannot start Portables: No active portable or no product selected for workbench or crafter.");
            }
        }

        if (coaezUtility.getPortableTask() != null && coaezUtility.getPortableTask().getActivePortable() != null) {
            Portable currentPortable = coaezUtility.getPortableTask().getActivePortable();
            ImGui.Text("Active Portable: " + currentPortable.getType().getName());

            Product currentProd = null;

            if (currentPortable instanceof PortableWorkbench) {
                currentProd = ((PortableWorkbench) currentPortable).getSelectedProduct();
            } else if (currentPortable instanceof PortableCrafter) {
                PortableCrafter pc = (PortableCrafter) currentPortable;
                currentProd = pc.getSelectedProduct();
                ImGui.Text("Selected Action: " + (pc.getInteractionOption() != null ? pc.getInteractionOption() : "None"));
                if (!pc.getGroupEnumIds().isEmpty() && pc.getSelectedGroupId() != -1) {
                     ImGui.Text("Selected Category: " + pc.getGroupName(pc.getSelectedGroupId()));
                }
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
            } else {
                // Display only if it's a type that *should* have a product
                if (currentPortable instanceof PortableWorkbench || currentPortable instanceof PortableCrafter) {
                    ImGui.Text("Selected Product: None");
                }
            }

        } else {
            ImGui.Text("Active Portable: None");
        }
    }

    private void updateActivePortableType() {
        
        if (coaezUtility.getPortableTask() == null) {
            
            return;
        }

        PortableType selectedType = portableTypes[selectedPortableTypeIndex];
        

        if (selectedType != PortableType.WORKBENCH && selectedType != PortableType.CRAFTER) {
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
             case SAWMILL:
             case RANGE:
             case CRAFTER:
             case WELL:
                 
                 PortableCrafter pc_instance = null;
                  try {
                      pc_instance = new PortableCrafter(coaezUtility);
                  } catch (Throwable t) {
                       ScriptConsole.println("[GUI|UpdateActivePortableType|CRAFTER] CRITICAL EXCEPTION during PortableCrafter constructor: " + t.getMessage());
                       t.printStackTrace();
                       // Clear related GUI state if crafter creation fails
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
                       break; // Exit switch
                    }

                    if (pc_instance != null && coaezUtility.getPortableTask() != null) {
                        coaezUtility.getPortableTask().setActivePortable(pc_instance);
                        // Initialize GUI state and PortableCrafter for the default (0th) crafter option
                        this.selectedCrafterOptionIndex = 0; 
                        
                        // Ensure PortableCrafter.CRAFTER_OPTIONS is accessible and has elements
                        if (PortableCrafter.CRAFTER_OPTIONS != null && PortableCrafter.CRAFTER_OPTIONS.length > 0) {
                            String defaultOptionName = PortableCrafter.CRAFTER_OPTIONS[this.selectedCrafterOptionIndex];
                            pc_instance.setSelectedInteractionOption(defaultOptionName);

                            this.currentCrafterGroupIds = pc_instance.getGroupEnumIds();

                            if (!this.currentCrafterGroupIds.isEmpty()) { // Grouped Mode for default option
                                this.selectedCrafterGroupIndex = 0; // Default to first group
                                if (!this.currentCrafterGroupIds.isEmpty()) { // Check again before get()
                                    int defaultGroupId = this.currentCrafterGroupIds.get(this.selectedCrafterGroupIndex);
                                    pc_instance.setSelectedGroupId(defaultGroupId); // Inform crafter instance
                                    this.currentCrafterGroupNames = this.currentCrafterGroupIds.stream()
                                                                        .map(pc_instance::getGroupName)
                                                                        .collect(Collectors.toList());
                                    this.currentCrafterProducts = new ArrayList<>(pc_instance.getProductsForGroup(defaultGroupId));
                                    this.selectedCrafterProductIndex = 0;
                                } else { // Should be rare, if getGroupEnumIds was non-empty
                                     this.currentCrafterGroupNames.clear();
                                     this.currentCrafterProducts.clear();
                                     this.selectedCrafterProductIndex = 0;
                                }
                            } else { // Direct Product Mode for default option
                                this.currentCrafterGroupNames.clear();
                                this.selectedCrafterGroupIndex = 0;
                                pc_instance.setSelectedGroupId(-1); // Indicate no group
                                this.currentCrafterProducts = new ArrayList<>(pc_instance.getDirectProducts());
                                this.selectedCrafterProductIndex = 0;
                            }

                            // Set default product in crafter instance itself
                            if (!this.currentCrafterProducts.isEmpty() && this.selectedCrafterProductIndex < this.currentCrafterProducts.size()) {
                                pc_instance.setSelectedProduct(this.currentCrafterProducts.get(this.selectedCrafterProductIndex));
                            } else {
                                pc_instance.setSelectedProduct(null);
                                this.selectedCrafterProductIndex = 0; // Ensure index is safe
                            }
                        } else {
                            // Handle case where CRAFTER_OPTIONS might be empty or null
                            ScriptConsole.println("[GUI|UpdateActivePortableType|CRAFTER] CRAFTER_OPTIONS is not available. Cannot initialize default option.");
                            this.currentCrafterGroupIds.clear();
                            this.currentCrafterGroupNames.clear();
                            this.currentCrafterProducts.clear();
                            pc_instance.setSelectedProduct(null);
                        }
                        // coaezUtility.getPortableTask().setSelectedProduct(null); // Redundant if pc_instance manages its own product
                  }
                 break;
            default:
                 ScriptConsole.println("[GUI] Unknown portable type selected in update: " + selectedType);
                  coaezUtility.getPortableTask().setActivePortable(null);
                  coaezUtility.getPortableTask().setSelectedProduct(null);
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
                config.addProperty("crafterOptionIndex", String.valueOf(selectedCrafterOptionIndex));
                
                if (pc.getSelectedProduct() != null) {
                    config.addProperty("crafterProductId", String.valueOf(pc.getSelectedProduct().getId()));
                }
                // Only save group ID if the current option uses groups and a valid group is selected
                if (!currentCrafterGroupIds.isEmpty() && selectedCrafterGroupIndex < currentCrafterGroupIds.size() && selectedCrafterGroupIndex >= 0) {
                     config.addProperty("crafterSelectedGroupId", String.valueOf(currentCrafterGroupIds.get(selectedCrafterGroupIndex)));
                } else {
                    // If no groups or invalid index, perhaps remove the property or save -1
                    config.removeProperty("crafterSelectedGroupId"); 
                }
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
        
     }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}