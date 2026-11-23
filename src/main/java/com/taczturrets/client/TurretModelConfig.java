package com.taczturrets.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.taczturrets.TaczTurrets;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Live tunable gun-model rotations, persisted in {@code config/taczturrets-model.json}.
 * <p>
 * The renderers read these fields every frame, so the {@code /tczmodel} client command changes what is drawn
 * immediately - no game restart and no reload is needed. Values are model corrections applied about the turret
 * origin after the aim rotation: first Y (yaw), then X (pitch), then Z (roll).
 */
public final class TurretModelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("taczturrets-model.json");

    /** Fixed turret (base 1): Y, X, Z. */
    public static float fixedYaw = 90.0F;
    public static float fixedPitch = 0.0F;
    public static float fixedRoll = 0.0F;
    /** Seat / rotating turret (base 2): Y, X, Z. */
    public static float seatYaw = 90.0F;
    public static float seatPitch = 0.0F;
    public static float seatRoll = 0.0F;
    /** Gun mounted on a CBC cannon mount (base 3): Y, X, Z. */
    public static float mountYaw = 90.0F;
    public static float mountPitch = 0.0F;
    public static float mountRoll = 0.0F;

    private static boolean loaded;

    private TurretModelConfig() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(FILE)) {
            return;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null) {
                return;
            }
            fixedYaw = read(json, "fixedYaw", fixedYaw);
            fixedPitch = read(json, "fixedPitch", fixedPitch);
            fixedRoll = read(json, "fixedRoll", fixedRoll);
            seatYaw = read(json, "seatYaw", seatYaw);
            seatPitch = read(json, "seatPitch", seatPitch);
            seatRoll = read(json, "seatRoll", seatRoll);
            mountYaw = read(json, "mountYaw", mountYaw);
            mountPitch = read(json, "mountPitch", mountPitch);
            mountRoll = read(json, "mountRoll", mountRoll);
            TaczTurrets.LOGGER.info("[taczturrets] loaded model angles from {}", FILE);
        } catch (Exception exception) {
            TaczTurrets.LOGGER.warn("[taczturrets] could not read model angles from {}", FILE, exception);
        }
    }

    public static synchronized void save() {
        JsonObject json = new JsonObject();
        json.addProperty("fixedYaw", fixedYaw);
        json.addProperty("fixedPitch", fixedPitch);
        json.addProperty("fixedRoll", fixedRoll);
        json.addProperty("seatYaw", seatYaw);
        json.addProperty("seatPitch", seatPitch);
        json.addProperty("seatRoll", seatRoll);
        json.addProperty("mountYaw", mountYaw);
        json.addProperty("mountPitch", mountPitch);
        json.addProperty("mountRoll", mountRoll);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(json), StandardCharsets.UTF_8);
            TaczTurrets.LOGGER.info("[taczturrets] saved model angles to {}", FILE);
        } catch (IOException exception) {
            TaczTurrets.LOGGER.warn("[taczturrets] could not save model angles to {}", FILE, exception);
        }
    }

    private static float read(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    /** @return a human readable dump used by {@code /tczmodel list} */
    public static String describe() {
        return String.format(java.util.Locale.ROOT,
                "fixed: Y=%.1f X=%.1f Z=%.1f | seat: Y=%.1f X=%.1f Z=%.1f | mount: Y=%.1f X=%.1f Z=%.1f",
                fixedYaw, fixedPitch, fixedRoll, seatYaw, seatPitch, seatRoll, mountYaw, mountPitch, mountRoll);
    }

    /** Forces a re-read of the config file (used by {@code /tczmodel reload}). */
    public static synchronized void reload() {
        loaded = false;
        load();
    }

    /** @return the current value for a target/axis pair, or {@code NaN} when the pair is unknown */
    public static float get(String target, String axis) {
        return switch (target) {
            case "fixed" -> switch (axis) {
                case "yaw" -> fixedYaw;
                case "pitch" -> fixedPitch;
                case "roll" -> fixedRoll;
                default -> Float.NaN;
            };
            case "seat" -> switch (axis) {
                case "yaw" -> seatYaw;
                case "pitch" -> seatPitch;
                case "roll" -> seatRoll;
                default -> Float.NaN;
            };
            case "mount" -> switch (axis) {
                case "yaw" -> mountYaw;
                case "pitch" -> mountPitch;
                case "roll" -> mountRoll;
                default -> Float.NaN;
            };
            default -> Float.NaN;
        };
    }

    /** Applies one axis change for a target name ({@code fixed}, {@code seat} or {@code mount}). */
    public static boolean set(String target, String axis, float degrees) {
        switch (target) {
            case "fixed" -> {
                switch (axis) {
                    case "yaw" -> {
                        fixedYaw = degrees;
                        return true;
                    }
                    case "pitch" -> {
                        fixedPitch = degrees;
                        return true;
                    }
                    case "roll" -> {
                        fixedRoll = degrees;
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            }
            case "seat" -> {
                switch (axis) {
                    case "yaw" -> {
                        seatYaw = degrees;
                        return true;
                    }
                    case "pitch" -> {
                        seatPitch = degrees;
                        return true;
                    }
                    case "roll" -> {
                        seatRoll = degrees;
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            }
            case "mount" -> {
                switch (axis) {
                    case "yaw" -> {
                        mountYaw = degrees;
                        return true;
                    }
                    case "pitch" -> {
                        mountPitch = degrees;
                        return true;
                    }
                    case "roll" -> {
                        mountRoll = degrees;
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            }
            default -> {
                return false;
            }
        }
    }
}
