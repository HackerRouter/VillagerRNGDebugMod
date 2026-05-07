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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Entity.class)
public abstract class MixinLivingEntity implements RandomAccessor {
    @Shadow @Mutable public Random random;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void wrapRandom(CallbackInfo ci) {
        if ((Object)this instanceof Villager) {
            Villager villager = (Villager)(Object)this;
            if (VillagerRNGTracker.isTracked(villager)) {
                this.random = new TrackedRandom(villager);
            }
        }
    }

    @Override
    public void setRandom(Random random) {
        this.random = random;
    }
}
