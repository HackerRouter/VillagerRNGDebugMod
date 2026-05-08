package hackerrouter.villagerrngdebug;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Random;

public class TrackedRandom extends Random {
    private final LivingEntity owner;
    private long callCount = 0;
    private int lastResult = 0;
    private int tickCallCount = 0;
    private long lastGameTick = -1;
    private CallSite currentSite = CallSite.UNKNOWN;

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public TrackedRandom(LivingEntity owner) {
        super(owner.getRandom().nextLong());
        this.owner = owner;
    }

    public void resetTickCount() {
        tickCallCount = 0;
    }

    public int getLastResult() {
        return lastResult;
    }

    private CallSite detectCallSite(String method, Object bound) {
        // Reset counter on new game tick
        long currentTick = owner.level.getGameTime();
        if (currentTick != lastGameTick) {
            lastGameTick = currentTick;
            tickCallCount = 0;
        }
        tickCallCount++;

        // Walk stack to identify call origin (uses intermediary mappings at runtime)
        CallSite detected = STACK_WALKER.walk(frames -> {
            java.util.List<StackWalker.StackFrame> frameList = frames.collect(java.util.stream.Collectors.toList());
            java.util.Optional<CallSite> result = frameList.stream()
                .map(frame -> {
                    String className = frame.getClassName();
                    String methodName = frame.getMethodName();

                    if (className.contains("TrackedRandom") || className.contains("RNGLogger")) {
                        return (CallSite) null;
                    }

                    // FrostWalkerEnchantment (before Mob.baseTick to avoid misidentification)
                    if (className.contains("FrostWalker") || className.contains("class_1887") || className.endsWith(".buk")) {
                        return CallSite.FROST_WALK;
                    }

                    // Villager trade/level-up (before RAID_CHECK)
                    if (methodName.equals("getOffer") || methodName.equals("method_7246")) {
                        return CallSite.LEVEL_UP_OFFER_PARAM;
                    }
                    if (methodName.equals("addOffersFromItemListings") || methodName.equals("method_19170")) {
                        return CallSite.LEVEL_UP_SLOT_SELECT;
                    }
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) &&
                        (methodName.equals("updateTrades") || methodName.equals("method_7237"))) {
                        return CallSite.LEVEL_UP_SLOT_SELECT;
                    }
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) &&
                        (methodName.equals("increaseMerchantCareer") || methodName.equals("method_16918"))) {
                        return CallSite.LEVEL_UP_SLOT_SELECT;
                    }
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) &&
                        (methodName.equals("customServerAiStep") || methodName.equals("method_5958"))) {
                        return CallSite.RAID_CHECK;
                    }
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) &&
                        (methodName.equals("rewardTradeXp") || methodName.equals("method_18008"))) {
                        return CallSite.TRADE_REWARD_XP;
                    }
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) &&
                        (methodName.equals("getBreedOffspring") || methodName.equals("method_5613"))) {
                        return CallSite.BREED_TYPE;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("markHurt") || methodName.equals("method_5785"))) {
                        return CallSite.MARK_HURT;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("getVoicePitch") || methodName.equals("method_6017"))) {
                        return CallSite.VOICE_PITCH;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("tickEffects") || methodName.equals("method_6050"))) {
                        return CallSite.POTION_PARTICLE;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("spawnSoulSpeedParticle") || methodName.equals("method_25937"))) {
                        return CallSite.SOUL_SPEED_PARTICLE;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("decreaseAirSupply") || methodName.equals("method_6130"))) {
                        return CallSite.RESPIRATION_ENCHANT;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("pushEntities") || methodName.equals("method_6070"))) {
                        return CallSite.ENTITY_CRAMMING;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("triggerItemUseEffects") || methodName.equals("method_6098"))) {
                        return CallSite.EAT_SOUND;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("spawnItemParticles") || methodName.equals("method_6037"))) {
                        return CallSite.EAT_PARTICLE;
                    }
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("handleEntityEvent") || methodName.equals("method_11148") || methodName.equals("method_5711"))) {
                        return CallSite.THORNS_SOUND;
                    }
                    // baseTick must be AFTER specific LivingEntity methods
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) &&
                        (methodName.equals("baseTick") || methodName.equals("method_5773"))) {
                        return CallSite.DROWN_BUBBLE;
                    }
                    if (methodName.equals("lavaHurt") || methodName.equals("method_5730")) {
                        return CallSite.LAVA_HURT;
                    }
                    if (methodName.equals("playEntityOnFireExtinguishedSound") || methodName.equals("method_36975")) {
                        return CallSite.FIRE_EXTINGUISH;
                    }
                    if (methodName.equals("playAmethystStepSound") || methodName.equals("method_37215")) {
                        return CallSite.AMETHYST_STEP;
                    }
                    if (methodName.equals("playSwimSound") || methodName.equals("method_5734")) {
                        return CallSite.SWIM_SOUND;
                    }
                    if (methodName.equals("doWaterSplashEffect") || methodName.equals("method_5746")) {
                        return CallSite.WATER_SPLASH_SOUND;
                    }
                    if (methodName.equals("spawnSprintParticle") || methodName.equals("method_5839")) {
                        return CallSite.SPRINT_PARTICLE;
                    }
                    if (methodName.equals("moveTowardsClosestSpace") || methodName.equals("method_30673") || methodName.equals("method_5632")) {
                        return CallSite.STUCK_IN_BLOCK;
                    }
                    if ((className.endsWith(".Mob") || className.endsWith(".class_1308")) &&
                        (methodName.equals("baseTick") || methodName.equals("method_5670"))) {
                        return CallSite.AMBIENT_SOUND;
                    }
                    return (CallSite) null;
                })
                .filter(site -> site != null)
                .findFirst();
            
            if (!result.isPresent()) {
                StringBuilder sb = new StringBuilder("[UNKNOWN_CALLSITE] ");
                frameList.stream()
                    .filter(f -> !f.getClassName().contains("TrackedRandom") && !f.getClassName().contains("RNGLogger"))
                    .limit(12)
                    .forEach(f -> sb.append(f.getClassName()).append(".").append(f.getMethodName()).append(" | "));
                System.out.println(sb.toString());
            }
            
            return result.orElse(CallSite.UNKNOWN);
        });

        return detected;
    }

    private void logCall(String method, Object bound, Object result, long seedBefore, long seedAfter) {
        Level level = owner.level;
        long gameTick = level.getGameTime();
        currentSite = detectCallSite(method, bound);
        RNGLogger.log(owner, currentSite, method, bound, result, seedBefore, seedAfter, ++callCount, gameTick);
    }

    @Override
    public int nextInt(int bound) {
        long seedBefore = VillagerRNGTracker.getSeed(this);
        int result = super.nextInt(bound);
        long seedAfter = VillagerRNGTracker.getSeed(this);
        lastResult = result;
        // bound=61 → FrostWalkerEnchantment (obfuscated class, matched by parameter signature)
        if (bound == 61) {
            Level level = owner.level;
            long gameTick = level.getGameTime();
            if (gameTick != lastGameTick) { lastGameTick = gameTick; tickCallCount = 0; }
            tickCallCount++;
            RNGLogger.log(owner, CallSite.FROST_WALK, "nextInt", bound, result, seedBefore, seedAfter, ++callCount, gameTick);
            return result;
        }
        logCall("nextInt", bound, result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public float nextFloat() {
        long seedBefore = VillagerRNGTracker.getSeed(this);
        float result = super.nextFloat();
        long seedAfter = VillagerRNGTracker.getSeed(this);
        logCall("nextFloat", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public double nextDouble() {
        long seedBefore = VillagerRNGTracker.getSeed(this);
        double result = super.nextDouble();
        long seedAfter = VillagerRNGTracker.getSeed(this);
        logCall("nextDouble", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public boolean nextBoolean() {
        long seedBefore = VillagerRNGTracker.getSeed(this);
        boolean result = super.nextBoolean();
        long seedAfter = VillagerRNGTracker.getSeed(this);
        logCall("nextBoolean", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public long nextLong() {
        long seedBefore = VillagerRNGTracker.getSeed(this);
        long result = super.nextLong();
        long seedAfter = VillagerRNGTracker.getSeed(this);
        logCall("nextLong", "", result, seedBefore, seedAfter);
        return result;
    }

    @Override
    public double nextGaussian() {
        long seedBefore = VillagerRNGTracker.getSeed(this);
        double result = super.nextGaussian();
        long seedAfter = VillagerRNGTracker.getSeed(this);
        logCall("nextGaussian", "", result, seedBefore, seedAfter);
        return result;
    }
}
