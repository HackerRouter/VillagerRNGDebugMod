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

/** Injects into Villager.readAdditionalSaveData TAIL to re-wrap random after UUID is loaded from NBT. */
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
