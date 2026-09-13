package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.client.SoundPacketHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinSetTime {

    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void onHandleSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        SoundPacketHandler.onSetTime(packet.gameTime());
    }
}
