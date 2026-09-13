package hackerrouter.villagerrngdebug.client;

import java.util.UUID;

public class ClientRngState {
    public static final int REQUIRED_OBSERVATIONS = 10;

    private UUID targetUuid;
    private State state = State.IDLE;

    private long[] triggerTicks = new long[REQUIRED_OBSERVATIONS];
    private int triggerCount = 0;

    // [0] = actual seed at trigger[0]+1 (window start)
    // [1] = actual seed after trigger[last] raid check
    private long crackedSeedStart;
    private long crackedSeedEnd;

    public enum State { IDLE, COLLECTING, CRACKING, CRACKED }

    public void setTarget(UUID uuid) {
        this.targetUuid = uuid;
        resetCollection();
        this.state = State.COLLECTING;
    }

    public void resetCollection() {
        this.triggerCount = 0;
        this.triggerTicks = new long[REQUIRED_OBSERVATIONS];
        this.crackedSeedStart = 0;
        this.crackedSeedEnd = 0;
    }

    public void reset() { this.state = State.IDLE; resetCollection(); }
    public void clearTarget() { this.targetUuid = null; reset(); }

    public UUID getTargetUuid() { return targetUuid; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getTriggerCount() { return triggerCount; }

    public void recordTrigger(long tick) {
        if (triggerCount < REQUIRED_OBSERVATIONS) triggerTicks[triggerCount++] = tick;
    }

    public long getTriggerTick(int index) { return triggerTicks[index]; }

    public long[] getTriggerTicks() {
        long[] result = new long[triggerCount];
        System.arraycopy(triggerTicks, 0, result, 0, triggerCount);
        return result;
    }

    public boolean isCollectionComplete() { return triggerCount >= REQUIRED_OBSERVATIONS; }

    public void setCrackedSeeds(long seedStart, long seedEnd) {
        this.crackedSeedStart = seedStart;
        this.crackedSeedEnd = seedEnd;
        this.state = State.CRACKED;
    }

    public long getCrackedSeedStart() { return crackedSeedStart; }
    public long getCrackedSeedEnd() { return crackedSeedEnd; }
}
