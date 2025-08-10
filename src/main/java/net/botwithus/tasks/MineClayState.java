package net.botwithus.tasks;

public class MineClayState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleMineClayUnderground();
    }
}

