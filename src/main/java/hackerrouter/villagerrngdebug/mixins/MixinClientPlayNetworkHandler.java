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

    // @At("TAIL") only runs on the main-thread invocation; the network-thread call
    // throws RunningOnDifferentThreadException before reaching it.
    @Inject(method = "handleSoundEvent", at = @At("TAIL"))
    private void onHandleSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        SoundPacketHandler.onSoundPacket(
            packet.getSound(),
            packet.getX(),
            packet.getY(),
            packet.getZ(),
            packet.getPitch()
        );
    }
}
