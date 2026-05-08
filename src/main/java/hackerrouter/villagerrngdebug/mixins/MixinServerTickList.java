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
