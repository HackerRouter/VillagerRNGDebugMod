package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.FrostWalkLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(FrostedIceBlock.class)
public class MixinFrostedIce {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(BlockState state, ServerLevel level, BlockPos pos, Random random, CallbackInfo ci) {
        if (state.getValue(FrostedIceBlock.AGE) == 0) {
            FrostWalkLogger.recordFirstTick(pos, level.getGameTime(), level.getServer());
        }
    }
}
