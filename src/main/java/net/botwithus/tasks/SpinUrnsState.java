package net.botwithus.tasks;

public class SpinUrnsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleSpinUrns();
    }
}

