package xyz.MrYArg1cH.ParticleFX.effect;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public record SoundSpec(
        boolean enabled,
        Sound sound,
        float volume,
        float pitch
) {
    public static SoundSpec disabled() {
        return new SoundSpec(false, null, 1.0F, 1.0F);
    }

    public static SoundSpec fromConfig(ConfigurationSection section, Logger logger, String path) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return disabled();
        }

        String soundName = section.getString("type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        Sound soundValue = null;
        try {
            soundValue = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            logger.warning("Invalid sound '" + soundName + "' at " + path + ".type, disabling sound.");
        }

        return new SoundSpec(
                soundValue != null,
                soundValue,
                (float) section.getDouble("volume", 0.8D),
                (float) section.getDouble("pitch", 1.2D)
        );
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", enabled && sound != null);
        values.put("type", sound == null ? "ENTITY_EXPERIENCE_ORB_PICKUP" : sound.name());
        values.put("volume", volume);
        values.put("pitch", pitch);
        return values;
    }

    public void play(Location location) {
        if (!enabled || sound == null || location == null) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(location, sound, volume, pitch);
    }

    public static final class Builder {
        private boolean enabled;
        private Sound sound;
        private float volume = 1.0F;
        private float pitch = 1.0F;

        public Builder() {
        }

        private Builder(SoundSpec soundSpec) {
            enabled = soundSpec.enabled;
            sound = soundSpec.sound;
            volume = soundSpec.volume;
            pitch = soundSpec.pitch;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder sound(Sound sound) {
            this.sound = sound;
            return this;
        }

        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public SoundSpec build() {
            if (!enabled) {
                return SoundSpec.disabled();
            }
            return new SoundSpec(true, sound == null ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : sound, volume, pitch);
        }
    }
}
