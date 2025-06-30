package net.botwithus.tasks;

public enum BeachEventObjects {
    COCONUT_SKY(97336),
    BARBEQUE_GRILL(97275),
    DUNGEONEERING_HOLE(114121),
    HOOK_A_DUCK(104332),
    BODYBUILDING(97379),
    PILEOFCOCONUTS(97332),
    
    // Palm trees
    PALM_TREE_1(117500),
    PALM_TREE_2(117502),
    PALM_TREE_3(117504),
    PALM_TREE_4(117506),
    PALM_TREE_5(117508),
    PALM_TREE_6(117510),
    
    // Sandcastle objects
    WIZARDS_SANDCASTLE_1(97416),
    WIZARDS_SANDCASTLE_2(97417),
    WIZARDS_SANDCASTLE_3(97418),
    WIZARDS_SANDCASTLE_4(97419),
    
    LUMBRIDGE_SANDCASTLE_1(97424),
    LUMBRIDGE_SANDCASTLE_2(97425),
    LUMBRIDGE_SANDCASTLE_3(97426),
    LUMBRIDGE_SANDCASTLE_4(97427),
    
    PYRAMID_SANDCASTLE_1(109550),
    PYRAMID_SANDCASTLE_2(109551),
    PYRAMID_SANDCASTLE_3(109552),
    PYRAMID_SANDCASTLE_4(109553),
    
    EXCHANGE_SANDCASTLE_1(97420),
    EXCHANGE_SANDCASTLE_2(97421),
    EXCHANGE_SANDCASTLE_3(97422),
    EXCHANGE_SANDCASTLE_4(97423);
    
    private final int id;
    
    BeachEventObjects(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public static int[] getPalmTrees() {
        return new int[]{117500, 117502, 117504, 117506, 117508, 117510};
    }
    
    public static int[] getWizardsSandcastles() {
        return new int[]{97416, 97417, 97418, 97419};
    }
    
    public static int[] getLumbridgeSandcastles() {
        return new int[]{97424, 97425, 97426, 97427};
    }
    
    public static int[] getPyramidSandcastles() {
        return new int[]{109550, 109551, 109552, 109553};
    }
    
    public static int[] getExchangeSandcastles() {
        return new int[]{97420, 97421, 97422, 97423};
    }
} 