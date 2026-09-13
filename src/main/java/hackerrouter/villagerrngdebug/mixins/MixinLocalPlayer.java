package hackerrouter.villagerrngdebug.mixins;

import hackerrouter.villagerrngdebug.client.VRNGClientCommand;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(method = "chat", at = @At("HEAD"), cancellable = true)
    private void onChat(String message, CallbackInfo ci) {
        if (message.startsWith("/vrngc")) {
            if (VRNGClientCommand.executeCommand(message)) {
                ci.cancel();
            }
        }
    }
}
