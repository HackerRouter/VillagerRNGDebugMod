package hackerrouter.villagerrngdebug.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.UUID;

/**
 * Client-side command /vrngc for RNG cracking.
 */
public class VRNGClientCommand {

    public static void register(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("vrngc")
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("track")
                    .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("uuid", StringArgumentType.string())))
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("status"))
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("reset"))
        );
    }

    public static boolean executeCommand(String message) {
        String[] parts = message.trim().split("\\s+");
        if (parts.length < 1 || !parts[0].equals("/vrngc")) return false;

        if (parts.length < 2) {
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent("Usage: /vrngc <track|status|reset>")
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY))));
            return true;
        }

        switch (parts[1]) {
            case "track":
                if (parts.length >= 3) {
                    trackByUuid(parts[2]);
                } else {
                    track();
                }
                return true;
            case "status":
                status();
                return true;
            case "reset":
                reset();
                return true;
            default:
                sendMessage(new TextComponent("[VRNGC] ")
                    .append(new TextComponent("Unknown subcommand: " + parts[1])
                        .withStyle(style -> style.withColor(ChatFormatting.RED))));
                return true;
        }
    }

    private static void track() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        // Priority 1: crosshair target
        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            Entity targetEntity = ((EntityHitResult) hitResult).getEntity();
            if (targetEntity instanceof Villager) {
                Villager villager = (Villager) targetEntity;
                ClientRngState state = SoundPacketHandler.getState();
                state.setTarget(villager.getUUID());
                sendMessage(new TextComponent("[VRNGC] ")
                    .append(new TextComponent("Tracking Villager@" + villager.getUUID() + ", collecting sounds (0/" + ClientRngState.REQUIRED_OBSERVATIONS + ")...")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN))));
                return;
            }
        }

        // Priority 2: nearest villager within 10 blocks
        AABB box = player.getBoundingBox().inflate(10);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, box, e -> true);

        if (villagers.isEmpty()) {
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent("No villager found nearby or in crosshair")
                    .withStyle(style -> style.withColor(ChatFormatting.RED))));
            return;
        }

        Villager closest = villagers.get(0);
        double closestDist = player.distanceToSqr(closest);
        for (int i = 1; i < villagers.size(); i++) {
            double dist = player.distanceToSqr(villagers.get(i));
            if (dist < closestDist) {
                closest = villagers.get(i);
                closestDist = dist;
            }
        }

        ClientRngState state = SoundPacketHandler.getState();
        state.setTarget(closest.getUUID());
        sendMessage(new TextComponent("[VRNGC] ")
            .append(new TextComponent("Tracking Villager@" + closest.getUUID() + ", collecting sounds (0/" + ClientRngState.REQUIRED_OBSERVATIONS + ")...")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN))));
    }

    private static void trackByUuid(String uuidStr) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent("Invalid UUID format")
                    .withStyle(style -> style.withColor(ChatFormatting.RED))));
            return;
        }

        ClientRngState state = SoundPacketHandler.getState();
        state.setTarget(uuid);
        sendMessage(new TextComponent("[VRNGC] ")
            .append(new TextComponent("Tracking Villager@" + uuid + ", collecting sounds (0/" + ClientRngState.REQUIRED_OBSERVATIONS + ")...")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN))));
    }

    private static void status() {
        ClientRngState state = SoundPacketHandler.getState();
        UUID target = state.getTargetUuid();

        if (target == null) {
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent("No villager tracked")
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY))));
            return;
        }

        MutableComponent msg = new TextComponent("[VRNGC] ")
            .append(new TextComponent("Villager@" + target + " "));

        switch (state.getState()) {
            case COLLECTING:
                msg.append(new TextComponent(String.format("collecting (%d/%d)",
                    state.getTriggerCount(), ClientRngState.REQUIRED_OBSERVATIONS))
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW)));
                if (state.getTriggerCount() > 0) {
                    long firstTick = state.getTriggerTick(0);
                    long lastTick = state.getTriggerTick(state.getTriggerCount() - 1);
                    msg.append(new TextComponent(String.format(" [ticks %d-%d]", firstTick, lastTick))
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY)));
                }
                break;
            case CRACKING:
                msg.append(new TextComponent("cracking...")
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA)));
                break;
            case CRACKED:
                msg.append(new TextComponent("start="))
                    .append(new TextComponent(String.format("0x%012X", state.getCrackedSeedStart()))
                        .withStyle(style -> style
                            .withColor(ChatFormatting.GOLD)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.format("0x%012X", state.getCrackedSeedStart())))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to copy")))))
                    .append(new TextComponent("  end="))
                    .append(new TextComponent(String.format("0x%012X", state.getCrackedSeedEnd()))
                        .withStyle(style -> style
                            .withColor(ChatFormatting.GOLD)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.format("0x%012X", state.getCrackedSeedEnd())))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to copy")))));
                break;
            default:
                msg.append(new TextComponent("idle")
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY)));
                break;
        }

        sendMessage(msg);
    }

    private static void reset() {
        ClientRngState state = SoundPacketHandler.getState();
        if (state.getTargetUuid() != null) {
            state.setTarget(state.getTargetUuid());
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent("Reset. Collecting sounds (0/" + ClientRngState.REQUIRED_OBSERVATIONS + ")...")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN))));
        } else {
            sendMessage(new TextComponent("[VRNGC] ")
                .append(new TextComponent("No villager tracked")
                    .withStyle(style -> style.withColor(ChatFormatting.RED))));
        }
    }

    private static void sendMessage(MutableComponent message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(message, mc.player.getUUID());
        }
    }
}
