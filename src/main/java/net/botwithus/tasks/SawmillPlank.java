package net.botwithus.tasks;

public enum SawmillPlank {
    // Page 1 Planks
    NORMAL_PLANKS("Planks", "1. Planks", false, 0),
    OAK_PLANKS("Oak planks", "2. Oak planks", false, 1),
    WILLOW_PLANKS("Willow planks", "3. Willow planks", false, 2),
    TEAK_PLANKS("Teak planks", "4. Teak planks", false, 3),
    MAPLE_PLANKS("Maple planks", "5. Maple planks", false, 4),
    ACADIA_PLANKS("Acadia planks", "6. Acadia planks", false, 5),
    MAHOGANY_PLANKS("Mahogany planks", "7. Mahogany planks", false, 6),

    // Page 2 Planks (example, assuming "More options" was clicked)
    YEW_PLANKS("Yew planks", "1. Yew planks", true, 0),
    MAGIC_PLANKS("Magic planks", "2. Magic planks", true, 1),
    ELDER_PLANKS("Elder planks", "3. Elder planks", true, 2);

    private final String displayName;
    private final String dialogOptionText;
    private final boolean secondPage;
    private final int interfaceOptionIndex;

    SawmillPlank(String displayName, String dialogOptionText, boolean secondPage, int interfaceOptionIndex) {
        this.displayName = displayName;
        this.dialogOptionText = dialogOptionText;
        this.secondPage = secondPage;
        this.interfaceOptionIndex = interfaceOptionIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDialogOptionText() {
        return dialogOptionText;
    }

    public boolean isSecondPage() {
        return secondPage;
    }

    public int getInterfaceOptionIndex() {
        return interfaceOptionIndex;
    }

    @Override
    public String toString() {
        return displayName;
    }
} 