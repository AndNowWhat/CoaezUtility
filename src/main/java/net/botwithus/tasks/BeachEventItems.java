package net.botwithus.tasks;

public enum BeachEventItems {
    TROPICAL_TROUT(35106),
    PINATA(53329),
    COCONUT(35102),
    ICECREAM(35049),
    INV_SHIPS(33769),
    INV_SHIP_KIT(33768),
    
    LEMON_SOUR(35054),
    PINEAPPLETINI(35053),
    PINK_FIZZ(35051),
    PURPLE_LUMBRIDGE(35052),
    FISHERMANS_FRIEND(51732),
    GEORGES_PEACH_DELIGHT(51733),
    A_HOLE_IN_ONE(51729),
    PALMER_FARMER(51731),
    UGLY_DUCKLING(51730);
    
    private final int id;
    
    BeachEventItems(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
} 