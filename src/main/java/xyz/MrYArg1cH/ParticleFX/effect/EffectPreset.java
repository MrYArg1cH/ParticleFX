package xyz.MrYArg1cH.ParticleFX.effect;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public record EffectPreset(
        boolean enabled,
        String permission,
        AnimationType animationType,
        int durationTicks,
        int frameIntervalTicks,
        int points,
        int arms,
        double radius,
        double radiusGrowthPerTick,
        double height,
        double yOffset,
        double verticalWaveAmplitude,
        double spinSpeed,
        int cooldownTicks,
        int startDelayTicks,
        double originOffsetX,
        double originOffsetY,
        double originOffsetZ,
        List<ParticleSpec> particles,
        SoundSpec sound
) {
    public static EffectPreset fromSection(
            ConfigurationSection section,
            Logger logger,
            String path,
            AnimationType fallbackAnimation
    ) {
        if (section == null) {
            return builder().animationType(fallbackAnimation).build();
        }

        List<ParticleSpec> particles = new ArrayList<>();
        List<Map<?, ?>> rawParticles = section.getMapList("particles");
        for (int i = 0; i < rawParticles.size(); i++) {
            particles.add(ParticleSpec.fromMap(rawParticles.get(i), logger, path + ".particles[" + i + "]"));
        }
        if (particles.isEmpty()) {
            particles.add(ParticleSpec.fromSection(section, logger, path + ".particles.default"));
        }

        return EffectPreset.builder()
                .enabled(section.getBoolean("enabled", true))
                .permission(section.getString("permission", ""))
                .animationType(AnimationType.fromString(section.getString("animation"), fallbackAnimation))
                .durationTicks(section.getInt("duration-ticks", 16))
                .frameIntervalTicks(section.getInt("frame-interval-ticks", 1))
                .points(section.getInt("points", 24))
                .arms(section.getInt("arms", 2))
                .radius(section.getDouble("radius", 1.0D))
                .radiusGrowthPerTick(section.getDouble("radius-growth-per-tick", 0.03D))
                .height(section.getDouble("height", 1.6D))
                .yOffset(section.getDouble("y-offset", 0.0D))
                .verticalWaveAmplitude(section.getDouble("vertical-wave-amplitude", 0.08D))
                .spinSpeed(section.getDouble("spin-speed", 0.35D))
                .cooldownTicks(section.getInt("cooldown-ticks", 0))
                .startDelayTicks(section.getInt("start-delay-ticks", 0))
                .originOffsetX(section.getDouble("origin-offset-x", 0.0D))
                .originOffsetY(section.getDouble("origin-offset-y", 0.0D))
                .originOffsetZ(section.getDouble("origin-offset-z", 0.0D))
                .particles(particles)
                .sound(SoundSpec.fromConfig(section.getConfigurationSection("sound"), logger, path + ".sound"))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean canUse(Player player) {
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", enabled);
        values.put("permission", permission);
        values.put("animation", animationType.name());
        values.put("duration-ticks", durationTicks);
        values.put("frame-interval-ticks", frameIntervalTicks);
        values.put("points", points);
        values.put("arms", arms);
        values.put("radius", radius);
        values.put("radius-growth-per-tick", radiusGrowthPerTick);
        values.put("height", height);
        values.put("y-offset", yOffset);
        values.put("vertical-wave-amplitude", verticalWaveAmplitude);
        values.put("spin-speed", spinSpeed);
        values.put("cooldown-ticks", cooldownTicks);
        values.put("start-delay-ticks", startDelayTicks);
        values.put("origin-offset-x", originOffsetX);
        values.put("origin-offset-y", originOffsetY);
        values.put("origin-offset-z", originOffsetZ);

        List<Map<String, Object>> particleMaps = new ArrayList<>();
        for (ParticleSpec particle : particles) {
            particleMaps.add(particle.asMap());
        }
        values.put("particles", particleMaps);
        values.put("sound", sound.asMap());
        return values;
    }

    public void saveTo(ConfigurationSection section) {
        for (Map.Entry<String, Object> entry : asMap().entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
    }

    public static final class Builder {
        private boolean enabled = true;
        private String permission = "";
        private AnimationType animationType = AnimationType.RING_WAVE;
        private int durationTicks = 16;
        private int frameIntervalTicks = 1;
        private int points = 24;
        private int arms = 2;
        private double radius = 1.0D;
        private double radiusGrowthPerTick = 0.03D;
        private double height = 1.6D;
        private double yOffset = 0.0D;
        private double verticalWaveAmplitude = 0.08D;
        private double spinSpeed = 0.35D;
        private int cooldownTicks = 0;
        private int startDelayTicks = 0;
        private double originOffsetX = 0.0D;
        private double originOffsetY = 0.0D;
        private double originOffsetZ = 0.0D;
        private List<ParticleSpec> particles = new ArrayList<>(List.of(ParticleSpec.builder().build()));
        private SoundSpec sound = SoundSpec.disabled();

        private Builder() {
        }

        private Builder(EffectPreset preset) {
            enabled = preset.enabled;
            permission = preset.permission;
            animationType = preset.animationType;
            durationTicks = preset.durationTicks;
            frameIntervalTicks = preset.frameIntervalTicks;
            points = preset.points;
            arms = preset.arms;
            radius = preset.radius;
            radiusGrowthPerTick = preset.radiusGrowthPerTick;
            height = preset.height;
            yOffset = preset.yOffset;
            verticalWaveAmplitude = preset.verticalWaveAmplitude;
            spinSpeed = preset.spinSpeed;
            cooldownTicks = preset.cooldownTicks;
            startDelayTicks = preset.startDelayTicks;
            originOffsetX = preset.originOffsetX;
            originOffsetY = preset.originOffsetY;
            originOffsetZ = preset.originOffsetZ;
            particles = new ArrayList<>(preset.particles);
            sound = preset.sound;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission == null ? "" : permission;
            return this;
        }

        public Builder animationType(AnimationType animationType) {
            this.animationType = animationType == null ? AnimationType.RING_WAVE : animationType;
            return this;
        }

        public Builder durationTicks(int durationTicks) {
            this.durationTicks = durationTicks;
            return this;
        }

        public Builder frameIntervalTicks(int frameIntervalTicks) {
            this.frameIntervalTicks = frameIntervalTicks;
            return this;
        }

        public Builder points(int points) {
            this.points = points;
            return this;
        }

        public Builder arms(int arms) {
            this.arms = arms;
            return this;
        }

        public Builder radius(double radius) {
            this.radius = radius;
            return this;
        }

        public Builder radiusGrowthPerTick(double radiusGrowthPerTick) {
            this.radiusGrowthPerTick = radiusGrowthPerTick;
            return this;
        }

        public Builder height(double height) {
            this.height = height;
            return this;
        }

        public Builder yOffset(double yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        public Builder verticalWaveAmplitude(double verticalWaveAmplitude) {
            this.verticalWaveAmplitude = verticalWaveAmplitude;
            return this;
        }

        public Builder spinSpeed(double spinSpeed) {
            this.spinSpeed = spinSpeed;
            return this;
        }

        public Builder cooldownTicks(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
            return this;
        }

        public Builder startDelayTicks(int startDelayTicks) {
            this.startDelayTicks = startDelayTicks;
            return this;
        }

        public Builder originOffsetX(double originOffsetX) {
            this.originOffsetX = originOffsetX;
            return this;
        }

        public Builder originOffsetY(double originOffsetY) {
            this.originOffsetY = originOffsetY;
            return this;
        }

        public Builder originOffsetZ(double originOffsetZ) {
            this.originOffsetZ = originOffsetZ;
            return this;
        }

        public Builder particles(List<ParticleSpec> particles) {
            this.particles = particles == null ? new ArrayList<>() : new ArrayList<>(particles);
            return this;
        }

        public Builder sound(SoundSpec sound) {
            this.sound = sound == null ? SoundSpec.disabled() : sound;
            return this;
        }

        public EffectPreset build() {
            return new EffectPreset(
                    enabled,
                    permission == null ? "" : permission.trim(),
                    animationType == null ? AnimationType.RING_WAVE : animationType,
                    Math.max(0, durationTicks),
                    Math.max(1, frameIntervalTicks),
                    Math.max(8, points),
                    Math.max(1, arms),
                    radius,
                    radiusGrowthPerTick,
                    height,
                    yOffset,
                    verticalWaveAmplitude,
                    spinSpeed,
                    Math.max(0, cooldownTicks),
                    Math.max(0, startDelayTicks),
                    originOffsetX,
                    originOffsetY,
                    originOffsetZ,
                    List.copyOf(particles),
                    sound == null ? SoundSpec.disabled() : sound
            );
        }
    }
}
