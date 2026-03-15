package xyz.MrYArg1cH.ParticleFX.profile;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.MrYArg1cH.ParticleFX.ParticleFX;
import xyz.MrYArg1cH.ParticleFX.effect.AnimationType;
import xyz.MrYArg1cH.ParticleFX.effect.EffectPreset;
import xyz.MrYArg1cH.ParticleFX.effect.EffectTrigger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PlayerProfileManager {
    private final Logger logger;
    private final File playerDataFolder;
    private final Map<UUID, PlayerEffectProfile> cache = new ConcurrentHashMap<>();

    public PlayerProfileManager(ParticleFX plugin) {
        this.logger = plugin.getLogger();
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists() && !playerDataFolder.mkdirs()) {
            logger.warning("Failed to create playerdata folder at " + playerDataFolder.getAbsolutePath());
        }
    }

    public PlayerEffectProfile getProfile(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), ignored -> loadProfile(player));
    }

    public void unload(Player player) {
        PlayerEffectProfile profile = cache.remove(player.getUniqueId());
        if (profile != null) {
            profile.setLastKnownName(player.getName());
            saveProfile(profile);
        }
    }

    public void saveProfile(PlayerEffectProfile profile) {
        File file = new File(playerDataFolder, profile.uniqueId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("player-name", profile.lastKnownName());

        ConfigurationSection presetsSection = yaml.createSection("presets");
        for (Map.Entry<String, EffectPreset> entry : profile.presets().entrySet()) {
            ConfigurationSection presetSection = presetsSection.createSection(entry.getKey());
            entry.getValue().saveTo(presetSection);
        }

        ConfigurationSection bindingsSection = yaml.createSection("bindings");
        for (Map.Entry<EffectTrigger, List<String>> entry : profile.bindings().entrySet()) {
            bindingsSection.set(entry.getKey().key(), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            logger.warning("Failed to save player profile " + profile.uniqueId() + ": " + exception.getMessage());
        }
    }

    public void saveAll() {
        for (PlayerEffectProfile profile : cache.values()) {
            saveProfile(profile);
        }
    }

    private PlayerEffectProfile loadProfile(Player player) {
        File file = new File(playerDataFolder, player.getUniqueId() + ".yml");
        if (!file.exists()) {
            return new PlayerEffectProfile(player.getUniqueId(), player.getName());
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerEffectProfile profile = new PlayerEffectProfile(
                player.getUniqueId(),
                yaml.getString("player-name", player.getName())
        );
        profile.setLastKnownName(player.getName());

        ConfigurationSection presetsSection = yaml.getConfigurationSection("presets");
        if (presetsSection != null) {
            for (String key : presetsSection.getKeys(false)) {
                ConfigurationSection presetSection = presetsSection.getConfigurationSection(key);
                if (presetSection == null) {
                    continue;
                }

                profile.putPreset(
                        key,
                        EffectPreset.fromSection(presetSection, logger, "playerdata." + key, AnimationType.RING_WAVE)
                );
            }
        }

        ConfigurationSection bindingsSection = yaml.getConfigurationSection("bindings");
        if (bindingsSection != null) {
            for (String key : bindingsSection.getKeys(false)) {
                EffectTrigger trigger = EffectTrigger.fromString(key);
                if (trigger == null) {
                    logger.warning("Unknown trigger '" + key + "' in player profile " + player.getUniqueId() + ".");
                    continue;
                }

                List<String> bindingNames = new ArrayList<>();
                if (bindingsSection.isList(key)) {
                    for (String presetName : bindingsSection.getStringList(key)) {
                        String normalized = PlayerEffectProfile.normalizePresetName(presetName);
                        if (normalized != null && !bindingNames.contains(normalized)) {
                            bindingNames.add(normalized);
                        }
                    }
                } else {
                    String singleName = PlayerEffectProfile.normalizePresetName(bindingsSection.getString(key));
                    if (singleName != null) {
                        bindingNames.add(singleName);
                    }
                }

                if (bindingNames.isEmpty()) {
                    profile.clearBinding(trigger);
                } else {
                    profile.setBinding(trigger, bindingNames);
                }
            }
        }

        return profile;
    }
}
