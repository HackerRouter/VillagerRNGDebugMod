package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.RNGLogger;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Villager.class, WanderingTrader.class})
public class MixinAbstractVillager {

    @Inject(method = "updateTrades", at = @At("HEAD"))
    private void onUpdateTrades(CallbackInfo ci) {
        if (RNGLogger.isUpdateTradesLogEnabled()) {
            RNGLogger.logUpdateTradesStack((AbstractVillager) (Object) this);
        }
    }
}
