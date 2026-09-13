package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.client.SoundPacketHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "handleSoundEvent", at = @At("TAIL"))
    private void onHandleSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        SoundPacketHandler.onSoundPacket(
            packet.getSound().value(),
            packet.getX(),
            packet.getY(),
            packet.getZ(),
            packet.getPitch()
        );
    }
}
