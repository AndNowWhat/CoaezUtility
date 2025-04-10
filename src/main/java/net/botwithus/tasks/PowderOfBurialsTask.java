package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.script.Execution;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import net.botwithus.rs3.game.actionbar.ActionBar;

public class PowderOfBurialsTask implements Task {
    private final CoaezUtility script;
    private static final int BURIAL_POWDER_SPRITE_ID = 52805;
    
    private static final List<String> BONE_NAMES = Arrays.asList(
            "Bones", "Wolf bones", "Burnt bones", "Monkey bones", "Bat bones",
            "Big bones", "Jogre bones", "Zogre bones", "Shaikahan bones",
            "Baby dragon bones", "Wyvern bones", "Dragon bones", "Fayrg bones",
            "Raurg bones", "Dagannoth bones", "Airut bones", "Ourg bones",
            "Hardened dragon bones", "Dragonkin bones", "Dinosaur bones",
            "Frost dragon bones", "Reinforced dragon bones"
    );

    private static final List<String> ASH_NAMES = Arrays.asList(
            "Impious ashes",
            "Accursed ashes",
            "Infernal ashes",
            "Tortured ashes",
            "Searing ashes"
    );
    
    public PowderOfBurialsTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        if (!isPowderOfBurialsActive()) {
            activatePowderOfBurials();
            return;
        }

        if (hasBonesToBury()) {
            buryBones();
        } else {
            Bank.loadLastPreset();
        }
    }
    
    private boolean isPowderOfBurialsActive() {
        Execution.delay(script.getRandom().nextLong(100, 200));
        Component powderOfBurials = ComponentQuery.newQuery(284)
            .spriteId(BURIAL_POWDER_SPRITE_ID)
            .results()
            .first();
        return powderOfBurials != null;
    }
    
    private void activatePowderOfBurials() {
        if (isPowderOfBurialsActive()) return;
        Execution.delay(script.getRandom().nextLong(600, 800));

        if (inventoryInteract("Scatter", "Powder of burials")) {
            Execution.delayUntil(5000, this::isPowderOfBurialsActive);
        }
    }
    
    private boolean inventoryInteract(String option, String... items) {
        Pattern pattern = Pattern.compile(String.join("|", items), Pattern.CASE_INSENSITIVE);
        Item item = net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery.newQuery().name(pattern).results().first();

        if (item != null) {
            String itemName = item.getName();
            Component itemComponent = ComponentQuery.newQuery(1473).componentIndex(5).itemName(itemName).results().first();
            if (itemComponent != null) {
                return itemComponent.interact(option);
            }
        }
        return false;
    }
    
    private boolean hasBonesToBury() {
        // Check for bones
        for (String itemName : BONE_NAMES) {
            Component itemComponent = ComponentQuery.newQuery(1473).componentIndex(5).itemName(itemName).results().first();
            if (itemComponent != null) {
                Execution.delay(script.getRandom().nextInt(100, 300));
                return true;
            }
        }
        
        // Check for ashes
        for (String ashName : ASH_NAMES) {
            Component itemComponent = ComponentQuery.newQuery(1473).componentIndex(5).itemName(ashName).results().first();
            if (itemComponent != null) {
                Execution.delay(script.getRandom().nextInt(100, 300));
                return true;
            }
        }
        
        return false;
    }
    
    private void buryBones() {
        script.setNoBonesLeft(false);
        Random random = script.getRandom();
        
        while (!script.isNoBonesLeft() && script.isActive()) {
            boolean boneSuccess = false;
            boolean ashSuccess = false;
            
            for (String itemName : BONE_NAMES) {
                boneSuccess = ActionBar.useItem(itemName, 1);
                if (boneSuccess) {
                    break;
                }
            }
            
            for (String ashName : ASH_NAMES) {
                ashSuccess = ActionBar.useItem(ashName, 1);
                if (ashSuccess) {
                    break;
                }
            }
            
            if (!boneSuccess && !ashSuccess) {
                break;
            }
            
            Execution.delay(random.nextInt(250, 300));
        }
        
        if (!script.isActive()) {
            System.out.println("Script stopped while burying bones/scattering ashes.");
            return;
        }
        
        if (script.isNoBonesLeft()) {
            System.out.println("No bones or ashes left to process.");
            loadBankPreset();
        } else {
            System.out.println("Finished processing all available bones and ashes.");
        }
    }
    
    private void loadBankPreset() {
        script.setPresetLoaded(false);
        script.setWaitingForPreset(true);
        Bank.loadLastPreset();

        Execution.delayUntil(script.getRandom().nextInt(5000) + 5000, () -> script.isPresetLoaded());
        script.setWaitingForPreset(false);
        Execution.delayUntil(5000, this::hasBonesToBury);
    }
} 