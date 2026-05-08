package hackerrouter.villagerrngdebug.mixins;

import com.mojang.brigadier.CommandDispatcher;
import hackerrouter.villagerrngdebug.VRNGCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class MixinCommands {
    @Shadow private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void registerCommands(Commands.CommandSelection selection, CallbackInfo ci) {
        VRNGCommand.register(this.dispatcher);
    }
}
