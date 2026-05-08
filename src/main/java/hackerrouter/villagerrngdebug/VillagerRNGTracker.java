/*
 * This file is part of the VillagerRNGDebugMod project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * VillagerRNGDebugMod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VillagerRNGDebugMod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VillagerRNGDebugMod.  If not, see <https://www.gnu.org/licenses/>.
 */

package hackerrouter.villagerrngdebug;

import net.minecraft.world.entity.npc.Villager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class VillagerRNGTracker {
    // playerUUID -> villager UUID (persists across chunk reload)
    private static final Map<UUID, UUID> trackedVillagerUUIDs = new HashMap<>();
    // playerUUID -> current Villager instance (may become stale after chunk reload)
    private static final Map<UUID, Villager> trackedVillagers = new HashMap<>();
    private static Field seedField;

    static {
        try {
            seedField = Random.class.getDeclaredField("seed");
            seedField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            System.err.println("[VillagerRNGDebug] WARN: Random.seed field not found");
            seedField = null;
        } catch (Exception e) {
            System.err.println("[VillagerRNGDebug] WARN: Cannot access Random.seed (Java 16+ module restrictions). RNG manipulation disabled.");
            seedField = null;
        }
    }

    public static void setTracked(UUID playerUUID, Villager villager) {
        trackedVillagers.put(playerUUID, villager);
        trackedVillagerUUIDs.put(playerUUID, villager.getUUID());
        RNGLogger.log(String.format("[TRACK] Player %s now tracking Villager@%s", playerUUID, villager.getUUID()));
    }

    public static void clearTracked(UUID playerUUID) {
        Villager villager = trackedVillagers.remove(playerUUID);
        trackedVillagerUUIDs.remove(playerUUID);
        if (villager != null) {
            RNGLogger.log(String.format("[UNTRACK] Player %s stopped tracking Villager@%s", playerUUID, villager.getUUID()));
        }
    }

    /**
     * Called from MixinLivingEntity on entity init.
     * If the new villager's UUID matches a tracked UUID, re-wrap its random and update the instance.
     */
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

    public static long getSeed(Random random) {
        if (seedField == null) return 0;
        try {
            AtomicLong atomicLong = (AtomicLong) seedField.get(random);
            return atomicLong.get() ^ 0x5DEECE66DL;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void setSeed(Random random, long seed) {
        if (seedField == null) return;
        try {
            AtomicLong atomicLong = (AtomicLong) seedField.get(random);
            atomicLong.set(seed ^ 0x5DEECE66DL);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void advance(UUID playerUUID, int steps) {
        Villager villager = trackedVillagers.get(playerUUID);
        if (villager == null) return;
        Random random = villager.getRandom();
        long seed = getSeed(random);
        for (int i = 0; i < steps; i++) {
            seed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
        }
        setSeed(random, seed);
        RNGLogger.log(String.format("[MANIPULATE] Player %s advance +%d  new_seed=0x%012X", playerUUID, steps, seed));
    }

    public static boolean advanceTo(UUID playerUUID, long targetSeed, int maxSteps) {
        Villager villager = trackedVillagers.get(playerUUID);
        if (villager == null) return false;
        Random random = villager.getRandom();
        long seed = getSeed(random);
        for (int i = 0; i < maxSteps; i++) {
            seed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
            if (seed == targetSeed) {
                setSeed(random, seed);
                RNGLogger.log(String.format("[MANIPULATE] Player %s advance-to 0x%012X  steps=%d", playerUUID, targetSeed, i + 1));
                return true;
            }
        }
        RNGLogger.log(String.format("[MANIPULATE] Player %s advance-to FAILED  target=0x%012X  searched=%d steps", playerUUID, targetSeed, maxSteps));
        return false;
    }

    public static void wrapRandom(Villager villager) {
        Random current = villager.getRandom();
        System.out.println("[DEBUG] wrapRandom called for Villager@" + villager.getUUID() + ", current random type: " + current.getClass().getName());
        if (!(current instanceof TrackedRandom)) {
            ((RandomAccessor) villager).setRandom(new TrackedRandom(villager));
            System.out.println("[DEBUG] Wrapped random successfully");
            RNGLogger.log(String.format("[WRAP] Wrapped random for Villager@%s", villager.getUUID()));
        } else {
            System.out.println("[DEBUG] Random already wrapped");
        }
    }
}
