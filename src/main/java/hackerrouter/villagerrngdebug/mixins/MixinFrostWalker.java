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

package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.FrostWalkLogger;
import hackerrouter.villagerrngdebug.TrackedRandom;
import hackerrouter.villagerrngdebug.VillagerRNGTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrostWalkerEnchantment.class)
public class MixinFrostWalker {
    /**
     * Inject at TAIL. At this point all ice blocks have been placed and all
     * scheduleTick calls (each consuming one nextInt) have fired.
     * TrackedRandom records each (pos, delay) pair via onFrostWalkPlacement()
     * which is called from MixinServerTickList when scheduleTick is invoked
     * while a frost-walk placement is in progress.
     *
     * We set the "active entity" on FrostWalkLogger at HEAD so MixinServerTickList
     * knows which entity's random to read from.
     */
    @Inject(method = "onEntityMoved", at = @At("HEAD"))
    private static void onFrostWalkHead(LivingEntity entity, Level level, BlockPos pos, int enchantLevel, CallbackInfo ci) {
        if (!FrostWalkLogger.isEnabled()) return;
        if (entity instanceof Villager && VillagerRNGTracker.isTracked((Villager) entity)
                && entity.getRandom() instanceof TrackedRandom) {
            FrostWalkLogger.beginFrostWalk((Villager) entity, level);
        }
    }

    @Inject(method = "onEntityMoved", at = @At("TAIL"))
    private static void onFrostWalkTail(LivingEntity entity, Level level, BlockPos pos, int enchantLevel, CallbackInfo ci) {
        FrostWalkLogger.endFrostWalk();
    }
}
