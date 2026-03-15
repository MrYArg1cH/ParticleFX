package xyz.MrYArg1cH.ParticleFX.config;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import xyz.MrYArg1cH.ParticleFX.effect.AnimationType;
import xyz.MrYArg1cH.ParticleFX.effect.EffectPreset;
import xyz.MrYArg1cH.ParticleFX.effect.EffectTrigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public record EffectConfig(
        Set<String> disabledWorlds,
        String reloadPermission,
        String customizePermission,
        Messages messages,
        Map<String, EffectPreset> presets,
        Map<EffectTrigger, List<String>> defaultBindings
) {
    public static EffectConfig load(FileConfiguration config, Logger logger) {
        Set<String> disabledWorlds = new TreeSet<>();
        for (String worldName : config.getStringList("settings.disabled-worlds")) {
            if (worldName != null && !worldName.isBlank()) {
                disabledWorlds.add(worldName.toLowerCase(Locale.ROOT));
            }
        }

        Messages messages = new Messages(
                config.getString("messages.prefix", "&8[&bParticleFX&8] &r"),
                config.getString("messages.usage", "&7Use &b/particlefx help &7for commands."),
                config.getString("messages.no-permission", "&cYou do not have permission."),
                config.getString("messages.player-only", "&cOnly players can use this command."),
                config.getString("messages.unknown-subcommand", "&cUnknown subcommand."),
                config.getString("messages.reload-success", "&aConfiguration reloaded.")
        );

        Map<String, EffectPreset> presets = new LinkedHashMap<>();
        EnumMap<EffectTrigger, List<String>> bindings = new EnumMap<>(EffectTrigger.class);

        ConfigurationSection effectsSection = config.getConfigurationSection("effects");
        if (effectsSection != null) {
            ConfigurationSection presetsSection = effectsSection.getConfigurationSection("presets");
            if (presetsSection != null) {
                for (String key : presetsSection.getKeys(false)) {
                    String normalizedName = normalizePresetName(key);
                    if (normalizedName != null) {
                        presets.put(normalizedName, EffectPreset.fromSection(
                                presetsSection.getConfigurationSection(key),
                                logger,
                                "effects.presets." + key,
                                AnimationType.RING_WAVE
                        ));
                    }
                }
            }

            ConfigurationSection bindingsSection = effectsSection.getConfigurationSection("bindings");
            if (bindingsSection != null) {
                for (String key : bindingsSection.getKeys(false)) {
                    EffectTrigger trigger = EffectTrigger.fromString(key);
                    if (trigger == null) {
                        logger.warning("Unknown trigger '" + key + "' in effects.bindings, skipping.");
                        continue;
                    }
                    bindings.put(trigger, Collections.unmodifiableList(readBinding(bindingsSection, key)));
                }
            }

            loadLegacyPreset(effectsSection, "jump", EffectTrigger.JUMP, AnimationType.RING_WAVE, presets, bindings, logger);
            loadLegacyPreset(effectsSection, "death", EffectTrigger.DEATH, AnimationType.DOUBLE_HELIX, presets, bindings, logger);
            loadLegacyPreset(effectsSection, "respawn", EffectTrigger.RESPAWN, AnimationType.PHOENIX_BURST, presets, bindings, logger);
        }

        return new EffectConfig(
                Set.copyOf(disabledWorlds),
                config.getString("settings.reload-permission", "particlefx.reload"),
                config.getString("settings.customize-permission", "particlefx.customize"),
                messages,
                Map.copyOf(presets),
                Map.copyOf(bindings)
        );
    }

    public boolean isWorldDisabled(World world) {
        return world != null && disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public EffectPreset findGlobalPreset(String name) {
        String normalizedName = normalizePresetName(name);
        return normalizedName == null ? null : presets.get(normalizedName);
    }

    public List<String> bindingsFor(EffectTrigger trigger) {
        return defaultBindings.getOrDefault(trigger, List.of());
    }

    private static void loadLegacyPreset(
            ConfigurationSection effectsSection,
            String key,
            EffectTrigger trigger,
            AnimationType fallbackAnimation,
            Map<String, EffectPreset> presets,
            EnumMap<EffectTrigger, List<String>> bindings,
            Logger logger
    ) {
        ConfigurationSection section = effectsSection.getConfigurationSection(key);
        if (section == null) {
            return;
        }

        String normalizedName = normalizePresetName(key);
        presets.putIfAbsent(normalizedName, EffectPreset.fromSection(
                section,
                logger,
                "effects." + key,
                fallbackAnimation
        ));
        bindings.putIfAbsent(trigger, List.of(normalizedName));
    }

    private static List<String> readBinding(ConfigurationSection section, String key) {
        List<String> rawValues = new ArrayList<>();
        if (section.isList(key)) {
            rawValues.addAll(section.getStringList(key));
        } else {
            String single = section.getString(key);
            if (single != null && !single.isBlank()) {
                rawValues.add(single);
            }
        }

        List<String> normalized = new ArrayList<>();
        for (String rawValue : rawValues) {
            String presetName = normalizePresetName(rawValue);
            if (presetName != null && !normalized.contains(presetName)) {
                normalized.add(presetName);
            }
        }
        return normalized;
    }

    public static String normalizePresetName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        return normalized.isBlank() ? null : normalized;
    }

    public record Messages(
            String prefix,
            String usage,
            String noPermission,
            String playerOnly,
            String unknownSubcommand,
            String reloadSuccess
    ) {
    }
}
