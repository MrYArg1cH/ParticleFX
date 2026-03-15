package xyz.MrYArg1cH.ParticleFX.command;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import xyz.MrYArg1cH.ParticleFX.ParticleFX;
import xyz.MrYArg1cH.ParticleFX.effect.AnimationType;
import xyz.MrYArg1cH.ParticleFX.effect.EffectPreset;
import xyz.MrYArg1cH.ParticleFX.effect.EffectTrigger;
import xyz.MrYArg1cH.ParticleFX.effect.ParticleSpec;
import xyz.MrYArg1cH.ParticleFX.effect.SoundSpec;
import xyz.MrYArg1cH.ParticleFX.profile.PlayerEffectProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ParticleFXCommand implements CommandExecutor, TabCompleter {
    private static final List<String> TOP_LEVEL = List.of(
            "help", "reload", "actions", "animations", "presets", "preview", "preset", "bind"
    );
    private static final List<String> PRESET_ACTIONS = List.of(
            "create", "clone", "rename", "delete", "info", "set", "particle"
    );
    private static final List<String> PRESET_PROPERTIES = List.of(
            "enabled", "permission", "animation", "duration", "interval", "points", "arms", "radius",
            "radius-growth", "height", "y-offset", "wave", "spin", "cooldown", "delay",
            "origin-x", "origin-y", "origin-z", "sound-enabled", "sound-type", "sound-volume", "sound-pitch"
    );
    private static final List<String> PARTICLE_ACTIONS = List.of("add", "remove", "set", "list", "clear");
    private static final List<String> PARTICLE_PROPERTIES = List.of(
            "particle", "count", "offset-x", "offset-y", "offset-z", "extra", "color",
            "from-color", "to-color", "size", "block-data", "item"
    );
    private static final List<String> BIND_ACTIONS = List.of("list", "add", "remove", "clear", "reset");

    private final ParticleFX plugin;

    public ParticleFXCommand(ParticleFX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "actions" -> handleActions(sender);
            case "animations" -> handleAnimations(sender);
            case "presets" -> handlePresets(sender);
            case "preview" -> handlePreview(sender, args);
            case "preset" -> handlePreset(sender, args);
            case "bind" -> handleBind(sender, args);
            default -> {
                send(sender, plugin.getEffectConfig().messages().unknownSubcommand());
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return complete(args[0], TOP_LEVEL);
        }

        Player player = sender instanceof Player onlinePlayer ? onlinePlayer : null;
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "preview" -> player == null || args.length != 2
                    ? List.of()
                    : complete(args[1], plugin.getEffectService().availablePresetNames(player));
            case "preset" -> completePreset(sender, player, args);
            case "bind" -> completeBind(sender, player, args);
            default -> List.of();
        };
    }

    private boolean handleReload(CommandSender sender) {
        String permission = plugin.getEffectConfig().reloadPermission();
        if (!hasPermission(sender, permission)) {
            send(sender, plugin.getEffectConfig().messages().noPermission());
            return true;
        }

        plugin.loadRuntimeConfig();
        send(sender, plugin.getEffectConfig().messages().reloadSuccess());
        return true;
    }

    private boolean handleActions(CommandSender sender) {
        send(sender, "&7Available actions: &b" + String.join("&7, &b", EffectTrigger.keys()));
        return true;
    }

    private boolean handleAnimations(CommandSender sender) {
        List<String> names = Arrays.stream(AnimationType.values()).map(Enum::name).toList();
        send(sender, "&7Available animations: &b" + String.join("&7, &b", names));
        return true;
    }

    private boolean handlePresets(CommandSender sender) {
        Player player = requirePlayerWithCustomize(sender);
        if (player == null) {
            return true;
        }

        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        List<String> globalNames = new ArrayList<>(plugin.getEffectConfig().presets().keySet());
        List<String> personalNames = new ArrayList<>(profile.presets().keySet());
        globalNames.sort(String::compareTo);
        personalNames.sort(String::compareTo);

        send(player, "&7Personal presets: &b" + (personalNames.isEmpty() ? "none" : String.join("&7, &b", personalNames)));
        send(player, "&7Global presets: &b" + (globalNames.isEmpty() ? "none" : String.join("&7, &b", globalNames)));
        return true;
    }

    private boolean handlePreview(CommandSender sender, String[] args) {
        Player player = requirePlayerWithCustomize(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            send(player, "&cUsage: /particlefx preview <preset>");
            return true;
        }

        EffectPreset preset = plugin.getEffectService().resolvePreset(player, args[1]);
        if (preset == null) {
            send(player, "&cPreset not found: &f" + args[1]);
            return true;
        }

        if (!preset.canUse(player)) {
            send(player, "&cYou do not have permission to preview this preset.");
            return true;
        }

        plugin.getEffectService().preview(player, preset);
        send(player, "&aPreview started for preset &f" + normalizeName(args[1]) + "&a.");
        return true;
    }

    private boolean handlePreset(CommandSender sender, String[] args) {
        Player player = requirePlayerWithCustomize(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            send(player, "&cUsage: /particlefx preset <create|clone|rename|delete|info|set|particle> ...");
            return true;
        }

        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> presetCreate(player, profile, args);
            case "clone" -> presetClone(player, profile, args);
            case "rename" -> presetRename(player, profile, args);
            case "delete" -> presetDelete(player, profile, args);
            case "info" -> presetInfo(player, profile, args);
            case "set" -> presetSet(player, profile, args);
            case "particle" -> presetParticle(player, profile, args);
            default -> {
                send(player, "&cUsage: /particlefx preset <create|clone|rename|delete|info|set|particle> ...");
                yield true;
            }
        };
    }

    private boolean handleBind(CommandSender sender, String[] args) {
        Player player = requirePlayerWithCustomize(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            send(player, "&cUsage: /particlefx bind <list|add|remove|clear|reset> ...");
            return true;
        }

        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> bindList(player, profile);
            case "add" -> bindAdd(player, profile, args);
            case "remove" -> bindRemove(player, profile, args);
            case "clear" -> bindClear(player, profile, args);
            case "reset" -> bindReset(player, profile, args);
            default -> {
                send(player, "&cUsage: /particlefx bind <list|add|remove|clear|reset> ...");
                yield true;
            }
        };
    }

    private boolean presetCreate(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 3) {
            send(player, "&cUsage: /particlefx preset create <name>");
            return true;
        }

        String normalizedName = normalizeName(args[2]);
        if (normalizedName == null) {
            send(player, "&cPreset name is invalid.");
            return true;
        }
        if (plugin.getEffectService().resolvePreset(player, normalizedName) != null) {
            send(player, "&cA preset with that name already exists.");
            return true;
        }

        profile.putPreset(normalizedName, EffectPreset.builder().build());
        saveProfile(profile);
        send(player, "&aCreated personal preset &f" + normalizedName + "&a.");
        return true;
    }

    private boolean presetClone(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 4) {
            send(player, "&cUsage: /particlefx preset clone <source> <new-name>");
            return true;
        }

        EffectPreset source = plugin.getEffectService().resolvePreset(player, args[2]);
        if (source == null) {
            send(player, "&cPreset not found: &f" + args[2]);
            return true;
        }

        String newName = normalizeName(args[3]);
        if (newName == null) {
            send(player, "&cPreset name is invalid.");
            return true;
        }
        if (plugin.getEffectService().resolvePreset(player, newName) != null) {
            send(player, "&cA preset with that name already exists.");
            return true;
        }

        profile.putPreset(newName, source.toBuilder().build());
        saveProfile(profile);
        send(player, "&aCloned preset to &f" + newName + "&a.");
        return true;
    }

    private boolean presetRename(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 4) {
            send(player, "&cUsage: /particlefx preset rename <old-name> <new-name>");
            return true;
        }

        if (profile.getPreset(args[2]) == null) {
            send(player, "&cYou can only rename personal presets.");
            return true;
        }

        String newName = normalizeName(args[3]);
        if (newName == null) {
            send(player, "&cPreset name is invalid.");
            return true;
        }

        EffectPreset existing = plugin.getEffectService().resolvePreset(player, newName);
        if (existing != null && !normalizeName(args[2]).equals(newName)) {
            send(player, "&cA preset with that name already exists.");
            return true;
        }

        if (!profile.renamePreset(args[2], newName)) {
            send(player, "&cFailed to rename preset.");
            return true;
        }

        saveProfile(profile);
        send(player, "&aPreset renamed to &f" + newName + "&a.");
        return true;
    }

    private boolean presetDelete(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 3) {
            send(player, "&cUsage: /particlefx preset delete <name>");
            return true;
        }

        if (profile.removePreset(args[2]) == null) {
            send(player, "&cYou can only delete personal presets.");
            return true;
        }

        saveProfile(profile);
        send(player, "&aPreset deleted: &f" + normalizeName(args[2]));
        return true;
    }

    private boolean presetInfo(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 3) {
            send(player, "&cUsage: /particlefx preset info <name>");
            return true;
        }

        String normalizedName = normalizeName(args[2]);
        EffectPreset preset = plugin.getEffectService().resolvePreset(player, normalizedName);
        if (preset == null) {
            send(player, "&cPreset not found: &f" + args[2]);
            return true;
        }

        String source = profile.getPreset(normalizedName) != null ? "personal" : "global";
        send(player, "&7Preset &f" + normalizedName + " &7(" + source + ")");
        send(player, "&7Animation: &b" + preset.animationType().name()
                + " &7| Points: &b" + preset.points()
                + " &7| Arms: &b" + preset.arms());
        send(player, "&7Radius: &b" + preset.radius()
                + " &7| Height: &b" + preset.height()
                + " &7| Spin: &b" + preset.spinSpeed());
        send(player, "&7Cooldown: &b" + preset.cooldownTicks()
                + " &7| Delay: &b" + preset.startDelayTicks()
                + " &7| Particles: &b" + preset.particles().size());
        send(player, "&7Permission: &b" + (preset.permission().isBlank() ? "none" : preset.permission()));
        return true;
    }

    private boolean presetSet(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 5) {
            send(player, "&cUsage: /particlefx preset set <name> <property> <value>");
            return true;
        }

        EffectPreset preset = profile.getPreset(args[2]);
        if (preset == null) {
            send(player, "&cYou can only edit personal presets.");
            return true;
        }

        try {
            EffectPreset updated = updatePresetProperty(preset, args[3], join(args, 4));
            profile.putPreset(args[2], updated);
            saveProfile(profile);
            send(player, "&aUpdated &f" + normalizeName(args[2]) + "&a property &f" + args[3] + "&a.");
        } catch (IllegalArgumentException exception) {
            send(player, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean presetParticle(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 3) {
            send(player, "&cUsage: /particlefx preset particle <add|remove|set|list|clear> ...");
            return true;
        }

        return switch (args[2].toLowerCase(Locale.ROOT)) {
            case "add" -> particleAdd(player, profile, args);
            case "remove" -> particleRemove(player, profile, args);
            case "set" -> particleSet(player, profile, args);
            case "list" -> particleList(player, profile, args);
            case "clear" -> particleClear(player, profile, args);
            default -> {
                send(player, "&cUsage: /particlefx preset particle <add|remove|set|list|clear> ...");
                yield true;
            }
        };
    }

    private boolean bindList(Player player, PlayerEffectProfile profile) {
        boolean hasAny = false;
        for (EffectTrigger trigger : EffectTrigger.values()) {
            List<String> effective = plugin.getEffectService().resolveEffectiveBinding(player, trigger);
            boolean override = profile.hasBindingOverride(trigger);
            if (effective.isEmpty() && !override) {
                continue;
            }

            hasAny = true;
            String source = override ? "custom" : "global";
            String value = effective.isEmpty() ? "off" : String.join(", ", effective);
            send(player, "&7" + trigger.key() + " &8[" + source + "] &7-> &b" + value);
        }

        if (!hasAny) {
            send(player, "&7No bindings are active.");
        }
        return true;
    }

    private boolean bindAdd(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 4) {
            send(player, "&cUsage: /particlefx bind add <action> <preset>");
            return true;
        }

        EffectTrigger trigger = parseTrigger(args[2]);
        if (trigger == null) {
            send(player, "&cUnknown action: &f" + args[2]);
            return true;
        }

        String presetName = normalizeName(args[3]);
        if (presetName == null) {
            send(player, "&cPreset name is invalid.");
            return true;
        }
        if (plugin.getEffectService().resolvePreset(player, presetName) == null) {
            send(player, "&cPreset not found: &f" + args[3]);
            return true;
        }

        List<String> updated = new ArrayList<>(plugin.getEffectService().resolveEffectiveBinding(player, trigger));
        if (!updated.contains(presetName)) {
            updated.add(presetName);
        }
        profile.setBinding(trigger, updated);
        saveProfile(profile);
        send(player, "&aBound preset &f" + presetName + " &ato action &f" + trigger.key() + "&a.");
        return true;
    }

    private boolean bindRemove(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 4) {
            send(player, "&cUsage: /particlefx bind remove <action> <preset>");
            return true;
        }

        EffectTrigger trigger = parseTrigger(args[2]);
        if (trigger == null) {
            send(player, "&cUnknown action: &f" + args[2]);
            return true;
        }

        String presetName = normalizeName(args[3]);
        if (presetName == null) {
            send(player, "&cPreset name is invalid.");
            return true;
        }
        List<String> updated = new ArrayList<>(plugin.getEffectService().resolveEffectiveBinding(player, trigger));
        updated.removeIf(presetName::equals);
        profile.setBinding(trigger, updated);
        saveProfile(profile);
        send(player, "&aRemoved preset &f" + presetName + " &afrom action &f" + trigger.key() + "&a.");
        return true;
    }

    private boolean bindClear(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 3) {
            send(player, "&cUsage: /particlefx bind clear <action>");
            return true;
        }

        EffectTrigger trigger = parseTrigger(args[2]);
        if (trigger == null) {
            send(player, "&cUnknown action: &f" + args[2]);
            return true;
        }

        profile.clearBinding(trigger);
        saveProfile(profile);
        send(player, "&aAction &f" + trigger.key() + " &ais now disabled for your profile.");
        return true;
    }

    private boolean bindReset(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 3) {
            send(player, "&cUsage: /particlefx bind reset <action>");
            return true;
        }

        EffectTrigger trigger = parseTrigger(args[2]);
        if (trigger == null) {
            send(player, "&cUnknown action: &f" + args[2]);
            return true;
        }

        profile.resetBinding(trigger);
        saveProfile(profile);
        send(player, "&aAction &f" + trigger.key() + " &areset to global defaults.");
        return true;
    }

    private boolean particleAdd(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 5) {
            send(player, "&cUsage: /particlefx preset particle add <preset> <particle>");
            return true;
        }

        EffectPreset preset = profile.getPreset(args[3]);
        if (preset == null) {
            send(player, "&cYou can only edit personal presets.");
            return true;
        }

        try {
            Particle particle = parseParticle(args[4]);
            List<ParticleSpec> particles = new ArrayList<>(preset.particles());
            particles.add(ParticleSpec.builder().particle(particle).build());
            profile.putPreset(args[3], preset.toBuilder().particles(particles).build());
            saveProfile(profile);
            send(player, "&aAdded particle &f" + particle.name() + "&a to preset &f" + normalizeName(args[3]) + "&a.");
        } catch (IllegalArgumentException exception) {
            send(player, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean particleRemove(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 5) {
            send(player, "&cUsage: /particlefx preset particle remove <preset> <index>");
            return true;
        }

        EffectPreset preset = profile.getPreset(args[3]);
        if (preset == null) {
            send(player, "&cYou can only edit personal presets.");
            return true;
        }

        try {
            int index = parseIndex(args[4], preset.particles().size());
            List<ParticleSpec> particles = new ArrayList<>(preset.particles());
            particles.remove(index);
            profile.putPreset(args[3], preset.toBuilder().particles(particles).build());
            saveProfile(profile);
            send(player, "&aRemoved particle #" + (index + 1) + " from preset &f" + normalizeName(args[3]) + "&a.");
        } catch (IllegalArgumentException exception) {
            send(player, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean particleSet(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 7) {
            send(player, "&cUsage: /particlefx preset particle set <preset> <index> <property> <value>");
            return true;
        }

        EffectPreset preset = profile.getPreset(args[3]);
        if (preset == null) {
            send(player, "&cYou can only edit personal presets.");
            return true;
        }

        try {
            int index = parseIndex(args[4], preset.particles().size());
            List<ParticleSpec> particles = new ArrayList<>(preset.particles());
            particles.set(index, updateParticleProperty(particles.get(index), args[5], join(args, 6)));
            profile.putPreset(args[3], preset.toBuilder().particles(particles).build());
            saveProfile(profile);
            send(player, "&aUpdated particle #" + (index + 1) + " on preset &f" + normalizeName(args[3]) + "&a.");
        } catch (IllegalArgumentException exception) {
            send(player, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean particleList(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 4) {
            send(player, "&cUsage: /particlefx preset particle list <preset>");
            return true;
        }

        EffectPreset preset = profile.getPreset(args[3]);
        if (preset == null) {
            send(player, "&cYou can only inspect personal presets with this command.");
            return true;
        }

        if (preset.particles().isEmpty()) {
            send(player, "&7Preset has no particles.");
            return true;
        }

        send(player, "&7Particles for &f" + normalizeName(args[3]) + "&7:");
        for (int i = 0; i < preset.particles().size(); i++) {
            ParticleSpec particle = preset.particles().get(i);
            send(player, "&7#" + (i + 1) + " &b" + particle.particle().name()
                    + " &7count=&b" + particle.count()
                    + " &7extra=&b" + particle.extra());
        }
        return true;
    }

    private boolean particleClear(Player player, PlayerEffectProfile profile, String[] args) {
        if (args.length < 4) {
            send(player, "&cUsage: /particlefx preset particle clear <preset>");
            return true;
        }

        EffectPreset preset = profile.getPreset(args[3]);
        if (preset == null) {
            send(player, "&cYou can only edit personal presets.");
            return true;
        }

        profile.putPreset(args[3], preset.toBuilder().particles(List.of()).build());
        saveProfile(profile);
        send(player, "&aRemoved all particles from preset &f" + normalizeName(args[3]) + "&a.");
        return true;
    }

    private EffectPreset updatePresetProperty(EffectPreset preset, String property, String rawValue) {
        String normalizedProperty = normalizeKey(property);
        EffectPreset.Builder builder = preset.toBuilder();
        SoundSpec.Builder soundBuilder = preset.sound().toBuilder();

        switch (normalizedProperty) {
            case "enabled" -> builder.enabled(parseBoolean(rawValue));
            case "permission" -> builder.permission(clearableString(rawValue));
            case "animation" -> builder.animationType(parseAnimation(rawValue));
            case "duration", "duration-ticks" -> builder.durationTicks(parseInt(rawValue));
            case "interval", "frame-interval", "frame-interval-ticks" -> builder.frameIntervalTicks(parseInt(rawValue));
            case "points" -> builder.points(parseInt(rawValue));
            case "arms" -> builder.arms(parseInt(rawValue));
            case "radius" -> builder.radius(parseDouble(rawValue));
            case "radius-growth", "radius-growth-per-tick" -> builder.radiusGrowthPerTick(parseDouble(rawValue));
            case "height" -> builder.height(parseDouble(rawValue));
            case "y-offset" -> builder.yOffset(parseDouble(rawValue));
            case "wave", "vertical-wave-amplitude" -> builder.verticalWaveAmplitude(parseDouble(rawValue));
            case "spin", "spin-speed" -> builder.spinSpeed(parseDouble(rawValue));
            case "cooldown", "cooldown-ticks" -> builder.cooldownTicks(parseInt(rawValue));
            case "delay", "start-delay", "start-delay-ticks" -> builder.startDelayTicks(parseInt(rawValue));
            case "origin-x", "origin-offset-x" -> builder.originOffsetX(parseDouble(rawValue));
            case "origin-y", "origin-offset-y" -> builder.originOffsetY(parseDouble(rawValue));
            case "origin-z", "origin-offset-z" -> builder.originOffsetZ(parseDouble(rawValue));
            case "sound-enabled" -> {
                boolean enabled = parseBoolean(rawValue);
                soundBuilder.enabled(enabled);
                if (enabled && preset.sound().sound() == null) {
                    soundBuilder.sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                }
            }
            case "sound-type" -> {
                String value = clearableString(rawValue);
                if (value.isBlank()) {
                    soundBuilder.enabled(false).sound(null);
                } else {
                    soundBuilder.enabled(true).sound(parseSound(value));
                }
            }
            case "sound-volume" -> {
                soundBuilder.enabled(true).volume((float) parseDouble(rawValue));
                if (preset.sound().sound() == null) {
                    soundBuilder.sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                }
            }
            case "sound-pitch" -> {
                soundBuilder.enabled(true).pitch((float) parseDouble(rawValue));
                if (preset.sound().sound() == null) {
                    soundBuilder.sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                }
            }
            default -> throw new IllegalArgumentException("Unknown preset property: " + property);
        }

        return builder.sound(soundBuilder.build()).build();
    }

    private ParticleSpec updateParticleProperty(ParticleSpec particleSpec, String property, String rawValue) {
        String normalizedProperty = normalizeKey(property);
        ParticleSpec.Builder builder = particleSpec.toBuilder();

        switch (normalizedProperty) {
            case "particle" -> builder.particle(parseParticle(rawValue));
            case "count" -> builder.count(parseInt(rawValue));
            case "offset-x" -> builder.offsetX(parseDouble(rawValue));
            case "offset-y" -> builder.offsetY(parseDouble(rawValue));
            case "offset-z" -> builder.offsetZ(parseDouble(rawValue));
            case "extra" -> builder.extra(parseDouble(rawValue));
            case "color" -> builder.dustOptions(new Particle.DustOptions(parseColor(rawValue), existingSize(particleSpec, 1.0F)));
            case "from-color" -> builder.dustTransition(new Particle.DustTransition(
                    parseColor(rawValue),
                    existingToColor(particleSpec),
                    existingSize(particleSpec, 1.0F)
            ));
            case "to-color" -> builder.dustTransition(new Particle.DustTransition(
                    existingFromColor(particleSpec),
                    parseColor(rawValue),
                    existingSize(particleSpec, 1.0F)
            ));
            case "size" -> {
                float size = (float) parseDouble(rawValue);
                if (particleSpec.particle() == Particle.DUST) {
                    builder.dustOptions(new Particle.DustOptions(existingFromColor(particleSpec), Math.max(0.2F, size)));
                } else {
                    builder.dustTransition(new Particle.DustTransition(
                            existingFromColor(particleSpec),
                            existingToColor(particleSpec),
                            Math.max(0.2F, size)
                    ));
                }
            }
            case "block-data" -> builder.blockData(rawValue);
            case "item" -> builder.itemMaterial(rawValue);
            default -> throw new IllegalArgumentException("Unknown particle property: " + property);
        }

        return builder.build();
    }

    private void saveProfile(PlayerEffectProfile profile) {
        plugin.getPlayerProfileManager().saveProfile(profile);
    }

    private Player requirePlayerWithCustomize(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, plugin.getEffectConfig().messages().playerOnly());
            return null;
        }

        if (!hasPermission(sender, plugin.getEffectConfig().customizePermission())) {
            send(sender, plugin.getEffectConfig().messages().noPermission());
            return null;
        }
        return player;
    }

    private void sendHelp(CommandSender sender) {
        send(sender, "&7/particlefx reload &8- &fReload config");
        send(sender, "&7/particlefx actions &8- &fList available action triggers");
        send(sender, "&7/particlefx animations &8- &fList available animation types");
        send(sender, "&7/particlefx presets &8- &fShow your personal presets and global presets");
        send(sender, "&7/particlefx preset create <name> &8- &fCreate a personal preset");
        send(sender, "&7/particlefx preset set <name> <property> <value> &8- &fEdit a preset field");
        send(sender, "&7/particlefx preset particle <...> &8- &fManage particle layers inside a preset");
        send(sender, "&7/particlefx bind <list|add|remove|clear|reset> &8- &fBind presets to actions");
        send(sender, "&7/particlefx preview <preset> &8- &fPreview a preset at your location");
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }

    private List<String> completePreset(CommandSender sender, Player player, String[] args) {
        if (args.length == 2) {
            return complete(args[1], PRESET_ACTIONS);
        }
        if (player == null || !hasPermission(sender, plugin.getEffectConfig().customizePermission())) {
            return List.of();
        }

        PlayerEffectProfile profile = plugin.getPlayerProfileManager().getProfile(player);
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> List.of();
            case "clone" -> args.length == 3
                    ? complete(args[2], plugin.getEffectService().availablePresetNames(player))
                    : List.of();
            case "rename", "delete", "set" -> {
                if (args.length == 3) {
                    yield complete(args[2], new ArrayList<>(profile.presets().keySet()));
                }
                if ("set".equalsIgnoreCase(args[1]) && args.length == 4) {
                    yield complete(args[3], PRESET_PROPERTIES);
                }
                if ("set".equalsIgnoreCase(args[1]) && args.length == 5 && isAnimationProperty(args[3])) {
                    yield complete(args[4], Arrays.stream(AnimationType.values()).map(Enum::name).toList());
                }
                if ("set".equalsIgnoreCase(args[1]) && args.length == 5 && isSoundTypeProperty(args[3])) {
                    yield complete(args[4], Arrays.stream(Sound.values()).map(sound -> sound.name()).limit(40).toList());
                }
                yield List.of();
            }
            case "info" -> args.length == 3
                    ? complete(args[2], plugin.getEffectService().availablePresetNames(player))
                    : List.of();
            case "particle" -> completeParticle(profile, args);
            default -> List.of();
        };
    }

    private List<String> completeParticle(PlayerEffectProfile profile, String[] args) {
        if (args.length == 3) {
            return complete(args[2], PARTICLE_ACTIONS);
        }
        if (args.length == 4) {
            return complete(args[3], new ArrayList<>(profile.presets().keySet()));
        }
        if ("add".equalsIgnoreCase(args[2]) && args.length == 5) {
            return complete(args[4], Arrays.stream(Particle.values()).map(Enum::name).limit(60).toList());
        }
        if (("remove".equalsIgnoreCase(args[2]) || "set".equalsIgnoreCase(args[2])) && args.length == 5) {
            return complete(args[4], particleIndices(profile.getPreset(args[3])));
        }
        if ("set".equalsIgnoreCase(args[2]) && args.length == 6) {
            return complete(args[5], PARTICLE_PROPERTIES);
        }
        if ("set".equalsIgnoreCase(args[2]) && args.length == 7 && "particle".equalsIgnoreCase(args[5])) {
            return complete(args[6], Arrays.stream(Particle.values()).map(Enum::name).limit(60).toList());
        }
        return List.of();
    }

    private List<String> completeBind(CommandSender sender, Player player, String[] args) {
        if (args.length == 2) {
            return complete(args[1], BIND_ACTIONS);
        }
        if (player == null || !hasPermission(sender, plugin.getEffectConfig().customizePermission())) {
            return List.of();
        }

        if (args.length == 3) {
            return complete(args[2], EffectTrigger.keys());
        }
        if ((args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) && args.length == 4) {
            return complete(args[3], plugin.getEffectService().availablePresetNames(player));
        }
        return List.of();
    }

    private List<String> particleIndices(EffectPreset preset) {
        if (preset == null || preset.particles().isEmpty()) {
            return List.of();
        }

        List<String> indices = new ArrayList<>();
        for (int i = 1; i <= preset.particles().size(); i++) {
            indices.add(String.valueOf(i));
        }
        return indices;
    }

    private List<String> complete(String token, List<String> options) {
        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, completions);
        completions.sort(String.CASE_INSENSITIVE_ORDER);
        return completions;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(color(plugin.getEffectConfig().messages().prefix() + message));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private String normalizeName(String raw) {
        return PlayerEffectProfile.normalizePresetName(raw);
    }

    private String normalizeKey(String raw) {
        return raw.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private EffectTrigger parseTrigger(String raw) {
        return EffectTrigger.fromString(raw);
    }

    private boolean parseBoolean(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new IllegalArgumentException("Boolean value expected.");
        };
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Integer value expected.");
        }
    }

    private int parseIndex(String raw, int size) {
        int index = parseInt(raw) - 1;
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Particle index is out of range.");
        }
        return index;
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Number value expected.");
        }
    }

    private AnimationType parseAnimation(String raw) {
        try {
            return AnimationType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown animation. Use /particlefx animations.");
        }
    }

    private Particle parseParticle(String raw) {
        try {
            return Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown particle type.");
        }
    }

    private Sound parseSound(String raw) {
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown sound type.");
        }
    }

    private Color parseColor(String raw) {
        String value = raw.trim();
        try {
            if (value.startsWith("#") && value.length() == 7) {
                return Color.fromRGB(Integer.parseInt(value.substring(1), 16));
            }

            String[] split = value.split(",");
            if (split.length == 3) {
                return Color.fromRGB(
                        clampColor(split[0]),
                        clampColor(split[1]),
                        clampColor(split[2])
                );
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Color must be #RRGGBB or r,g,b.");
        }
        throw new IllegalArgumentException("Color must be #RRGGBB or r,g,b.");
    }

    private int clampColor(String raw) {
        int value = parseInt(raw.trim());
        return Math.max(0, Math.min(255, value));
    }

    private String clearableString(String raw) {
        String value = raw.trim();
        if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear") || value.equals("-")) {
            return "";
        }
        return value;
    }

    private boolean isAnimationProperty(String property) {
        return "animation".equalsIgnoreCase(property);
    }

    private boolean isSoundTypeProperty(String property) {
        return normalizeKey(property).equals("sound-type");
    }

    private float existingSize(ParticleSpec particleSpec, float fallback) {
        if (particleSpec.dustOptions() != null) {
            return particleSpec.dustOptions().getSize();
        }
        if (particleSpec.dustTransition() != null) {
            return particleSpec.dustTransition().getSize();
        }
        return fallback;
    }

    private Color existingFromColor(ParticleSpec particleSpec) {
        if (particleSpec.dustOptions() != null) {
            return particleSpec.dustOptions().getColor();
        }
        if (particleSpec.dustTransition() != null) {
            return particleSpec.dustTransition().getColor();
        }
        return Color.WHITE;
    }

    private Color existingToColor(ParticleSpec particleSpec) {
        if (particleSpec.dustTransition() != null) {
            return particleSpec.dustTransition().getToColor();
        }
        return Color.AQUA;
    }
}
