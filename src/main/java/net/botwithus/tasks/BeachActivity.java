package net.botwithus.tasks;

public enum BeachActivity {
    COCONUT_SHY(0, "Coconut Shy"),
    SANDCASTLE_BUILDING(1, "Sandcastle Building"),
    BARBEQUES(2, "Barbeques"),
    ROCK_POOLS(3, "Rock Pools"),
    PALM_TREE_FARMING(4, "Palm Tree Farming"),
    BODY_BUILDING(5, "Body Building"),
    HOOK_A_DUCK(6, "Hook a Duck"),
    DUNGEONEERING_HOLE(7, "Dungeoneering Hole");
    
    private final int id;
    private final String name;
    
    BeachActivity(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public static BeachActivity getById(int id) {
        for (BeachActivity activity : values()) {
            if (activity.getId() == id) {
                return activity;
            }
        }
        return null;
    }
} 