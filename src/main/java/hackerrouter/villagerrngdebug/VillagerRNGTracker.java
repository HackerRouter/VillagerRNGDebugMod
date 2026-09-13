package hackerrouter.villagerrngdebug;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerRNGTracker {
    private static final Map<UUID, UUID> trackedVillagerUUIDs = new HashMap<>();
    private static final Map<UUID, Villager> trackedVillagers = new HashMap<>();

    public static void setTracked(UUID playerUUID, Villager villager) {
        trackedVillagers.put(playerUUID, villager);
        trackedVillagerUUIDs.put(playerUUID, villager.getUUID());
    }

    public static void clearTracked(UUID playerUUID) {
        trackedVillagers.remove(playerUUID);
        trackedVillagerUUIDs.remove(playerUUID);
    }

    public static boolean isTrackedByUUID(Villager villager) {
        UUID villagerUUID = villager.getUUID();
        for (Map.Entry<UUID, UUID> entry : trackedVillagerUUIDs.entrySet()) {
            if (villagerUUID.equals(entry.getValue())) {
                trackedVillagers.put(entry.getKey(), villager);
                return true;
            }
        }
        return false;
    }

    public static boolean isTracked(Villager villager) {
        return trackedVillagers.containsValue(villager);
    }

    public static Villager getTracked(UUID playerUUID) {
        return trackedVillagers.get(playerUUID);
    }

    public static long getSeed(RandomSource random) {
        if (random instanceof LegacyRandomSource legacy) {
            return legacy.seed.get() ^ 0x5DEECE66DL;
        }
        return 0;
    }

    public static void setSeed(RandomSource random, long seed) {
        if (random instanceof LegacyRandomSource legacy) {
            legacy.seed.set(seed ^ 0x5DEECE66DL);
        }
    }

    public static void advance(UUID playerUUID, int steps) {
        Villager villager = trackedVillagers.get(playerUUID);
        if (villager == null) return;
        RandomSource random = villager.getRandom();
        long seed = getSeed(random);
        long internal = seed ^ 0x5DEECE66DL;
        for (int i = 0; i < steps; i++) {
            internal = (internal * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
        }
        setSeed(random, internal ^ 0x5DEECE66DL);
    }

    public static boolean advanceTo(UUID playerUUID, long targetSeed, int maxSteps) {
        Villager villager = trackedVillagers.get(playerUUID);
        if (villager == null) return false;
        RandomSource random = villager.getRandom();
        long seed = getSeed(random);
        long internal = seed ^ 0x5DEECE66DL;
        long targetInternal = targetSeed ^ 0x5DEECE66DL;
        for (int i = 0; i < maxSteps; i++) {
            internal = (internal * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
            if (internal == targetInternal) {
                setSeed(random, internal ^ 0x5DEECE66DL);
                return true;
            }
        }
        return false;
    }

    public static void onTradeUpdate(AbstractVillager villager) {
        if (RNGLogger.isUpdateTradesLogEnabled()) {
            RNGLogger.logUpdateTradesStack(villager);
        }
    }

    public static void wrapRandom(Villager villager) {
        RandomSource current = villager.getRandom();
        if (!(current instanceof TrackedRandom)) {
            long seed = getSeed(current);
            ((RandomAccessor) villager).villagerRngDebug$setRandom(new TrackedRandom(villager, seed));
        }
    }
}
