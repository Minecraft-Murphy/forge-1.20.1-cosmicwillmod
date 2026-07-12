package com.Murphy.cosmicwill.nullification;

public enum ErasureMode {
    DESTRUCTION_SINGLE(true, 0),
    DESTRUCTION_SWEEP(true, 0),
    RULE_NULLIFICATION(false, 1),
    CONCEPT_ERASURE(false, 0);

    private final boolean settlesRewards;
    private final int eraseDelayTicks;

    ErasureMode(boolean settlesRewards, int eraseDelayTicks) {
        this.settlesRewards = settlesRewards;
        this.eraseDelayTicks = eraseDelayTicks;
    }

    public boolean settlesRewards() {
        return settlesRewards;
    }

    public boolean isNullification() {
        return !settlesRewards;
    }

    public int eraseDelayTicks() {
        return eraseDelayTicks;
    }

    public boolean isDestructionSweep() {
        return this == DESTRUCTION_SWEEP;
    }
}
