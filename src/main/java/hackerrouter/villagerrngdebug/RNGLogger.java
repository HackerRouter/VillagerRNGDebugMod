package hackerrouter.villagerrngdebug;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RNGLogger {
    private static final Set<UUID> chatLogEnabledPlayers = new HashSet<>();
    private static MinecraftServer server;
    private static final Map<UUID, Long> playerStartTicks = new HashMap<>();
    private static volatile boolean updateTradesLogEnabled = false;

    public static void log(LivingEntity owner, CallSite site, String method, Object bound, Object result,
                           long seedBefore, long seedAfter, long callCount, long gameTick) {
        if (owner instanceof Villager villager) {
            sendChatLogToTrackers(villager, gameTick, callCount, method, bound, result, site, seedBefore, seedAfter);
        }
    }

    public static void setServer(MinecraftServer srv) {
        server = srv;
    }

    public static void setUpdateTradesLogEnabled(boolean enabled) {
        updateTradesLogEnabled = enabled;
    }

    public static boolean isUpdateTradesLogEnabled() {
        return updateTradesLogEnabled;
    }

    public static void logUpdateTradesStack(AbstractVillager villager) {
        StringBuilder sb = new StringBuilder();
        sb.append("[UPDATE_TRADES] villager=").append(villager.getUUID())
          .append(" pos=").append(villager.blockPosition());
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stack.length; i++) {
            sb.append("\n    at ").append(stack[i]);
        }
        VillagerRNGDebugMod.LOGGER.info(sb.toString());
    }

    public static void setChatLogEnabled(UUID playerUUID, boolean enabled) {
        if (enabled) {
            chatLogEnabledPlayers.add(playerUUID);
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
                    long startTick = playerStartTicks.getOrDefault(playerUUID, gameTick);
                    long relativeTick = gameTick - startTick;

                    MutableComponent message = Component.literal("[")
                        .append(Component.literal("T=" + relativeTick).withStyle(style -> style.withColor(ChatFormatting.AQUA)))
                        .append(Component.literal("]["))
                        .append(Component.literal("#" + String.format("%05d", callCount)).withStyle(style -> style.withColor(ChatFormatting.YELLOW)))
                        .append(Component.literal("] "))
                        .append(Component.literal(method + "(" + bound + ")=" + result).withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                        .append(Component.literal("   src="))
                        .append(Component.literal(site.toString()).withStyle(style -> style.withColor(
                            site == CallSite.UNKNOWN ? ChatFormatting.RED : ChatFormatting.GREEN)))
                        .append(Component.literal("   seed_pre="))
                        .append(Component.literal(String.format("0x%012X", seedBefore))
                            .withStyle(style -> style
                                .withColor(ChatFormatting.GOLD)
                                .withClickEvent(new ClickEvent.CopyToClipboard(String.format("0x%012X", seedBefore)))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))))
                        .append(Component.literal("  seed_post="))
                        .append(Component.literal(String.format("0x%012X", seedAfter))
                            .withStyle(style -> style
                                .withColor(ChatFormatting.GOLD)
                                .withClickEvent(new ClickEvent.CopyToClipboard(String.format("0x%012X", seedAfter)))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))));

                    player.sendSystemMessage(message);
                }
            }
        }
    }
}
