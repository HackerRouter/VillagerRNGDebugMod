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

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class FrostWalkLogger {
    private static final Map<BlockPos, FrostIceData> iceBlocks = new HashMap<>();

    public static void recordPlacement(BlockPos pos, long placeTick, int delay) {
        iceBlocks.put(pos, new FrostIceData(placeTick, delay));
        RNGLogger.log(String.format("[T=%d] >>> FROST_WALK  pos=(%d,%d,%d)  tile_delay=%d", 
            placeTick, pos.getX(), pos.getY(), pos.getZ(), delay));
    }

    public static void recordTick(BlockPos pos, long currentTick, int age) {
        FrostIceData data = iceBlocks.get(pos);
        if (data != null && age == 0) {
            long elapsed = currentTick - data.placeTick;
            String status = (elapsed == data.delay) ? "✓" : "✗";
            RNGLogger.log(String.format("[T=%d] FROST_ICE_TICK  pos=(%d,%d,%d)  age=0->1  elapsed=%d  expected=%d  %s",
                currentTick, pos.getX(), pos.getY(), pos.getZ(), elapsed, data.delay, status));
        }
    }

    private static class FrostIceData {
        final long placeTick;
        final int delay;

        FrostIceData(long placeTick, int delay) {
            this.placeTick = placeTick;
            this.delay = delay;
        }
    }
}
