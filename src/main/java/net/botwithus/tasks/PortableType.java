package net.botwithus.tasks;

public enum PortableType {
    BRAZIER("Portable brazier"),
    WORKBENCH("Portable workbench"),
    FLETCHER("Portable fletcher"),
    SAWMILL("Portable sawmill"),
    RANGE("Portable range"),
    CRAFTER("Portable crafter"),
    WELL("Portable well");

    private final String name;

    PortableType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}