package hackerrouter.villagerrngdebug.client;

import com.seedfinding.latticg.reversal.DynamicProgram;
import com.seedfinding.latticg.reversal.calltype.java.JavaCalls;
import com.seedfinding.latticg.util.LCG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cracks the villager RNG seed from ambient sound trigger ticks using
 * nextInt(1000) interval constraints.
 *
 * Per tick the villager RNG consumes nextInt(1000) then nextInt(100);
 * a trigger tick additionally consumes two nextFloat() calls for voice pitch.
 * The first trigger anchors the window (ambientSoundTime resets to -80).
 */
public class PitchCracker {
    public static final long MULT = 0x5DEECE66DL;
    public static final long ADD = 0xBL;
    public static final long MASK = (1L << 48) - 1;

    private static final int MAX_CONSTRAINTS = 9;

    /**
     * @param triggerTicks world ticks of the observed ambient sound triggers
     * @return {windowStartSeed, lastTriggerEndSeed}, or null on failure
     */
    public static long[] crack(long[] triggerTicks) {
        if (triggerTicks.length < 2) return null;

        int n = triggerTicks.length - 1;
        int[] relativeTicks = new int[n];
        int[] astAtTrigger = new int[n];
        long baseOffset = triggerTicks[0] + 1;

        for (int i = 0; i < n; i++) {
            relativeTicks[i] = (int)(triggerTicks[i + 1] - baseOffset);
            astAtTrigger[i] = (int)(-80 + (triggerTicks[i + 1] - triggerTicks[i]));
        }

        int totalTicks = relativeTicks[n - 1] + 1;

        List<Long> candidates = solveWithConstraints(relativeTicks, astAtTrigger, totalTicks, Math.min(MAX_CONSTRAINTS, n));
        if (candidates.isEmpty()) return null;

        List<Long> verified = new ArrayList<>();
        for (long internalSeed : candidates) {
            if (verifyCandidate(internalSeed, relativeTicks, astAtTrigger, totalTicks)) {
                verified.add(internalSeed);
            }
        }

        long windowInternal;
        if (!verified.isEmpty()) {
            windowInternal = verified.get(0);
        } else if (candidates.size() == 1) {
            windowInternal = candidates.get(0);
        } else {
            return null;
        }

        long lastTriggerInternal = advanceToEndOfLastTrigger(windowInternal, relativeTicks, astAtTrigger, totalTicks);

        return new long[]{ windowInternal ^ MULT, lastTriggerInternal ^ MULT };
    }

    /** Replays forward to the seed state after the last trigger's raid check. */
    private static long advanceToEndOfLastTrigger(long internalSeed, int[] triggerTicks, int[] astAtTrigger, int totalTicks) {
        long seed = internalSeed;
        int ast = -80;
        int trigIdx = 0;

        for (int tick = 0; tick < totalTicks; tick++) {
            int val = nextInt1000(seed);
            seed = advanceForNextInt1000(seed);

            if (val < ast) {
                trigIdx++;
                seed = (seed * MULT + ADD) & MASK; // nextFloat A
                seed = (seed * MULT + ADD) & MASK; // nextFloat B
                ast = -80;
            } else {
                ast++;
            }

            seed = advanceForNextInt100(seed); // raid check

            if (trigIdx == triggerTicks.length) return seed; // after last trigger's raid check
        }
        return seed;
    }

