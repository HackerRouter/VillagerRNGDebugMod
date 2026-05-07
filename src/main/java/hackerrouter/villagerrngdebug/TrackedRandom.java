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

    // StackWalker used to detect call origin
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
        // Automatically reset counter by detecting game tick changes
        long currentTick = owner.level.getGameTime();
        if (currentTick != lastGameTick) {
            lastGameTick = currentTick;
            tickCallCount = 0;
        }
        tickCallCount++;

        // Use StackWalker to detect call origin
        // At runtime uses intermediary mappings (class_xxxx, method_xxxx)
        CallSite detected = STACK_WALKER.walk(frames -> {
            java.util.List<StackWalker.StackFrame> frameList = frames.collect(java.util.stream.Collectors.toList());
            java.util.Optional<CallSite> result = frameList.stream()
                .map(frame -> {
                    String className = frame.getClassName();
                    String methodName = frame.getMethodName();

                    // Skip stack frames from TrackedRandom itself and RNGLogger
                    if (className.contains("TrackedRandom") || className.contains("RNGLogger")) {
                        return (CallSite) null;
                    }

                    // === FrostWalkerEnchantment (MUST be before Mob.baseTick to avoid misidentification) ===
                    // FrostWalkerEnchantment.onEntityMoved → Mth.nextInt(random, 60, 120) → nextInt(61)
                    // Signature: bound=61 and FrostWalkerEnchantment present in stack (class_1887 / buk)
                    if (className.contains("FrostWalker") || className.contains("class_1887") || className.endsWith(".buk")) {
                        return CallSite.FROST_WALK;
                    }

                    // === Villager trade/level-up (must be before RAID_CHECK since they're called from within customServerAiStep) ===
                    // VillagerTrades$ItemListing.getOffer - LEVEL_UP_OFFER_PARAM (method_7246)
                    // Trade parameter randomization: enchantment type/level/price/color etc., called internally by ItemListing implementations
                    if (methodName.equals("getOffer") || methodName.equals("method_7246")) {
                        return CallSite.LEVEL_UP_OFFER_PARAM;
                    }
                    // AbstractVillager.addOffersFromItemListings - LEVEL_UP_SLOT_SELECT (method_19170)
                    // Slot selection randomization: nextInt(listings.length), only consumed when candidates > 2 (e.g. librarian levels 1-4 have 3-4 candidates each)
                    if (methodName.equals("addOffersFromItemListings") || methodName.equals("method_19170")) {
                        return CallSite.LEVEL_UP_SLOT_SELECT;
                    }
                    // Villager.updateTrades - LEVEL_UP_SLOT_SELECT (method_7237)
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) && 
                        (methodName.equals("updateTrades") || methodName.equals("method_7237"))) {
                        return CallSite.LEVEL_UP_SLOT_SELECT;
                    }
                    // Villager.increaseMerchantCareer - LEVEL_UP_SLOT_SELECT (method_16918)
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) && 
                        (methodName.equals("increaseMerchantCareer") || methodName.equals("method_16918"))) {
                        return CallSite.LEVEL_UP_SLOT_SELECT;
                    }
                    // === Villager ===
                    // Villager.customServerAiStep - RAID_CHECK (method_5958)
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) && 
                        (methodName.equals("customServerAiStep") || methodName.equals("method_5958"))) {
                        return CallSite.RAID_CHECK;
                    }
                    // Villager.rewardTradeXp - TRADE_REWARD_XP (method_18008)
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) && 
                        (methodName.equals("rewardTradeXp") || methodName.equals("method_18008"))) {
                        return CallSite.TRADE_REWARD_XP;
                    }
                    // Villager.getBreedOffspring - BREED_TYPE (method_5613)
                    if ((className.endsWith(".Villager") || className.endsWith(".class_1646")) && 
                        (methodName.equals("getBreedOffspring") || methodName.equals("method_5613"))) {
                        return CallSite.BREED_TYPE;
                    }
                    // === LivingEntity (specific methods first, before baseTick) ===
                    // LivingEntity.markHurt - MARK_HURT (method_5785)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("markHurt") || methodName.equals("method_5785"))) {
                        return CallSite.MARK_HURT;
                    }
                    // LivingEntity.getVoicePitch - VOICE_PITCH (method_6017)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("getVoicePitch") || methodName.equals("method_6017"))) {
                        return CallSite.VOICE_PITCH;
                    }
                    // LivingEntity.tickEffects - POTION_PARTICLE (method_6050)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("tickEffects") || methodName.equals("method_6050"))) {
                        return CallSite.POTION_PARTICLE;
                    }
                    // LivingEntity.spawnSoulSpeedParticle - SOUL_SPEED_PARTICLE (method_25937)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("spawnSoulSpeedParticle") || methodName.equals("method_25937"))) {
                        return CallSite.SOUL_SPEED_PARTICLE;
                    }
                    // LivingEntity.decreaseAirSupply - RESPIRATION_ENCHANT (method_6130)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("decreaseAirSupply") || methodName.equals("method_6130"))) {
                        return CallSite.RESPIRATION_ENCHANT;
                    }
                    // LivingEntity.pushEntities - ENTITY_CRAMMING (method_6070)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("pushEntities") || methodName.equals("method_6070"))) {
                        return CallSite.ENTITY_CRAMMING;
                    }
                    // LivingEntity.triggerItemUseEffects - EAT_SOUND (method_6098)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("triggerItemUseEffects") || methodName.equals("method_6098"))) {
                        return CallSite.EAT_SOUND;
                    }
                    // LivingEntity.spawnItemParticles - EAT_PARTICLE (method_6037)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("spawnItemParticles") || methodName.equals("method_6037"))) {
                        return CallSite.EAT_PARTICLE;
                    }
                    // LivingEntity.handleEntityEvent - THORNS_SOUND (method_11148 / method_5711)
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("handleEntityEvent") || methodName.equals("method_11148") || methodName.equals("method_5711"))) {
                        return CallSite.THORNS_SOUND;
                    }
                    // LivingEntity.baseTick - DROWN_BUBBLE (method_5773) — must be AFTER specific methods
                    if ((className.endsWith(".LivingEntity") || className.endsWith(".class_1309")) && 
                        (methodName.equals("baseTick") || methodName.equals("method_5773"))) {
                        return CallSite.DROWN_BUBBLE;
                    }
                    // === Entity (specific methods first, before baseTick) ===
                    // Entity.lavaHurt - LAVA_HURT (method_5730)
                    if (methodName.equals("lavaHurt") || methodName.equals("method_5730")) {
                        return CallSite.LAVA_HURT;
                    }
                    // Entity.playEntityOnFireExtinguishedSound - FIRE_EXTINGUISH (method_36975)
                    if (methodName.equals("playEntityOnFireExtinguishedSound") || methodName.equals("method_36975")) {
                        return CallSite.FIRE_EXTINGUISH;
                    }
                    // Entity.playAmethystStepSound - AMETHYST_STEP (method_37215)
                    if (methodName.equals("playAmethystStepSound") || methodName.equals("method_37215")) {
                        return CallSite.AMETHYST_STEP;
                    }
                    // Entity.playSwimSound - SWIM_SOUND (method_5734)
                    if (methodName.equals("playSwimSound") || methodName.equals("method_5734")) {
                        return CallSite.SWIM_SOUND;
                    }
                    // Entity.doWaterSplashEffect - WATER_SPLASH (method_5746)
                    if (methodName.equals("doWaterSplashEffect") || methodName.equals("method_5746")) {
                        return CallSite.WATER_SPLASH_SOUND;
                    }
                    // Entity.spawnSprintParticle - SPRINT_PARTICLE (method_5839)
                    if (methodName.equals("spawnSprintParticle") || methodName.equals("method_5839")) {
                        return CallSite.SPRINT_PARTICLE;
                    }
                    // Entity.moveTowardsClosestSpace - STUCK_IN_BLOCK (method_30673 / method_5632)
                    if (methodName.equals("moveTowardsClosestSpace") || methodName.equals("method_30673") || methodName.equals("method_5632")) {
                        return CallSite.STUCK_IN_BLOCK;
                    }
                    // === Mob ===
                    // Mob.baseTick - AMBIENT_SOUND (method_5670)
                    if ((className.endsWith(".Mob") || className.endsWith(".class_1308")) && 
                        (methodName.equals("baseTick") || methodName.equals("method_5670"))) {
                        return CallSite.AMBIENT_SOUND;
                    }
                    return (CallSite) null;
                })
                .filter(site -> site != null)
                .findFirst();
            
            // If unrecognized, print stack frames for debugging
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
        // bound=61 uniquely corresponds to FrostWalkerEnchantment.onEntityMoved → Mth.nextInt(random,60,120)
        // StackWalker cannot reliably match FrostWalker class name in this scenario (obfuscated to buk), use parameter signature instead
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
