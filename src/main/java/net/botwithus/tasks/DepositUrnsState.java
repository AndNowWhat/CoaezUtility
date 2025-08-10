package net.botwithus.tasks;

public class DepositUrnsState implements UrnTaskState {
    @Override
    public void handle(ClayUrnTask context) {
        context.handleDepositUrns();
    }
}