    /** Builds the LattiCG program, constraining nextInt(1000).lessThan(ast) on the tightest triggers. */
    private static List<Long> solveWithConstraints(int[] triggerTicks, int[] astAtTrigger, int totalTicks, int maxConstraints) {
        // Lower ast is more informative.
        Integer[] indices = new Integer[triggerTicks.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingInt(i -> astAtTrigger[i]));

        Set<Integer> constrained = new HashSet<>();
        for (int k = 0; k < Math.min(maxConstraints, indices.length); k++) {
            if (astAtTrigger[indices[k]] >= 1) {
                constrained.add(indices[k]);
            }
        }

        DynamicProgram dp = DynamicProgram.create(LCG.JAVA);
        int prevTick = -1;

        for (int i = 0; i < triggerTicks.length; i++) {
            int t = triggerTicks[i];
            int ast = astAtTrigger[i];

            // Each skipped tick is 2 calls: nextInt(1000) + nextInt(100).
            int gap = t - (prevTick + 1);
            if (gap > 0) dp.skip(gap * 2L);

            if (constrained.contains(i) && ast >= 1 && ast < 1000) {
                dp.add(JavaCalls.nextInt(1000).lessThan(ast));
            } else {
                dp.skip(1);
            }
            dp.skip(3); // two pitch floats + raid check

            prevTick = t;
        }

        List<Long> results = new ArrayList<>();
        dp.reverse().forEach(results::add);
        return results;
    }

    /** Verifies a candidate by replaying the observation window. */
    private static boolean verifyCandidate(long internalSeed, int[] triggerTicks, int[] astAtTrigger, int totalTicks) {
        return verifyWithFullSim(internalSeed, triggerTicks, totalTicks);
    }

    /** Replays the window and checks that triggers land on the expected ticks. */
    private static boolean verifyWithFullSim(long internalSeed, int[] triggerTicks, int totalTicks) {
        long seed = internalSeed;
        int ast = -80;
        int trigIdx = 0;

        for (int tick = 0; tick < totalTicks; tick++) {
            int val = nextInt1000(seed);
            seed = advanceForNextInt1000(seed);

            if (val < ast) {
                if (trigIdx >= triggerTicks.length || triggerTicks[trigIdx] != tick) return false;
                trigIdx++;
                seed = (seed * MULT + ADD) & MASK; // voice pitch float 1
                seed = (seed * MULT + ADD) & MASK; // voice pitch float 2
                ast = -80;
            } else {
                ast++;
            }

            seed = advanceForNextInt100(seed); // raid check
        }
        return trigIdx == triggerTicks.length;
    }

    /** nextInt(1000) result for the current seed, without fully advancing. */
    private static int nextInt1000(long seed) {
        long s = (seed * MULT + ADD) & MASK;
        int bits = (int)(s >>> 17);
        int val = bits % 1000;
        if (bits - val + 999 < 0) {
            // Rejection retry.
            s = (s * MULT + ADD) & MASK;
            bits = (int)(s >>> 17);
            val = bits % 1000;
        }
        return val;
    }

    /** Advance seed past a nextInt(1000) call. */
    private static long advanceForNextInt1000(long seed) {
        long s = (seed * MULT + ADD) & MASK;
        int bits = (int)(s >>> 17);
        if (bits - (bits % 1000) + 999 < 0) {
            s = (s * MULT + ADD) & MASK; // rejection retry
        }
        return s;
    }

    /** Advance seed past a nextInt(100) call. */
    private static long advanceForNextInt100(long seed) {
        long s = (seed * MULT + ADD) & MASK;
        int bits = (int)(s >>> 17);
        if (bits - (bits % 100) + 99 < 0) {
            s = (s * MULT + ADD) & MASK;
        }
        return s;
    }

    /** Advance internal LCG seed by n steps. */
    public static long advance(long seed, long n) {
        long m = MULT, a = ADD, rm = 1, ra = 0;
        for (long k = n; k != 0; k >>>= 1) {
            if ((k & 1) != 0) { rm = (rm * m) & MASK; ra = (ra * m + a) & MASK; }
            a = (a * (m + 1)) & MASK; m = (m * m) & MASK;
        }
        return (rm * seed + ra) & MASK;
    }

    /** Convert internal seed to actual seed. */
    public static long toActual(long internalSeed) { return internalSeed ^ MULT; }

    /** Convert actual seed to internal seed. */
    public static long toInternal(long actualSeed) { return actualSeed ^ MULT; }

    private PitchCracker() {}
}
