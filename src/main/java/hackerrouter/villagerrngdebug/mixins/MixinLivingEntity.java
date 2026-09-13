package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.RandomAccessor;
import hackerrouter.villagerrngdebug.TrackedRandom;
import hackerrouter.villagerrngdebug.VillagerRNGTracker;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinLivingEntity implements RandomAccessor {
    @Shadow @Final @Mutable
    protected RandomSource random;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void wrapRandom(CallbackInfo ci) {
        if ((Object) this instanceof Villager villager) {
            if (VillagerRNGTracker.isTracked(villager)) {
                long seed = VillagerRNGTracker.getSeed(this.random);
                this.random = new TrackedRandom(villager, seed);
            }
        }
    }

    @Override
    public void villagerRngDebug$setRandom(RandomSource random) {
        this.random = random;
    }
}
