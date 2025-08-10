package net.botwithus.tasks;

public class FireUrnsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleFireUrns();
    }
}

