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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerTickList.class)
public class MixinServerTickList {
    @Inject(method = "scheduleTick(Lnet/minecraft/core/BlockPos;Ljava/lang/Object;ILnet/minecraft/world/level/TickPriority;)V", at = @At("HEAD"))
    private void onScheduleTick(BlockPos pos, Object type, int delay, TickPriority priority, CallbackInfo ci) {
        if (type == Blocks.FROSTED_ICE) {
            FrostWalkLogger.onScheduleFrostedIceTick(pos, delay);
        }
    }
}
