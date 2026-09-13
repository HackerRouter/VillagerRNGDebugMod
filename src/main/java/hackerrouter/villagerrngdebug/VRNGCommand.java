package hackerrouter.villagerrngdebug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class VRNGCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vrng")
            .then(Commands.literal("track")
                .executes(VRNGCommand::track)
                .then(Commands.argument("uuid", StringArgumentType.string())
                    .executes(VRNGCommand::trackByUuid)))
            .then(Commands.literal("untrack").executes(VRNGCommand::untrack))
            .then(Commands.literal("status").executes(VRNGCommand::status))
            .then(Commands.literal("log")
                .then(Commands.literal("on").executes(VRNGCommand::logOn))
                .then(Commands.literal("off").executes(VRNGCommand::logOff)))
            .then(Commands.literal("advance")
                .then(Commands.argument("steps", IntegerArgumentType.integer(1))
                    .executes(VRNGCommand::advance)))
            .then(Commands.literal("advance-to")
                .then(Commands.argument("seed", StringArgumentType.string())
                    .executes(VRNGCommand::advanceTo)))
            .then(Commands.literal("set-seed")
                .then(Commands.argument("seed", StringArgumentType.string())
                    .executes(VRNGCommand::setSeed)))
            .then(Commands.literal("update_trades")
                .then(Commands.literal("on").executes(VRNGCommand::updateTradesOn))
                .then(Commands.literal("off").executes(VRNGCommand::updateTradesOff)))
        );
    }

    private static int track(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();
        AABB box = player.getBoundingBox().inflate(10);
        List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, box);

        if (villagers.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No villager found nearby"));
            return 0;
        }

        Villager closest = villagers.get(0);
        VillagerRNGTracker.setTracked(playerUUID, closest);
        VillagerRNGTracker.wrapRandom(closest);
        ctx.getSource().sendSuccess(() -> Component.literal("Now tracking Villager@" + closest.getUUID()), false);
        return 1;
    }

    private static int trackByUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();
        String uuidStr = StringArgumentType.getString(ctx, "uuid");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid UUID format"));
            return 0;
        }

        Entity entity = player.level().getEntity(uuid);

        if (!(entity instanceof Villager villager)) {
            ctx.getSource().sendFailure(Component.literal("Entity not found or not a villager"));
            return 0;
        }

        VillagerRNGTracker.setTracked(playerUUID, villager);
        VillagerRNGTracker.wrapRandom(villager);
        ctx.getSource().sendSuccess(() -> Component.literal("Now tracking Villager@" + villager.getUUID()), false);
        return 1;
    }

    private static int untrack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        VillagerRNGTracker.clearTracked(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("Stopped tracking"), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Villager v = VillagerRNGTracker.getTracked(player.getUUID());
        if (v == null) {
            ctx.getSource().sendFailure(Component.literal("No villager tracked"));
            return 0;
        }
        long seed = VillagerRNGTracker.getSeed(v.getRandom());
        ctx.getSource().sendSuccess(() ->
            Component.literal("Villager@" + v.getUUID() + " seed=")
                .append(Component.literal(String.format("0x%012X", seed))
                    .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withClickEvent(new ClickEvent.CopyToClipboard(String.format("0x%012X", seed)))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))))),
            false);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int steps = IntegerArgumentType.getInteger(ctx, "steps");
        VillagerRNGTracker.advance(player.getUUID(), steps);
        ctx.getSource().sendSuccess(() -> Component.literal("Advanced " + steps + " steps"), false);
        return 1;
    }

    private static int advanceTo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String seedStr = StringArgumentType.getString(ctx, "seed");
        long target = parseSeed(seedStr);
        boolean success = VillagerRNGTracker.advanceTo(player.getUUID(), target, 1048576);
        if (success) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format("Advanced to seed 0x%012X", target)), false);
        } else {
            ctx.getSource().sendFailure(Component.literal("Failed to reach target seed"));
        }
        return success ? 1 : 0;
    }

    private static int setSeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String seedStr = StringArgumentType.getString(ctx, "seed");
        long seed = parseSeed(seedStr);
        Villager v = VillagerRNGTracker.getTracked(player.getUUID());
        if (v == null) {
            ctx.getSource().sendFailure(Component.literal("No villager tracked"));
            return 0;
        }
        VillagerRNGTracker.setSeed(v.getRandom(), seed);
        ctx.getSource().sendSuccess(() ->
            Component.literal("Set seed to ")
                .append(Component.literal(String.format("0x%012X", seed))
                    .withStyle(style -> style.withColor(ChatFormatting.GOLD))),
            false);
        return 1;
    }

    private static long parseSeed(String input) {
        if (input.startsWith("0x") || input.startsWith("0X")) {
            return Long.parseLong(input.substring(2), 16);
        }
        return Long.parseLong(input);
    }

    private static int logOn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RNGLogger.setChatLogEnabled(player.getUUID(), true);
        ctx.getSource().sendSuccess(() -> Component.literal("VillagerRNGDebug logging enabled"), false);
        return 1;
    }

    private static int logOff(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RNGLogger.setChatLogEnabled(player.getUUID(), false);
        ctx.getSource().sendSuccess(() -> Component.literal("VillagerRNGDebug logging disabled"), false);
        return 1;
    }

    private static int updateTradesOn(CommandContext<CommandSourceStack> ctx) {
        RNGLogger.setUpdateTradesLogEnabled(true);
        ctx.getSource().sendSuccess(() -> Component.literal("Update Trades stack logging enabled"), false);
        return 1;
    }

    private static int updateTradesOff(CommandContext<CommandSourceStack> ctx) {
        RNGLogger.setUpdateTradesLogEnabled(false);
        ctx.getSource().sendSuccess(() -> Component.literal("Update Trades stack logging disabled"), false);
        return 1;
    }
}
