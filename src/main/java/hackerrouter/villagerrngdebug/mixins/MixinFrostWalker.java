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
