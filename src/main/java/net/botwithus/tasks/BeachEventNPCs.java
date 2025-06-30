package net.botwithus.tasks;

public enum BeachEventNPCs {
    FISHING_SPOT(21157),
    WELLINGTON(21150),
    CLAWDIA(21156),
    GRETA(21333),
    PINATA(29225),
    IVAN(0),
    
    // Sandcastle NPCs
    WIZARDS_NPC(21164),
    LUMBRIDGE_NPC(21167),
    PYRAMID_NPC(21166),
    EXCHANGE_NPC(21165);
    
    private final int id;
    
    BeachEventNPCs(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
} 