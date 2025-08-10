package net.botwithus.tasks;

public class GoUpstairsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleGoUpstairs();
    }
}

