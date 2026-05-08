package hackerrouter.villagerrngdebug;

public enum CallSite {
    // Entity
    LAVA_HURT, FIRE_EXTINGUISH, AMETHYST_STEP, SWIM_SOUND,
    WATER_SPLASH_SOUND, WATER_SPLASH_BUBBLE, WATER_SPLASH_SPLASH,
    SPRINT_PARTICLE, STUCK_IN_BLOCK,

    // LivingEntity
    DROWN_BUBBLE, RESPIRATION_ENCHANT, POTION_PARTICLE, SOUL_SPEED_PARTICLE,
    MARK_HURT, VOICE_PITCH, THORNS_SOUND, ENTITY_CRAMMING, EAT_SOUND, EAT_PARTICLE,

    // Mob
    AMBIENT_SOUND,

    // Villager
    RAID_CHECK, BRAIN_AI, TRADE_REWARD_XP,
    LEVEL_UP_SLOT_SELECT,  // nextInt(listings.length) slot selection
    LEVEL_UP_OFFER_PARAM,  // trade parameter randomization
    BREED_TYPE,

    // FrostWalkerEnchantment
    FROST_WALK,

    UNKNOWN
}
