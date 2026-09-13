package hackerrouter.villagerrngdebug.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class SoundPacketHandler {

    private static final ClientRngState state = new ClientRngState();
    private static volatile long lastServerGameTime = -1;
    private static volatile long lastSetTimeNanos = -1;

    public static void onSetTime(long gameTime) {
        lastServerGameTime = gameTime;
        lastSetTimeNanos = System.nanoTime();
    }

    private static long estimateServerTick() {
        if (lastServerGameTime < 0) return -1;
        long elapsedNanos = System.nanoTime() - lastSetTimeNanos;
        long elapsedTicks = elapsedNanos / 50_000_000L;
        return lastServerGameTime + elapsedTicks;
    }

    public static ClientRngState getState() {
        return state;
    }

    public static void onSoundPacket(SoundEvent sound, double x, double y, double z, float pitch) {
        if (!sound.location().equals(SoundEvents.VILLAGER_AMBIENT.location())) {
            return;
        }

        if (state.getTargetUuid() == null || state.getState() != ClientRngState.State.COLLECTING) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

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

        if (state.getTriggerCount() > 0) {
            long lastTick = state.getTriggerTick(state.getTriggerCount() - 1);
            long delta = currentTick - lastTick;
            if (delta < 81) {
                sendMessage(Component.literal("[VRNGC] ")
                    .append(Component.literal(String.format(
                        "Clock jump detected (delta=%d < 81). Restarting collection.", delta))
                        .withStyle(style -> style.withColor(ChatFormatting.RED))));
                state.resetCollection();
            }
        }

        state.recordTrigger(currentTick);
        int count = state.getTriggerCount();

        if (!state.isCollectionComplete()) {
            sendMessage(Component.literal("[VRNGC] ")
                .append(Component.literal(String.format("Sound #%d at tick=%d (%d/%d)",
                    count, currentTick, count, ClientRngState.REQUIRED_OBSERVATIONS))
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW))));
        } else {
            long[] ticks = state.getTriggerTicks();
            long delta = ticks[ticks.length - 1] - ticks[0];

            sendMessage(Component.literal("[VRNGC] ")
                .append(Component.literal(String.format(
                    "Collected %d sounds over %d ticks. Cracking...",
                    count, delta))
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA))));

            state.setState(ClientRngState.State.CRACKING);

            new Thread(() -> {
                long t0 = System.currentTimeMillis();
                long[] seeds = PitchCracker.crack(ticks);
                long ms = System.currentTimeMillis() - t0;

                Minecraft.getInstance().execute(() -> {
                    if (seeds != null) {
                        state.setCrackedSeeds(seeds[0], seeds[1]);
                        MutableComponent msg = Component.literal("[VRNGC] ")
                            .append(Component.literal(String.format("Cracked in %dms! ", ms))
                                .withStyle(s -> s.withColor(ChatFormatting.GREEN)))
                            .append(Component.literal("start="))
                            .append(Component.literal(String.format("0x%012X", seeds[0]))
                                .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                                    .withClickEvent(new ClickEvent.CopyToClipboard(String.format("0x%012X", seeds[0])))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))))
                            .append(Component.literal("  end="))
                            .append(Component.literal(String.format("0x%012X", seeds[1]))
                                .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                                    .withClickEvent(new ClickEvent.CopyToClipboard(String.format("0x%012X", seeds[1])))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))));
                        sendMessage(msg);
                    } else {
                        sendMessage(Component.literal("[VRNGC] ")
                            .append(Component.literal(String.format("No solution found (%dms). Resetting...", ms))
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
            mc.player.displayClientMessage(message, false);
        }
    }
}
