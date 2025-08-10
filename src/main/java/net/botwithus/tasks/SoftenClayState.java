package net.botwithus.tasks;

public class SoftenClayState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleSoftenClayAtSink();
    }
}

