package xyz.MrYArg1cH.ParticleFX.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.MrYArg1cH.ParticleFX.ParticleFX;
import xyz.MrYArg1cH.ParticleFX.config.EffectConfig;
import xyz.MrYArg1cH.ParticleFX.effect.EffectPreset;
import xyz.MrYArg1cH.ParticleFX.effect.EffectTrigger;
import xyz.MrYArg1cH.ParticleFX.profile.PlayerEffectProfile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectService {
    private final ParticleFX plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public EffectService(ParticleFX plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, EffectTrigger trigger, Location origin) {
        if (player == null || trigger == null || origin == null || origin.getWorld() == null) {
            return;
        }

        EffectConfig config = plugin.getEffectConfig();
        if (config.isWorldDisabled(origin.getWorld())) {
            return;
        }

        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        List<String> bindingNames = resolveBindingNames(profile, config, trigger);
        if (bindingNames.isEmpty()) {
            return;
        }

        for (String presetName : new LinkedHashSet<>(bindingNames)) {
            EffectPreset preset = resolvePreset(profile, config, presetName);
            if (preset == null || !preset.enabled() || !preset.canUse(player)) {
                continue;
            }

            if (isOnCooldown(player, trigger, presetName, preset)) {
                continue;
            }

            scheduleEffect(origin, preset);
        }
    }

    public EffectPreset resolvePreset(Player player, String name) {
        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        return resolvePreset(profile, plugin.getEffectConfig(), name);
    }

    public List<String> resolveEffectiveBinding(Player player, EffectTrigger trigger) {
        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        return List.copyOf(resolveBindingNames(profile, plugin.getEffectConfig(), trigger));
    }

    public List<String> availablePresetNames(Player player) {
        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        TreeSet<String> names = new TreeSet<>(profile.presets().keySet());
        names.addAll(plugin.getEffectConfig().presets().keySet());
        return List.copyOf(names);
    }

    public void preview(Player player, EffectPreset preset) {
        if (player == null || preset == null || player.getLocation().getWorld() == null) {
            return;
        }

        if (!preset.enabled() || !preset.canUse(player) || plugin.getEffectConfig().isWorldDisabled(player.getWorld())) {
            return;
        }

        scheduleEffect(player.getLocation(), preset);
    }

    private List<String> resolveBindingNames(PlayerEffectProfile profile, EffectConfig config, EffectTrigger trigger) {
        if (profile.hasBindingOverride(trigger)) {
            return profile.bindingOverride(trigger);
        }
        return config.bindingsFor(trigger);
    }

    private EffectPreset resolvePreset(PlayerEffectProfile profile, EffectConfig config, String name) {
        String normalizedName = PlayerEffectProfile.normalizePresetName(name);
        if (normalizedName == null) {
            return null;
        }

        EffectPreset personalPreset = profile.getPreset(normalizedName);
        return personalPreset != null ? personalPreset : config.findGlobalPreset(normalizedName);
    }

    private boolean isOnCooldown(Player player, EffectTrigger trigger, String presetName, EffectPreset preset) {
        int cooldownTicks = preset.cooldownTicks();
        if (cooldownTicks <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownTicks * 50L;
        String key = player.getUniqueId() + "|" + trigger.key() + "|" + presetName;
        Long lastUse = cooldowns.get(key);
        if (lastUse != null && now - lastUse < cooldownMillis) {
            return true;
        }

        cooldowns.put(key, now);
        return false;
    }

    private void scheduleEffect(Location origin, EffectPreset preset) {
        Location effectOrigin = origin.clone();
        int delay = preset.startDelayTicks();
        if (delay <= 0) {
            plugin.getParticleAnimator().playEffect(effectOrigin, preset);
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            plugin.getParticleAnimator().playEffect(effectOrigin, preset);
        }, delay);
    }
}
