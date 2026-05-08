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
