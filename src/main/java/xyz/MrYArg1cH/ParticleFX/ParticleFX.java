package xyz.MrYArg1cH.ParticleFX;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.MrYArg1cH.ParticleFX.command.ParticleFXCommand;
import xyz.MrYArg1cH.ParticleFX.config.EffectConfig;
import xyz.MrYArg1cH.ParticleFX.effect.ParticleAnimator;
import xyz.MrYArg1cH.ParticleFX.listener.PlayerEffectListener;
import xyz.MrYArg1cH.ParticleFX.profile.PlayerProfileManager;
import xyz.MrYArg1cH.ParticleFX.service.EffectService;

public final class ParticleFX extends JavaPlugin {
    private EffectConfig effectConfig;
    private ParticleAnimator particleAnimator;
    private PlayerProfileManager playerProfileManager;
    private EffectService effectService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRuntimeConfig();

        playerProfileManager = new PlayerProfileManager(this);
        particleAnimator = new ParticleAnimator(this);
        effectService = new EffectService(this);

        getServer().getPluginManager().registerEvents(new PlayerEffectListener(this), this);

        PluginCommand command = getCommand("particlefx");
        if (command != null) {
            ParticleFXCommand executor = new ParticleFXCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Command 'particlefx' is missing in plugin.yml.");
        }
    }

    public void loadRuntimeConfig() {
        reloadConfig();
        effectConfig = EffectConfig.load(getConfig(), getLogger());
        if (particleAnimator != null) {
            particleAnimator.cancelAll();
        }
    }

    public EffectConfig getEffectConfig() {
        return effectConfig;
    }

    public ParticleAnimator getParticleAnimator() {
        return particleAnimator;
    }

    public PlayerProfileManager getPlayerProfileManager() {
        return playerProfileManager;
    }

    public EffectService getEffectService() {
        return effectService;
    }

    @Override
    public void onDisable() {
        if (particleAnimator != null) {
            particleAnimator.cancelAll();
        }
        if (playerProfileManager != null) {
            playerProfileManager.saveAll();
        }
    }
}
