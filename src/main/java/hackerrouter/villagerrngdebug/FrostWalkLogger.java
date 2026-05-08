package hackerrouter.villagerrngdebug;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FrostWalkLogger {
    private static boolean enabled = false;
    private static int counter = 0;
    private static UUID ownerUUID = null;
    private static long startTick = 0;
    private static final Map<BlockPos, FrostIceData> iceBlocks = new HashMap<>();

    private static Villager activeVillager = null;
    private static Level activeLevel = null;

    public static void setEnabled(boolean on, MinecraftServer server, UUID playerUUID) {
        enabled = on;
        ownerUUID = on ? playerUUID : null;
        if (on) {
            counter = 0;
            iceBlocks.clear();
            startTick = server != null ? server.overworld().getGameTime() : 0;
            sendToOwner(server, new TextComponent("FrostIce logging enabled").withStyle(ChatFormatting.WHITE));
        } else {
            sendToOwner(server, new TextComponent("FrostIce logging disabled").withStyle(ChatFormatting.WHITE));
        }
    }

    public static boolean isEnabled() { return enabled; }

    /** Called at HEAD of FrostWalkerEnchantment.onEntityMoved. */
    public static void beginFrostWalk(Villager villager, Level level) {
        activeVillager = villager;
        activeLevel = level;
    }

    /** Called at TAIL of FrostWalkerEnchantment.onEntityMoved. */
    public static void endFrostWalk() {
        activeVillager = null;
        activeLevel = null;
    }

    /** Called from MixinServerTickList on scheduleTick(FROSTED_ICE). delay = nextInt(61) + 60. */
    public static void onScheduleFrostedIceTick(BlockPos pos, int delay) {
        if (!enabled || activeVillager == null || activeLevel == null) return;
        if (!(activeVillager.getRandom() instanceof TrackedRandom)) return;
        int id = ++counter;
        iceBlocks.put(pos.immutable(), new FrostIceData(activeLevel.getGameTime(), delay, id));
        String prefix = String.format("[T=%d][#%d] FROST_PLACE  pos=(%d,%d,%d)  first_tick_delay=",
                activeLevel.getGameTime() - startTick, id, pos.getX(), pos.getY(), pos.getZ());
        String delayStr = String.valueOf(delay);
        String msg = prefix + delayStr;
        RNGLogger.log(msg);
        net.minecraft.network.chat.MutableComponent chat = new TextComponent(prefix).withStyle(ChatFormatting.BLUE)
                .append(new TextComponent(delayStr).withStyle(style -> style
                        .withColor(ChatFormatting.BLUE)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, delayStr))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                new TextComponent("Click to copy")))));
        sendToOwner(activeLevel.getServer(), chat);
    }

    /** Called on first tile tick of a frosted ice block (age 0 → 1). */
    public static void recordFirstTick(BlockPos pos, long currentTick, MinecraftServer server) {
        if (!enabled) return;
        FrostIceData data = iceBlocks.remove(pos);
        if (data == null) return;
        long elapsed = currentTick - data.placeTick;
        boolean match = elapsed == data.delay;
        String verdict = match ? "CORRECT" : "WRONG";
        String msg = String.format("[T=%d][#%d] FROST_TICK1  pos=(%d,%d,%d)  elapsed=%d  expected=%d  %s",
                currentTick - startTick, data.id, pos.getX(), pos.getY(), pos.getZ(), elapsed, data.delay, verdict);
        RNGLogger.log(msg);
        String baseMsg = String.format("[T=%d][#%d] FROST_TICK1  pos=(%d,%d,%d)  elapsed=%d  expected=%d  ",
                currentTick - startTick, data.id, pos.getX(), pos.getY(), pos.getZ(), elapsed, data.delay);
        net.minecraft.network.chat.MutableComponent chat = new TextComponent(baseMsg).withStyle(ChatFormatting.BLUE)
                .append(new TextComponent(verdict).withStyle(match ? ChatFormatting.GREEN : ChatFormatting.RED));
        sendToOwner(server, chat);
    }

    private static void sendToOwner(MinecraftServer server, net.minecraft.network.chat.Component msg) {
        if (server == null || ownerUUID == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(ownerUUID);
        if (p != null) p.sendMessage(msg, ownerUUID);
    }

    private static class FrostIceData {
        final long placeTick;
        final int delay;
        final int id;
        FrostIceData(long placeTick, int delay, int id) {
            this.placeTick = placeTick;
            this.delay = delay;
            this.id = id;
        }
    }
}
