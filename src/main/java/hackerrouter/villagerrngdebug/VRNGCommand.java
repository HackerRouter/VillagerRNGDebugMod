/*
 * This file is part of the VillagerRNGDebugMod project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * VillagerRNGDebugMod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VillagerRNGDebugMod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VillagerRNGDebugMod.  If not, see <https://www.gnu.org/licenses/>.
 */

package hackerrouter.villagerrngdebug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
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
            .then(Commands.literal("frosted_ice")
                .then(Commands.literal("on").executes(VRNGCommand::frostedIceOn))
                .then(Commands.literal("off").executes(VRNGCommand::frostedIceOff)))
        );
    }

    private static int track(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();
        AABB box = player.getBoundingBox().inflate(10);
        List<Villager> villagers = player.level.getEntitiesOfClass(Villager.class, box);
        
        if (villagers.isEmpty()) {
            ctx.getSource().sendFailure(new TextComponent("No villager found nearby"));
            return 0;
        }
        
        Villager closest = villagers.get(0);
        VillagerRNGTracker.setTracked(playerUUID, closest);
        VillagerRNGTracker.wrapRandom(closest);
        ctx.getSource().sendSuccess(new TextComponent("Now tracking Villager@" + closest.getUUID()), false);
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
            ctx.getSource().sendFailure(new TextComponent("Invalid UUID format"));
            return 0;
        }
        
        Entity entity = player.getLevel().getEntity(uuid);
        
        if (!(entity instanceof Villager)) {
            ctx.getSource().sendFailure(new TextComponent("Entity not found or not a villager"));
            return 0;
        }
        
        Villager villager = (Villager) entity;
        VillagerRNGTracker.setTracked(playerUUID, villager);
        VillagerRNGTracker.wrapRandom(villager);
        ctx.getSource().sendSuccess(new TextComponent("Now tracking Villager@" + villager.getUUID()), false);
        return 1;
    }

    private static int untrack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        VillagerRNGTracker.clearTracked(player.getUUID());
        ctx.getSource().sendSuccess(new TextComponent("Stopped tracking"), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Villager v = VillagerRNGTracker.getTracked(player.getUUID());
        if (v == null) {
            ctx.getSource().sendFailure(new TextComponent("No villager tracked"));
            return 0;
        }
        long seed = VillagerRNGTracker.getSeed(v.getRandom());
        ctx.getSource().sendSuccess(
            new TextComponent("Villager@" + v.getUUID() + " seed=")
                .append(new TextComponent(String.format("0x%012X", seed))
                    .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD))),
            false);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int steps = IntegerArgumentType.getInteger(ctx, "steps");
        VillagerRNGTracker.advance(player.getUUID(), steps);
        ctx.getSource().sendSuccess(new TextComponent("Advanced " + steps + " steps"), false);
        return 1;
    }

    private static int advanceTo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String seedStr = StringArgumentType.getString(ctx, "seed");
        long target = parseSeed(seedStr);
        boolean success = VillagerRNGTracker.advanceTo(player.getUUID(), target, 1048576);
        if (success) {
            ctx.getSource().sendSuccess(new TextComponent(String.format("Advanced to seed 0x%012X", target)), false);
        } else {
            ctx.getSource().sendFailure(new TextComponent("Failed to reach target seed"));
        }
        return success ? 1 : 0;
    }

    private static int setSeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String seedStr = StringArgumentType.getString(ctx, "seed");
        long seed = parseSeed(seedStr);
        Villager v = VillagerRNGTracker.getTracked(player.getUUID());
        if (v == null) {
            ctx.getSource().sendFailure(new TextComponent("No villager tracked"));
            return 0;
        }
        long old = VillagerRNGTracker.getSeed(v.getRandom());
        VillagerRNGTracker.setSeed(v.getRandom(), seed);
        RNGLogger.log(String.format("[MANIPULATE] Player %s set-seed  old=0x%012X  new=0x%012X", player.getUUID(), old, seed));
        ctx.getSource().sendSuccess(
            new TextComponent("Set seed to ")
                .append(new TextComponent(String.format("0x%012X", seed))
                    .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD))),
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
        ctx.getSource().sendSuccess(new TextComponent("VillagerRNGDebug logging enabled"), false);
        return 1;
    }

    private static int logOff(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RNGLogger.setChatLogEnabled(player.getUUID(), false);
        ctx.getSource().sendSuccess(new TextComponent("VillagerRNGDebug logging disabled"), false);
        return 1;
    }

    private static int frostedIceOn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FrostWalkLogger.setEnabled(true, ctx.getSource().getServer(), player.getUUID());
        return 1;
    }

    private static int frostedIceOff(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FrostWalkLogger.setEnabled(false, ctx.getSource().getServer(), player.getUUID());
        return 1;
    }
}
