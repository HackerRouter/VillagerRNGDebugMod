package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.client.VRNGClientCommand;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinLocalPlayer {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo ci) {
        if (command.startsWith("vrngc")) {
            if (VRNGClientCommand.executeCommand("/" + command)) {
                ci.cancel();
            }
        }
    }
}
