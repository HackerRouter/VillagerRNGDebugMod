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

public enum CallSite {
    // Entity.java
    LAVA_HURT,
    FIRE_EXTINGUISH,
    AMETHYST_STEP,
    SWIM_SOUND,
    WATER_SPLASH_SOUND,
    WATER_SPLASH_BUBBLE,
    WATER_SPLASH_SPLASH,
    SPRINT_PARTICLE,
    STUCK_IN_BLOCK,
    
    // LivingEntity.java
    DROWN_BUBBLE,
    RESPIRATION_ENCHANT,
    POTION_PARTICLE,
    SOUL_SPEED_PARTICLE,
    MARK_HURT,
    VOICE_PITCH,
    THORNS_SOUND,
    ENTITY_CRAMMING,
    EAT_SOUND,
    EAT_PARTICLE,
    
    // Mob.java
    AMBIENT_SOUND,
    
    // Villager.java
    RAID_CHECK,
    TRADE_REWARD_XP,
    // updateTrades → addOffersFromItemListings: nextInt(listings.length) slot selection (only when candidates > 2)
    LEVEL_UP_SLOT_SELECT,
    // updateTrades → addOffersFromItemListings → getOffer: trade parameter randomization (enchantment type/level/price/color etc.)
    LEVEL_UP_OFFER_PARAM,
    BREED_TYPE,
    
    // FrostWalkerEnchantment.java
    FROST_WALK,
    
    UNKNOWN
}
