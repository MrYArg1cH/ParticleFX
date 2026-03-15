package xyz.MrYArg1cH.ParticleFX.profile;

import xyz.MrYArg1cH.ParticleFX.config.EffectConfig;
import xyz.MrYArg1cH.ParticleFX.effect.EffectPreset;
import xyz.MrYArg1cH.ParticleFX.effect.EffectTrigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerEffectProfile {
    private final UUID uniqueId;
    private String lastKnownName;
    private final Map<String, EffectPreset> presets;
    private final EnumMap<EffectTrigger, List<String>> bindings;

    public PlayerEffectProfile(UUID uniqueId, String lastKnownName) {
        this(uniqueId, lastKnownName, new LinkedHashMap<>(), new EnumMap<>(EffectTrigger.class));
    }

    public PlayerEffectProfile(
            UUID uniqueId,
            String lastKnownName,
            Map<String, EffectPreset> presets,
            EnumMap<EffectTrigger, List<String>> bindings
    ) {
        this.uniqueId = uniqueId;
        this.lastKnownName = lastKnownName;
        this.presets = presets;
        this.bindings = bindings;
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public Map<String, EffectPreset> presets() {
        return Collections.unmodifiableMap(presets);
    }

    public Map<EffectTrigger, List<String>> bindings() {
        EnumMap<EffectTrigger, List<String>> copy = new EnumMap<>(EffectTrigger.class);
        for (Map.Entry<EffectTrigger, List<String>> entry : bindings.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public boolean hasPreset(String name) {
        String normalizedName = normalizePresetName(name);
        return normalizedName != null && presets.containsKey(normalizedName);
    }

    public EffectPreset getPreset(String name) {
        String normalizedName = normalizePresetName(name);
        return normalizedName == null ? null : presets.get(normalizedName);
    }

    public void putPreset(String name, EffectPreset preset) {
        String normalizedName = normalizePresetName(name);
        if (normalizedName != null && preset != null) {
            presets.put(normalizedName, preset);
        }
    }

    public EffectPreset removePreset(String name) {
        String normalizedName = normalizePresetName(name);
        if (normalizedName == null) {
            return null;
        }

        EffectPreset removed = presets.remove(normalizedName);
        if (removed != null) {
            for (Map.Entry<EffectTrigger, List<String>> entry : bindings.entrySet()) {
                List<String> updated = new ArrayList<>(entry.getValue());
                updated.removeIf(normalizedName::equals);
                entry.setValue(updated);
            }
        }
        return removed;
    }

    public boolean renamePreset(String oldName, String newName) {
        String oldNormalized = normalizePresetName(oldName);
        String newNormalized = normalizePresetName(newName);
        if (oldNormalized == null || newNormalized == null || oldNormalized.equals(newNormalized)) {
            return false;
        }

        EffectPreset preset = presets.remove(oldNormalized);
        if (preset == null) {
            return false;
        }

        presets.put(newNormalized, preset);
        for (Map.Entry<EffectTrigger, List<String>> entry : bindings.entrySet()) {
            List<String> updated = new ArrayList<>();
            for (String presetName : entry.getValue()) {
                updated.add(oldNormalized.equals(presetName) ? newNormalized : presetName);
            }
            entry.setValue(updated);
        }
        return true;
    }

    public boolean hasBindingOverride(EffectTrigger trigger) {
        return bindings.containsKey(trigger);
    }

    public List<String> bindingOverride(EffectTrigger trigger) {
        return bindings.getOrDefault(trigger, List.of());
    }

    public void addBinding(EffectTrigger trigger, String presetName) {
        String normalizedName = normalizePresetName(presetName);
        if (trigger == null || normalizedName == null) {
            return;
        }

        List<String> updated = new ArrayList<>(bindings.getOrDefault(trigger, List.of()));
        if (!updated.contains(normalizedName)) {
            updated.add(normalizedName);
        }
        bindings.put(trigger, updated);
    }

    public void setBinding(EffectTrigger trigger, List<String> presetNames) {
        if (trigger == null) {
            return;
        }

        List<String> updated = new ArrayList<>();
        if (presetNames != null) {
            for (String presetName : presetNames) {
                String normalizedName = normalizePresetName(presetName);
                if (normalizedName != null && !updated.contains(normalizedName)) {
                    updated.add(normalizedName);
                }
            }
        }
        bindings.put(trigger, updated);
    }

    public void removeBinding(EffectTrigger trigger, String presetName) {
        String normalizedName = normalizePresetName(presetName);
        if (trigger == null || normalizedName == null) {
            return;
        }

        List<String> updated = new ArrayList<>(bindings.getOrDefault(trigger, List.of()));
        updated.removeIf(normalizedName::equals);
        bindings.put(trigger, updated);
    }

    public void clearBinding(EffectTrigger trigger) {
        if (trigger != null) {
            bindings.put(trigger, new ArrayList<>());
        }
    }

    public void resetBinding(EffectTrigger trigger) {
        if (trigger != null) {
            bindings.remove(trigger);
        }
    }

    public static String normalizePresetName(String value) {
        return EffectConfig.normalizePresetName(value);
    }
}
