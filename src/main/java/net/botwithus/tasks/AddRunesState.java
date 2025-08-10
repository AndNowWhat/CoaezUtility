package net.botwithus.tasks;

public class AddRunesState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleAddRunesToUrns();
    }
}

