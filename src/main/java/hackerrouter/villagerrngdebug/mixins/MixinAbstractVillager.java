package hackerrouter.villagerrngdebug.mixins;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractVillager.class)
public abstract class MixinAbstractVillager {
    @Shadow
    private MerchantOffers offers;
}
