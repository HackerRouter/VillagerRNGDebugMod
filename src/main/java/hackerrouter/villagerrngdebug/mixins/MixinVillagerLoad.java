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

import hackerrouter.villagerrngdebug.RandomAccessor;
import hackerrouter.villagerrngdebug.TrackedRandom;
import hackerrouter.villagerrngdebug.VillagerRNGTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into Villager.readAdditionalSaveData TAIL.
 * At this point the entity UUID has been loaded from NBT (via super chain),
 * so isTrackedByUUID can correctly match and re-wrap the random.
 */
@Mixin(Villager.class)
public class MixinVillagerLoad {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadNbt(CompoundTag tag, CallbackInfo ci) {
        Villager villager = (Villager)(Object)this;
        if (VillagerRNGTracker.isTrackedByUUID(villager) && !(villager.getRandom() instanceof TrackedRandom)) {
            ((RandomAccessor) villager).setRandom(new TrackedRandom(villager));
        }
    }
}
