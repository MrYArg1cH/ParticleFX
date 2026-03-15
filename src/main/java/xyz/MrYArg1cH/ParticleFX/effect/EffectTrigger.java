package xyz.MrYArg1cH.ParticleFX.effect;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum EffectTrigger {
    JUMP("jump"),
    DEATH("death"),
    RESPAWN("respawn"),
    JOIN("join"),
    QUIT("quit"),
    MOVE("move"),
    SPRINT_START("sprint-start"),
    SPRINT_STOP("sprint-stop"),
    SNEAK_START("sneak-start"),
    SNEAK_STOP("sneak-stop"),
    ATTACK("attack"),
    HIT("hit"),
    CONSUME("consume"),
    TELEPORT("teleport"),
    BLOCK_BREAK("block-break"),
    BLOCK_PLACE("block-place"),
    SHOOT_BOW("shoot-bow"),
    LEVEL_UP("level-up");

    private final String key;

    EffectTrigger(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static EffectTrigger fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = normalize(value);
        for (EffectTrigger trigger : values()) {
            if (trigger.key.equals(normalized)) {
                return trigger;
            }
        }
        return null;
    }

    public static String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(EffectTrigger::key).toList();
    }
}
