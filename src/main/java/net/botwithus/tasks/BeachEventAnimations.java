package net.botwithus.tasks;

public enum BeachEventAnimations {
    ENTER_HOLE(27005),
    HOLE(32865),
    EXIT_HOLE(23051),
    BODYBUILDING(26551),
    BBQ(6784),
    DUCK(29210),
    COCONUT(26586),
    CRUL(26552),
    LUNGE(26553),
    FLY(26554),
    RAISE(26549),
    DIG(830);
    
    private final int id;
    
    BeachEventAnimations(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
} 