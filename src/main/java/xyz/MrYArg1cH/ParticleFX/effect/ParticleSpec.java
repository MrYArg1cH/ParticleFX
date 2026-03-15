package xyz.MrYArg1cH.ParticleFX.effect;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public record ParticleSpec(
        Particle particle,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        Particle.DustOptions dustOptions,
        Particle.DustTransition dustTransition,
        String blockData,
        String itemMaterial
) {
    public static ParticleSpec fromMap(Map<?, ?> rawMap, Logger logger, String path) {
        MemoryConfiguration section = new MemoryConfiguration();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                section.set(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return fromSection(section, logger, path);
    }

    public static ParticleSpec fromSection(ConfigurationSection section, Logger logger, String path) {
        String particleName = section.getString("particle", "END_ROD");
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            logger.warning("Invalid particle '" + particleName + "' at " + path + ".particle, using END_ROD.");
            particle = Particle.END_ROD;
        }

        int count = Math.max(1, section.getInt("count", 1));
        double offsetX = section.getDouble("offset-x", 0.0D);
        double offsetY = section.getDouble("offset-y", 0.0D);
        double offsetZ = section.getDouble("offset-z", 0.0D);
        double extra = section.getDouble("extra", 0.0D);

        Particle.DustOptions dust = null;
        Particle.DustTransition transition = null;
        String blockData = "";
        String itemMaterial = "";

        if (particle == Particle.DUST) {
            Color color = parseColor(section.getString("color", "255,255,255"), logger, path + ".color");
            float size = (float) section.getDouble("size", 1.0D);
            dust = new Particle.DustOptions(color, Math.max(0.2F, size));
        } else if (particle == Particle.DUST_COLOR_TRANSITION) {
            Color from = parseColor(section.getString("from-color", "255,255,255"), logger, path + ".from-color");
            Color to = parseColor(section.getString("to-color", "120,255,200"), logger, path + ".to-color");
            float size = (float) section.getDouble("size", 1.0D);
            transition = new Particle.DustTransition(from, to, Math.max(0.2F, size));
        } else if (BlockData.class.isAssignableFrom(particle.getDataType())) {
            blockData = section.getString("block-data", "minecraft:stone");
        } else if (ItemStack.class.isAssignableFrom(particle.getDataType())) {
            itemMaterial = section.getString("item", "STONE");
        }

        return new ParticleSpec(
                particle,
                count,
                offsetX,
                offsetY,
                offsetZ,
                extra,
                dust,
                transition,
                blockData == null ? "" : blockData,
                itemMaterial == null ? "" : itemMaterial
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("particle", particle.name());
        values.put("count", count);
        values.put("offset-x", offsetX);
        values.put("offset-y", offsetY);
        values.put("offset-z", offsetZ);
        values.put("extra", extra);

        if (particle == Particle.DUST && dustOptions != null) {
            values.put("color", serializeColor(dustOptions.getColor()));
            values.put("size", dustOptions.getSize());
        } else if (particle == Particle.DUST_COLOR_TRANSITION && dustTransition != null) {
            values.put("from-color", serializeColor(dustTransition.getColor()));
            values.put("to-color", serializeColor(dustTransition.getToColor()));
            values.put("size", dustTransition.getSize());
        } else if (!blockData.isBlank()) {
            values.put("block-data", blockData);
        } else if (!itemMaterial.isBlank()) {
            values.put("item", itemMaterial);
        }

        return values;
    }

    public void spawn(World world, Location location) {
        if (world == null || location == null) {
            return;
        }

        try {
            if (particle == Particle.DUST && dustOptions != null) {
                world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, dustOptions);
                return;
            }

            if (particle == Particle.DUST_COLOR_TRANSITION && dustTransition != null) {
                world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, dustTransition);
                return;
            }

            if (BlockData.class.isAssignableFrom(particle.getDataType())) {
                BlockData parsedBlockData = parseBlockData(blockData);
                if (parsedBlockData != null) {
                    world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, parsedBlockData);
                }
                return;
            }

            if (ItemStack.class.isAssignableFrom(particle.getDataType())) {
                ItemStack itemStack = parseItem(itemMaterial);
                if (itemStack != null) {
                    world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, itemStack);
                }
                return;
            }

            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static BlockData parseBlockData(String raw) {
        if (raw == null || raw.isBlank()) {
            return Material.STONE.createBlockData();
        }

        try {
            return Bukkit.createBlockData(raw);
        } catch (IllegalArgumentException ignored) {
            Material material = Material.matchMaterial(raw);
            return material == null ? Material.STONE.createBlockData() : material.createBlockData();
        }
    }

    private static ItemStack parseItem(String raw) {
        Material material = raw == null || raw.isBlank() ? Material.STONE : Material.matchMaterial(raw);
        if (material == null || material.isAir()) {
            return new ItemStack(Material.STONE);
        }
        return new ItemStack(material);
    }

    private static Color parseColor(String raw, Logger logger, String path) {
        if (raw == null || raw.isBlank()) {
            return Color.WHITE;
        }

        String value = raw.trim();
        try {
            if (value.startsWith("#") && value.length() == 7) {
                int rgb = Integer.parseInt(value.substring(1), 16);
                return Color.fromRGB(rgb);
            }

            String[] split = value.split(",");
            if (split.length == 3) {
                int r = clampColor(split[0]);
                int g = clampColor(split[1]);
                int b = clampColor(split[2]);
                return Color.fromRGB(r, g, b);
            }
        } catch (Exception ignored) {
            logger.warning("Invalid color '" + raw + "' at " + path + ", using white.");
            return Color.WHITE;
        }

        logger.warning("Invalid color '" + raw + "' at " + path + ", using white.");
        return Color.WHITE;
    }

    private static String serializeColor(Color color) {
        if (color == null) {
            return "255,255,255";
        }
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    private static int clampColor(String value) {
        int parsed = Integer.parseInt(value.trim());
        return Math.max(0, Math.min(255, parsed));
    }

    public static final class Builder {
        private Particle particle = Particle.END_ROD;
        private int count = 1;
        private double offsetX = 0.0D;
        private double offsetY = 0.0D;
        private double offsetZ = 0.0D;
        private double extra = 0.0D;
        private Particle.DustOptions dustOptions;
        private Particle.DustTransition dustTransition;
        private String blockData = "";
        private String itemMaterial = "";

        private Builder() {
        }

        private Builder(ParticleSpec particleSpec) {
            particle = particleSpec.particle;
            count = particleSpec.count;
            offsetX = particleSpec.offsetX;
            offsetY = particleSpec.offsetY;
            offsetZ = particleSpec.offsetZ;
            extra = particleSpec.extra;
            dustOptions = particleSpec.dustOptions;
            dustTransition = particleSpec.dustTransition;
            blockData = particleSpec.blockData;
            itemMaterial = particleSpec.itemMaterial;
        }

        public Builder particle(Particle particle) {
            this.particle = particle == null ? Particle.END_ROD : particle;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder offsetX(double offsetX) {
            this.offsetX = offsetX;
            return this;
        }

        public Builder offsetY(double offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        public Builder offsetZ(double offsetZ) {
            this.offsetZ = offsetZ;
            return this;
        }

        public Builder extra(double extra) {
            this.extra = extra;
            return this;
        }

        public Builder dustOptions(Particle.DustOptions dustOptions) {
            this.dustOptions = dustOptions;
            return this;
        }

        public Builder dustTransition(Particle.DustTransition dustTransition) {
            this.dustTransition = dustTransition;
            return this;
        }

        public Builder blockData(String blockData) {
            this.blockData = blockData == null ? "" : blockData;
            return this;
        }

        public Builder itemMaterial(String itemMaterial) {
            this.itemMaterial = itemMaterial == null ? "" : itemMaterial;
            return this;
        }

        public ParticleSpec build() {
            Particle selectedParticle = particle == null ? Particle.END_ROD : particle;
            String blockDataValue = blockData == null ? "" : blockData.trim();
            String itemMaterialValue = itemMaterial == null ? "" : itemMaterial.trim();

            if (selectedParticle == Particle.DUST) {
                Particle.DustOptions resolvedDust = dustOptions == null
                        ? new Particle.DustOptions(Color.WHITE, 1.0F)
                        : dustOptions;
                return new ParticleSpec(
                        selectedParticle,
                        Math.max(1, count),
                        offsetX,
                        offsetY,
                        offsetZ,
                        extra,
                        resolvedDust,
                        null,
                        "",
                        ""
                );
            }

            if (selectedParticle == Particle.DUST_COLOR_TRANSITION) {
                Particle.DustTransition resolvedTransition = dustTransition == null
                        ? new Particle.DustTransition(Color.WHITE, Color.AQUA, 1.0F)
                        : dustTransition;
                return new ParticleSpec(
                        selectedParticle,
                        Math.max(1, count),
                        offsetX,
                        offsetY,
                        offsetZ,
                        extra,
                        null,
                        resolvedTransition,
                        "",
                        ""
                );
            }

            if (BlockData.class.isAssignableFrom(selectedParticle.getDataType())) {
                return new ParticleSpec(
                        selectedParticle,
                        Math.max(1, count),
                        offsetX,
                        offsetY,
                        offsetZ,
                        extra,
                        null,
                        null,
                        blockDataValue.isBlank() ? "minecraft:stone" : blockDataValue,
                        ""
                );
            }

            if (ItemStack.class.isAssignableFrom(selectedParticle.getDataType())) {
                return new ParticleSpec(
                        selectedParticle,
                        Math.max(1, count),
                        offsetX,
                        offsetY,
                        offsetZ,
                        extra,
                        null,
                        null,
                        "",
                        itemMaterialValue.isBlank() ? "STONE" : itemMaterialValue
                );
            }

            return new ParticleSpec(
                    selectedParticle,
                    Math.max(1, count),
                    offsetX,
                    offsetY,
                    offsetZ,
                    extra,
                    null,
                    null,
                    "",
                    ""
            );
        }
    }
}
