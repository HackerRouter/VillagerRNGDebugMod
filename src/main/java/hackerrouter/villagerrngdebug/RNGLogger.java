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

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RNGLogger {
    private static final Path LOG_DIR = Paths.get("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("vrng_debug.log");
    private static BufferedWriter writer;
    private static final Set<UUID> chatLogEnabledPlayers = new HashSet<>();
    private static MinecraftServer server;
    private static final Map<UUID, Long> playerStartTicks = new HashMap<>();

    public static void init() {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
            writer = Files.newBufferedWriter(LOG_FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log("[INIT] VillagerRNG Debug Logger initialized at " + 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(LivingEntity owner, CallSite site, String method, Object bound, Object result, 
                          long seedBefore, long seedAfter, long callCount, long gameTick) {
        String line = String.format("[T=%d][#%05d] %s(%s)=%s   src=%s   seed_pre=0x%012X  seed_post=0x%012X",
            gameTick, callCount, method, bound, result, site, seedBefore, seedAfter);
        log(line);
        
        if (owner instanceof Villager) {
            sendChatLogToTrackers((Villager) owner, gameTick, callCount, method, bound, result, site, seedBefore, seedAfter);
        }
    }

    public static void log(String message) {
        if (writer != null) {
            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setServer(MinecraftServer srv) {
        server = srv;
    }

    public static void setChatLogEnabled(UUID playerUUID, boolean enabled) {
        if (enabled) {
            chatLogEnabledPlayers.add(playerUUID);
            // 记录开启时的游戏 tick，用于相对计时
            if (server != null) {
                playerStartTicks.put(playerUUID, server.overworld().getGameTime());
            }
        } else {
            chatLogEnabledPlayers.remove(playerUUID);
            playerStartTicks.remove(playerUUID);
        }
    }

    private static void sendChatLogToTrackers(Villager villager, long gameTick, long callCount, 
                                             String method, Object bound, Object result, 
                                             CallSite site, long seedBefore, long seedAfter) {
        if (server == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerUUID = player.getUUID();
            if (chatLogEnabledPlayers.contains(playerUUID)) {
                Villager tracked = VillagerRNGTracker.getTracked(playerUUID);
                if (tracked != null && tracked.getUUID().equals(villager.getUUID())) {
                    // 计算相对 tick（从 log 开启时开始）
                    long startTick = playerStartTicks.getOrDefault(playerUUID, gameTick);
                    long relativeTick = gameTick - startTick;
                    
                    // 使用 Minecraft 的文本组件系统添加颜色和可点击功能
                    net.minecraft.network.chat.MutableComponent message = 
                        new TextComponent("[")
                        .append(new TextComponent("T=" + relativeTick).withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA)))
                        .append("][")
                        .append(new TextComponent("#" + String.format("%05d", callCount)).withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW)))
                        .append("] ")
                        .append(new TextComponent(method + "(" + bound + ")=" + result).withStyle(style -> style.withColor(net.minecraft.ChatFormatting.WHITE)))
                        .append("   src=")
                        .append(new TextComponent(site.toString()).withStyle(style -> style.withColor(
                            site == CallSite.UNKNOWN ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN)))
                        .append("   seed_pre=")
                        .append(new TextComponent(String.format("0x%012X", seedBefore))
                            .withStyle(style -> style
                                .withColor(net.minecraft.ChatFormatting.GOLD)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, 
                                    String.format("0x%012X", seedBefore)))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    new TextComponent("点击复制")))))
                        .append("  seed_post=")
                        .append(new TextComponent(String.format("0x%012X", seedAfter))
                            .withStyle(style -> style
                                .withColor(net.minecraft.ChatFormatting.GOLD)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, 
                                    String.format("0x%012X", seedAfter)))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    new TextComponent("点击复制")))));
                    
                    player.sendMessage(message, playerUUID);
                }
            }
        }
    }
}
