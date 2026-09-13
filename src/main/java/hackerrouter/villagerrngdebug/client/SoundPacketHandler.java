package hackerrouter.villagerrngdebug.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * Handles incoming sound packets for RNG cracking.
 * Collects 10 ambient sound trigger ticks, then cracks using interval constraints.
 */
public class SoundPacketHandler {

    private static final ClientRngState state = new ClientRngState();
    private static volatile long lastServerGameTime = -1;
    private static volatile long lastSetTimeNanos = -1;

    /** Called by MixinSetTime when SetTimePacket arrives. */
    public static void onSetTime(long gameTime) {
        lastServerGameTime = gameTime;
        lastSetTimeNanos = System.nanoTime();
    }

    /** Estimate current server game tick via interpolation. */
    private static long estimateServerTick() {
        if (lastServerGameTime < 0) return -1;
        long elapsedNanos = System.nanoTime() - lastSetTimeNanos;
        long elapsedTicks = elapsedNanos / 50_000_000L; // 50ms per tick
        return lastServerGameTime + elapsedTicks;
    }

    public static ClientRngState getState() {
        return state;
    }

    /**
     * Called from the ClientPlayNetworkHandler mixin when a sound packet is received.
     */
    public static void onSoundPacket(SoundEvent sound, double x, double y, double z, float pitch) {
        // Only process villager ambient sounds
        if (sound != SoundEvents.VILLAGER_AMBIENT) {
            return;
        }

        if (state.getTargetUuid() == null || state.getState() != ClientRngState.State.COLLECTING) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        // Match sound position to target villager
        UUID targetUuid = state.getTargetUuid();
        AABB box = new AABB(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2);
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, box, e -> true);

        boolean matchesTarget = false;
        for (Villager v : nearby) {
            if (v.getUUID().equals(targetUuid)) {
                matchesTarget = true;
                break;
            }
        }

        if (!matchesTarget) return;

        long currentTick = estimateServerTick();
        if (currentTick < 0) currentTick = level.getGameTime();

        // Triggers are at least 81 ticks apart; a smaller delta means the client clock jumped.
        if (state.getTriggerCount() > 0) {
            long lastTick = state.getTriggerTick(state.getTriggerCount() - 1);
            long delta = currentTick - lastTick;
            if (delta < 81) {
                sendMessage(new TextComponent("[VRNGC] ")
                    .append(new TextComponent(String.format(
                        "Clock jump detected (delta=%d < 81). Restarting collection.", delta))
                        .withStyle(style -> style.withColor(ChatFormatting.RED))));
                state.resetCollection();
                // Record this trigger as the new first observation.
            }
        }

        state.recordTrigger(currentTick);
        int count = state.getTriggerCount();

        if (!state.isCollectionComplete()) {
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent(String.format("Sound #%d at tick=%d (%d/%d)",
                    count, currentTick, count, ClientRngState.REQUIRED_OBSERVATIONS))
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW))));
        } else {
            // All observations collected.
            long[] ticks = state.getTriggerTicks();
            long delta = ticks[ticks.length - 1] - ticks[0];

            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent(String.format(
                    "Collected %d sounds over %d ticks. Cracking...",
                    count, delta))
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA))));

            state.setState(ClientRngState.State.CRACKING);

            // Run cracking in a separate thread
            new Thread(() -> {
                long t0 = System.currentTimeMillis();
                long[] seeds = PitchCracker.crack(ticks);
                long ms = System.currentTimeMillis() - t0;

                Minecraft.getInstance().execute(() -> {
                    if (seeds != null) {
                        state.setCrackedSeeds(seeds[0], seeds[1]);
                        MutableComponent msg = new TextComponent("[VRNGC] ")
                            .append(new TextComponent(String.format("Cracked in %dms! ", ms))
                                .withStyle(s -> s.withColor(ChatFormatting.GREEN)))
                            .append(new TextComponent("start="))
                            .append(new TextComponent(String.format("0x%012X", seeds[0]))
                                .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.format("0x%012X", seeds[0])))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to copy")))))
                            .append(new TextComponent("  end="))
                            .append(new TextComponent(String.format("0x%012X", seeds[1]))
                                .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.format("0x%012X", seeds[1])))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to copy")))));
                        sendMessage(msg);
                    } else {
                        sendMessage(new TextComponent("[VRNGC] ")
                            .append(new TextComponent(String.format("No solution found (%dms). Resetting...", ms))
                                .withStyle(s -> s.withColor(ChatFormatting.RED))));
                        state.resetCollection();
                        state.setState(ClientRngState.State.COLLECTING);
                    }
                });
            }, "VRNGC-Cracker").start();
        }
    }

    private static void sendMessage(MutableComponent message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(message, mc.player.getUUID());
        }
    }
}
