package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.RandomAccessor;
import hackerrouter.villagerrngdebug.TrackedRandom;
import hackerrouter.villagerrngdebug.VillagerRNGTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class MixinVillagerLoad {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadNbt(ValueInput input, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (VillagerRNGTracker.isTrackedByUUID(villager) && !(villager.getRandom() instanceof TrackedRandom)) {
            long seed = VillagerRNGTracker.getSeed(villager.getRandom());
            ((RandomAccessor) villager).villagerRngDebug$setRandom(new TrackedRandom(villager, seed));
        }
    }

    @Inject(method = "updateTrades", at = @At("HEAD"))
    private void onUpdateTrades(ServerLevel level, CallbackInfo ci) {
        VillagerRNGTracker.onTradeUpdate((Villager) (Object) this);
    }
}
