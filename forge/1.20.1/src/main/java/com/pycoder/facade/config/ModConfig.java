package com.pycoder.facade.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ModConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_MOD_IDS;
    public static final ForgeConfigSpec.DoubleValue HARDNESS;
    public static final ForgeConfigSpec.DoubleValue EXPLOSION_RESISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IN_SURVIVAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IN_ADVENTURE;
    public static final ForgeConfigSpec.DoubleValue GHOST_ALPHA;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("facades");
        ALLOWED_MOD_IDS = builder
                .comment("Mod ids whose blocks should receive facade variants. Default is only vanilla Minecraft.")
                .defineListAllowEmpty("allowedModIds", List.of("minecraft"), ModConfig::isString);
        HARDNESS = builder
                .comment("Hardness used by generated facade blocks. Zero means instant break by hand.")
                .defineInRange("hardness", 0.0D, 0.0D, 100.0D);
        EXPLOSION_RESISTANCE = builder
                .comment("Explosion resistance used by generated facade blocks.")
                .defineInRange("explosionResistance", 3600000.0D, 0.0D, 3600000.0D);
        ENABLE_IN_SURVIVAL = builder
                .comment("Whether facade blocks and tools are available in survival mode.")
                .define("enableInSurvival", true);
        ENABLE_IN_ADVENTURE = builder
                .comment("Whether facade blocks and tools are available in adventure mode.")
                .define("enableInAdventure", true);
        GHOST_ALPHA = builder
                .comment("Transparency alpha used while ghost mode is active.")
                .defineInRange("ghostAlpha", 0.3D, 0.0D, 1.0D);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    private ModConfig() {
    }

    private static boolean isString(Object value) {
        return value instanceof String string && !string.isBlank();
    }

    public static List<? extends String> configuredAllowedModIds() {
        List<String> fileValues = readAllowedModIdsFromToml();
        return fileValues.isEmpty() ? ALLOWED_MOD_IDS.get() : fileValues;
    }

    public static List<String> autoConfigureAllowedModIds(List<String> detectedModIds) {
        List<String> normalizedDetectedModIds = normalizeModIds(detectedModIds);
        List<String> fileValues = readAllowedModIdsFromToml();
        if (!fileValues.isEmpty() && !isDefaultAllowedModIds(fileValues)) {
            return fileValues;
        }
        if (!normalizedDetectedModIds.isEmpty()) {
            writeAllowedModIdsToToml(normalizedDetectedModIds);
            return normalizedDetectedModIds;
        }
        return normalizeModIds(ALLOWED_MOD_IDS.get());
    }

    private static List<String> readAllowedModIdsFromToml() {
        Path configPath = configPath();
        if (!Files.isRegularFile(configPath)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(configPath);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                String content = line.split("#", 2)[0].trim();
                if (!content.startsWith("allowedModIds")) {
                    continue;
                }
                StringBuilder rawArray = new StringBuilder(content);
                while (rawArray.indexOf("]") < 0 && ++index < lines.size()) {
                    rawArray.append(',').append(lines.get(index).split("#", 2)[0].trim());
                }
                String rawContent = rawArray.toString();
                int equalsIndex = rawContent.indexOf('=');
                int openIndex = rawContent.indexOf('[', equalsIndex + 1);
                int closeIndex = rawContent.indexOf(']', openIndex + 1);
                if (equalsIndex < 0 || openIndex < 0 || closeIndex < 0) {
                    return List.of();
                }
                return parseStringList(rawContent.substring(openIndex + 1, closeIndex));
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return List.of();
    }

    private static void writeAllowedModIdsToToml(List<String> allowedModIds) {
        Path configPath = configPath();
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.isRegularFile(configPath)) {
                Files.writeString(configPath, defaultToml(allowedModIds));
                return;
            }
            List<String> lines = Files.readAllLines(configPath);
            List<String> updatedLines = new ArrayList<>();
            boolean replacedAllowedModIds = false;
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                String content = line.split("#", 2)[0].trim();
                if (!content.startsWith("allowedModIds")) {
                    updatedLines.add(line);
                    continue;
                }
                replacedAllowedModIds = true;
                updatedLines.add(allowedModIdsLine(allowedModIds));
                while (content.indexOf(']') < 0 && index + 1 < lines.size()) {
                    index++;
                    content = lines.get(index).split("#", 2)[0].trim();
                }
            }
            if (!replacedAllowedModIds) {
                updatedLines.add("");
                updatedLines.add("[facades]");
                updatedLines.add(allowedModIdsLine(allowedModIds));
            }
            Files.write(configPath, updatedLines);
        } catch (IOException ignored) {
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("facadebypycoder-common.toml");
    }

    private static String defaultToml(List<String> allowedModIds) {
        return "[facades]\n"
                + allowedModIdsLine(allowedModIds) + "\n"
                + "hardness = 0.0\n"
                + "explosionResistance = 3600000.0\n"
                + "enableInSurvival = true\n"
                + "enableInAdventure = true\n"
                + "ghostAlpha = 0.3\n";
    }

    private static String allowedModIdsLine(List<String> allowedModIds) {
        return "allowedModIds = [" + String.join(", ", allowedModIds.stream().map(value -> "\"" + value + "\"").toList()) + "]";
    }

    private static List<String> normalizeModIds(List<? extends String> modIds) {
        List<String> result = new ArrayList<>();
        for (String modId : modIds) {
            String normalizedModId = modId.trim().toLowerCase(Locale.ROOT);
            if (!normalizedModId.isBlank() && !result.contains(normalizedModId)) {
                result.add(normalizedModId);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isDefaultAllowedModIds(List<String> modIds) {
        return modIds.size() == 1 && "minecraft".equals(modIds.get(0));
    }

    private static List<String> parseStringList(String rawList) {
        List<String> result = new ArrayList<>();
        for (String rawValue : rawList.split(",")) {
            String value = rawValue.trim();
            value = value.replace('“', '"').replace('”', '"').replace('‘', '\'').replace('’', '\'');
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            value = value.trim();
            if (!value.isBlank()) {
                result.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(result);
    }
}
