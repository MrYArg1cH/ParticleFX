package xyz.MrYArg1cH.ParticleFX.effect;

import java.util.Locale;

public enum AnimationType {
    RING_WAVE,
    DOUBLE_HELIX,
    PHOENIX_BURST,
    TORNADO,
    CELESTIAL_SPHERE,
    ORBITAL,
    STARBURST,
    PILLAR_PULSE;

    public static AnimationType fromString(String value, AnimationType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return AnimationType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
