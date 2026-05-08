package hackerrouter.villagerrngdebug;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;

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
        if (owner instanceof Villager) {
            sendChatLogToTrackers((Villager) owner, gameTick, callCount, method, bound, result, site, seedBefore, seedAfter);
        }
    }

    /** No-op kept for call-site compatibility. */
    public static void log(String message) {}

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
                                    new TextComponent("Click to copy")))))
                        .append("  seed_post=")
                        .append(new TextComponent(String.format("0x%012X", seedAfter))
                            .withStyle(style -> style
                                .withColor(net.minecraft.ChatFormatting.GOLD)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD,
                                    String.format("0x%012X", seedAfter)))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    new TextComponent("Click to copy")))));

                    player.sendMessage(message, playerUUID);
                }
            }
        }
    }
}
