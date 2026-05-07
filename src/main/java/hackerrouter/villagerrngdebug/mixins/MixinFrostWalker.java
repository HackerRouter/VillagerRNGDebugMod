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
    @Inject(method = "onEntityMoved", at = @At("TAIL"))
    private static void afterFrostWalk(LivingEntity entity, Level level, BlockPos pos, int enchantLevel, CallbackInfo ci) {
        if (entity instanceof Villager && VillagerRNGTracker.isTracked((Villager)entity)) {
            if (entity.getRandom() instanceof TrackedRandom) {
                int lastResult = ((TrackedRandom)entity.getRandom()).getLastResult();
                int delay = lastResult + 60;
                FrostWalkLogger.recordPlacement(pos, level.getGameTime(), delay);
            }
        }
    }
}
