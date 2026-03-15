package xyz.MrYArg1cH.ParticleFX.effect;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleAnimator {
    private final JavaPlugin plugin;
    private final Set<BukkitTask> activeTasks = ConcurrentHashMap.newKeySet();

    public ParticleAnimator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playEffect(Location origin, EffectPreset preset) {
        if (origin == null || origin.getWorld() == null || !preset.enabled() || preset.particles().isEmpty()) {
            return;
        }

        final Location base = origin.clone();
        preset.sound().play(base);

        int maxTick = Math.max(0, preset.durationTicks());
        int interval = Math.max(1, preset.frameIntervalTicks());

        if (maxTick == 0) {
            renderFrame(base, preset, 0);
            return;
        }

        final int[] tick = {0};
        final BukkitTask[] holder = new BukkitTask[1];

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isEnabled() || base.getWorld() == null) {
                    stop();
                    return;
                }

                renderFrame(base, preset, tick[0]);
                tick[0] += interval;
                if (tick[0] > maxTick) {
                    stop();
                }
            }

            private void stop() {
                cancel();
                if (holder[0] != null) {
                    activeTasks.remove(holder[0]);
                }
            }
        };

        holder[0] = runnable.runTaskTimer(plugin, 0L, interval);
        activeTasks.add(holder[0]);
    }

    public void cancelAll() {
        for (BukkitTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
    }

    private void renderFrame(Location base, EffectPreset preset, int tick) {
        switch (preset.animationType()) {
            case RING_WAVE -> renderRingWave(base, preset, tick);
            case DOUBLE_HELIX -> renderDoubleHelix(base, preset, tick);
            case PHOENIX_BURST -> renderPhoenixBurst(base, preset, tick);
            case TORNADO -> renderTornado(base, preset, tick);
            case CELESTIAL_SPHERE -> renderCelestialSphere(base, preset, tick);
            case ORBITAL -> renderOrbital(base, preset, tick);
            case STARBURST -> renderStarburst(base, preset, tick);
            case PILLAR_PULSE -> renderPillarPulse(base, preset, tick);
        }
    }

    private void renderRingWave(Location base, EffectPreset preset, int tick) {
        int points = Math.max(8, preset.points());
        double radius = Math.max(0.15D, preset.radius() + tick * preset.radiusGrowthPerTick());
        double spin = tick * preset.spinSpeed();

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2D / points) * i + spin;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(angle * 2.0D + tick * 0.45D) * preset.verticalWaveAmplitude();
            spawnAt(base, preset, x, y, z);
        }
    }

    private void renderDoubleHelix(Location base, EffectPreset preset, int tick) {
        int points = Math.max(12, preset.points());
        int arms = Math.max(2, preset.arms());
        double height = Math.max(0.6D, preset.height());
        double spin = tick * preset.spinSpeed();
        double travel = (tick * 0.06D) % height;

        for (int arm = 0; arm < arms; arm++) {
            double armShift = Math.PI * 2D / arms * arm;
            for (int i = 0; i < points; i++) {
                double progress = i / (double) points;
                double angle = progress * Math.PI * 2D + spin + armShift;
                double radiusMod = Math.sin(progress * Math.PI * 2D + tick * 0.3D) * preset.radiusGrowthPerTick() * 7D;
                double radius = Math.max(0.08D, preset.radius() + radiusMod);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = (progress * height + travel) % height;
                spawnAt(base, preset, x, y, z);
            }
        }
    }

    private void renderPhoenixBurst(Location base, EffectPreset preset, int tick) {
        int points = Math.max(16, preset.points());
        int duration = Math.max(1, preset.durationTicks());
        double progress = Math.min(1.0D, tick / (double) duration);
        double rise = progress * Math.max(1.0D, preset.height());
        double ringRadius = Math.max(
                0.12D,
                preset.radius() * (1.0D - progress * 0.55D) + Math.sin(tick * 0.35D) * 0.08D
        );

        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2D / points * i + tick * preset.spinSpeed();
            double x = Math.cos(angle) * ringRadius;
            double z = Math.sin(angle) * ringRadius;
            double y = rise + Math.sin(angle * 3.0D + tick * 0.5D) * (preset.verticalWaveAmplitude() * 0.6D);
            spawnAt(base, preset, x, y, z);
        }

        if (tick <= 2) {
            double burstRadius = Math.max(0.25D, preset.radius() * 0.85D);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2D / points * i;
                double x = Math.cos(angle) * burstRadius;
                double z = Math.sin(angle) * burstRadius;
                spawnAt(base, preset, x, 0.06D, z);
            }
        }
    }

    private void renderTornado(Location base, EffectPreset preset, int tick) {
        int points = Math.max(18, preset.points());
        int arms = Math.max(2, preset.arms());
        double height = Math.max(1.0D, preset.height());
        double baseSpin = tick * preset.spinSpeed();

        for (int arm = 0; arm < arms; arm++) {
            double armShift = Math.PI * 2D / arms * arm;
            for (int i = 0; i < points; i++) {
                double progress = i / (double) points;
                double y = progress * height;
                double radius = Math.max(
                        0.08D,
                        preset.radius() * 0.25D + progress * (preset.radius() + tick * preset.radiusGrowthPerTick() * 0.35D)
                );
                double angle = baseSpin + progress * Math.PI * 6D + armShift;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                spawnAt(base, preset, x, y, z);
            }
        }
    }

    private void renderCelestialSphere(Location base, EffectPreset preset, int tick) {
        int rings = Math.max(4, preset.arms() + 2);
        int pointsPerRing = Math.max(8, preset.points() / rings);
        double radius = Math.max(0.25D, preset.radius() + Math.sin(tick * 0.25D) * preset.radiusGrowthPerTick() * 5D);
        double halfHeight = Math.max(0.6D, preset.height()) / 2.0D;

        for (int ring = 0; ring <= rings; ring++) {
            double latitude = -Math.PI / 2D + Math.PI / rings * ring;
            double ringRadius = Math.cos(latitude) * radius;
            double y = Math.sin(latitude) * halfHeight;
            double spinDirection = ring % 2 == 0 ? 1.0D : -1.0D;

            for (int i = 0; i < pointsPerRing; i++) {
                double angle = Math.PI * 2D / pointsPerRing * i + tick * preset.spinSpeed() * spinDirection;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                spawnAt(base, preset, x, y, z);
            }
        }
    }

    private void renderOrbital(Location base, EffectPreset preset, int tick) {
        int orbitCount = Math.max(2, preset.arms());
        int pointsPerOrbit = Math.max(10, preset.points() / orbitCount);
        double height = Math.max(0.8D, preset.height());

        for (int orbit = 0; orbit < orbitCount; orbit++) {
            double y = orbitCount == 1 ? 0.0D : ((orbit / (double) (orbitCount - 1)) - 0.5D) * height;
            double orbitRadius = Math.max(
                    0.1D,
                    preset.radius() + Math.sin(tick * 0.3D + orbit) * preset.radiusGrowthPerTick() * 5D
            );
            double speed = preset.spinSpeed() * (orbit % 2 == 0 ? 1.0D : -1.0D);

            for (int i = 0; i < pointsPerOrbit; i++) {
                double angle = Math.PI * 2D / pointsPerOrbit * i + tick * speed + orbit * 0.7D;
                double x = Math.cos(angle) * orbitRadius;
                double z = Math.sin(angle) * orbitRadius;
                spawnAt(base, preset, x, y, z);
            }
        }
    }

    private void renderStarburst(Location base, EffectPreset preset, int tick) {
        int rays = Math.max(4, preset.arms() * 2);
        int pointsPerRay = Math.max(3, preset.points() / rays);
        double pulse = 0.7D + Math.sin(tick * 0.4D) * 0.25D;

        for (int ray = 0; ray < rays; ray++) {
            double angle = Math.PI * 2D / rays * ray + tick * preset.spinSpeed() * 0.2D;
            for (int i = 1; i <= pointsPerRay; i++) {
                double progress = i / (double) pointsPerRay;
                double spokeScale = i % 2 == 0 ? 1.0D : 0.45D;
                double distance = (preset.radius() + tick * preset.radiusGrowthPerTick() * 0.4D) * progress * pulse * spokeScale;
                double x = Math.cos(angle) * distance;
                double z = Math.sin(angle) * distance;
                double y = Math.sin(progress * Math.PI + tick * 0.35D) * preset.verticalWaveAmplitude();
                spawnAt(base, preset, x, y, z);
            }
        }
    }

    private void renderPillarPulse(Location base, EffectPreset preset, int tick) {
        int layers = Math.max(6, preset.points() / 2);
        int ringPoints = Math.max(8, preset.points());
        double height = Math.max(1.0D, preset.height());
        double pulseRadius = Math.max(0.12D, preset.radius() + Math.sin(tick * 0.45D) * preset.radiusGrowthPerTick() * 6D);

        for (int layer = 0; layer <= layers; layer++) {
            double y = layer / (double) layers * height;
            spawnAt(base, preset, 0.0D, y, 0.0D);

            if ((layer + tick) % 2 != 0) {
                continue;
            }

            for (int i = 0; i < ringPoints; i++) {
                double angle = Math.PI * 2D / ringPoints * i + tick * preset.spinSpeed() * 0.25D;
                double x = Math.cos(angle) * pulseRadius;
                double z = Math.sin(angle) * pulseRadius;
                spawnAt(base, preset, x, y, z);
            }
        }
    }

    private void spawnAt(Location base, EffectPreset preset, double x, double y, double z) {
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        Location point = base.clone().add(
                preset.originOffsetX() + x,
                preset.originOffsetY() + preset.yOffset() + y,
                preset.originOffsetZ() + z
        );
        for (ParticleSpec particle : preset.particles()) {
            particle.spawn(world, point);
        }
    }
}
