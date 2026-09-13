package hackerrouter.villagerrngdebug;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

public class TrackedRandom extends LegacyRandomSource {
    private final LivingEntity owner;
    private long callCount = 0;
    private int tickCallCount = 0;
    private long lastGameTick = -1;
    private CallSite currentSite = CallSite.UNKNOWN;
    private boolean stackDumped = false;

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public TrackedRandom(LivingEntity owner, long seed) {
        super(seed);
        this.owner = owner;
    }

    public void resetTickCount() {
        tickCallCount = 0;
    }

    private CallSite detectCallSite(String method, Object bound) {
        Level level = owner.level();
        long currentTick = level.getGameTime();
        if (currentTick != lastGameTick) {
            lastGameTick = currentTick;
            tickCallCount = 0;
        }
        tickCallCount++;

        CallSite detected = STACK_WALKER.walk(frames -> {
            java.util.Optional<CallSite> result = frames
                .map(frame -> {
                    String cn = frame.getClassName();
                    String mn = frame.getMethodName();

                    if (cn.contains("TrackedRandom") || cn.contains("RNGLogger")) return (CallSite) null;

                    // AbstractVillager: addOffersFromItemListings, getOffer
                    if (mn.equals("method_19170")) return CallSite.LEVEL_UP_SLOT_SELECT;
                    if (mn.equals("method_7246"))  return CallSite.LEVEL_UP_OFFER_PARAM;

                    // Villager: updateTrades, customServerAiStep, rewardTradeXp, getBreedOffspring
                    if (cn.equals("net.minecraft.class_1646")) {
                        if (mn.equals("method_7237")) return CallSite.LEVEL_UP_SLOT_SELECT;
                        if (mn.equals("method_5958")) return CallSite.RAID_CHECK;
                        if (mn.equals("method_18008")) return CallSite.TRADE_REWARD_XP;
                        if (mn.equals("method_7225")) return CallSite.BREED_TYPE;
                    }

                    // GossipContainer: transferFrom
                    if (cn.equals("net.minecraft.class_4136") && mn.equals("method_19061")) return CallSite.GOSSIP_TRANSFER;

                    // Villager brain: behavior/util/sensing/goal and movement helpers
                    if (cn.contains(".ai.behavior.") || cn.contains(".ai.util.")
                            || cn.contains(".ai.sensing.") || cn.contains(".ai.goal.")
                            || cn.contains(".Brain") || cn.contains("RandomPos")
                            || cn.contains("BehaviorUtils") || cn.contains("Swim")) {
                        return CallSite.BRAIN_AI;
                    }

                    // LivingEntity: voice pitch, effects, air supply, cramming, equip, base tick
                    if (cn.equals("net.minecraft.class_1309")) {
                        if (mn.equals("method_6017")) return CallSite.VOICE_PITCH;
                        if (mn.equals("method_6050")) return CallSite.POTION_PARTICLE;
                        if (mn.equals("method_6130")) return CallSite.RESPIRATION_ENCHANT;
                        if (mn.equals("method_6070")) return CallSite.ENTITY_CRAMMING;
                        if (mn.equals("method_6116")) return CallSite.EQUIP_SOUND;
                        if (mn.equals("method_5670")) return CallSite.DROWN_BUBBLE;
                    }

                    // Entity: lava hurt, splash, sprint particle, extinguish, swim, amethyst step
                    if (mn.equals("method_5730"))  return CallSite.LAVA_HURT;
                    if (mn.equals("method_36975")) return CallSite.FIRE_EXTINGUISH;
                    if (mn.equals("method_37215")) return CallSite.AMETHYST_STEP;
                    if (mn.equals("method_5734"))  return CallSite.SWIM_SOUND;
                    if (mn.equals("method_5746"))  return CallSite.WATER_SPLASH_SOUND;
                    if (mn.equals("method_5839"))  return CallSite.SPRINT_PARTICLE;

                    // Mob: base tick (ambient sound)
                    if (cn.equals("net.minecraft.class_1308") && mn.equals("method_5670")) return CallSite.AMBIENT_SOUND;

                    return (CallSite) null;
                })
                .filter(site -> site != null)
                .findFirst();

            return result.orElse(CallSite.UNKNOWN);
        });

        return detected;
    }

    private void logCall(String method, Object bound, Object result, long seedBefore, long seedAfter) {
        Level level = owner.level();
        long gameTick = level.getGameTime();
        currentSite = detectCallSite(method, bound);
        RNGLogger.log(owner, currentSite, method, bound, result, seedBefore, seedAfter, ++callCount, gameTick);
    }

    @Override
    public int nextInt(int bound) {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        int result = super.nextInt(bound);
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextInt", bound, result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public float nextFloat() {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        float result = super.nextFloat();
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextFloat", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public double nextDouble() {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        double result = super.nextDouble();
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextDouble", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public long nextLong() {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        long result = super.nextLong();
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextLong", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public double nextGaussian() {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        double result = super.nextGaussian();
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextGaussian", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public int nextInt() {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        int result = super.nextInt();
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextInt", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public boolean nextBoolean() {
        long seedBefore = this.seed.get() ^ 0x5DEECE66DL;
        boolean result = super.nextBoolean();
        long seedAfter = this.seed.get() ^ 0x5DEECE66DL;
        logCall("nextBoolean", "", result, seedBefore, seedAfter);
        return result;
    }
}
