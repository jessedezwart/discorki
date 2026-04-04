package com.alistats.discorki.model;

import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
public enum Tier {
    IRON("🟤", false),
    BRONZE("🟠", false),
    SILVER("⚪", false),
    GOLD("🟡", false),
    PLATINUM("🟢", false),
    EMERALD("🟩", false),
    DIAMOND("🔵", false),
    MASTER("🟣", true),
    GRANDMASTER("🔴", true),
    CHALLENGER("👑", true);

    private final boolean isApex;
    private final String emoji;

    private static final int LP_VALUE_PER_TIER = 400;
    private static final int NORMAL_TIERS = 7;
    
    Tier(String emoji, boolean isApex) {
        this.isApex = isApex;
        this.emoji = emoji;
    }

    public int getTierLpValue() {
        if (!this.isApex) {
            return this.ordinal() * LP_VALUE_PER_TIER;
        } else {
            return NORMAL_TIERS * LP_VALUE_PER_TIER;
        }
    }

    public String getName() {
        return StringUtils.capitalize(this.name().toLowerCase());
    }
}
