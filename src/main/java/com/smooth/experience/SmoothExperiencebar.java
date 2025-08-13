package com.smooth.experience;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;
import org.joml.Vector3f;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public final class SmoothExperiencebar implements ClientModInitializer {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int OUTLINE_THICKNESS = 1;
    private static final Identifier BACKGROUND = Identifier.ofVanilla("hud/experience_bar_background");
    private static final Identifier PROGRESS = Identifier.ofVanilla("hud/experience_bar_progress");
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Vector2i windowSizeCache = new Vector2i(-1, -1);
    private final Vector2i barPositionCache = new Vector2i();
    private final Vector2i levelTextPosition = new Vector2i();
    private final Vector3f outlineColor = new Vector3f(1.0f, 0.0f, 0.0f);
    private float expProgress = 0;
    private int lastLevel = -1;
    private static boolean shouldRender = true;
    private boolean configLoaded = false;
    public static boolean incompatibleModDetected = false;
    public static boolean autohudDetected = false;
    private static Config config;
    private final Path configPath;

    public SmoothExperiencebar() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("smooth_experiencebar.json");
    }

    private static final class Config {
        boolean enabled = true;
        boolean enableSmoothing = true;
        boolean enableGradient = false;
        float smoothness = 0.05f;
        float minGradientIntensity = 0.7f;
        boolean outlineEffect = false;
        float outlineSpeed = 0.5f;
    }

    @Override
    public void onInitializeClient() {
        autohudDetected = FabricLoader.getInstance().isModLoaded("autohud");
        incompatibleModDetected = autohudDetected || FabricLoader.getInstance().isModLoaded("hudless");

        if (autohudDetected) {
            shouldRender = false;
            return;
        }

        shouldRender = !incompatibleModDetected;
        loadConfig();

        if (!autohudDetected) {
            HudElementRegistry.attachElementAfter(
                    Identifier.of("minecraft", "experience_level"),
                    Identifier.of("smooth_experiencebar", "experience_bar"),
                    this::onRender
            );
        }
    }

    private void onRender(DrawContext context, RenderTickCounter tickCounter) {
        if (autohudDetected || incompatibleModDetected || !shouldRender || !config.enabled) return;
        renderExperienceBar(context, tickCounter);
        renderExperienceLevel(context, tickCounter);
    }

    private void loadConfig() {
        if (configLoaded || autohudDetected) return;
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                config = new Gson().fromJson(json, Config.class);
                if (!json.contains("\"enabled\"")) {
                    config.enabled = true;
                    saveConfig();
                }
            } else {
                config = new Config();
                saveConfig();
            }
        } catch (IOException e) {
            config = new Config();
        }
        configLoaded = true;
    }

    private void saveConfig() {
        if (autohudDetected) return;
        try {
            Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(config));
        } catch (IOException ignored) {}
    }

    private boolean isChristmasSeason() {
        if (autohudDetected || incompatibleModDetected) return false;
        LocalDate now = LocalDate.now();
        return (now.getMonthValue() == 12 && now.getDayOfMonth() >= 15) || (now.getMonthValue() == 1 && now.getDayOfMonth() <= 5);
    }

    private void updateOutlineColor() {
        if (autohudDetected || incompatibleModDetected) return;
        float time = System.currentTimeMillis() * 0.001f * config.outlineSpeed;
        outlineColor.x = 0.5f + 0.5f * (float)Math.sin(time * 1.1f);
        outlineColor.y = 0.5f + 0.5f * (float)Math.sin(time * 1.3f + 2.094f);
        outlineColor.z = 0.5f + 0.5f * (float)Math.sin(time * 1.5f + 4.188f);
    }

    private void renderExperienceBar(DrawContext context, RenderTickCounter ignoredTickCounter) {
        if (autohudDetected || incompatibleModDetected) return;
        PlayerEntity player = client.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        Vector2i currentSize = new Vector2i(
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        if (!currentSize.equals(windowSizeCache)) {
            windowSizeCache.set(currentSize);
            barPositionCache.set((currentSize.x >> 1) - 91, currentSize.y - 29);
        }

        float targetExp = player.experienceProgress;
        expProgress = config.enableSmoothing
                ? expProgress + (targetExp - expProgress) * config.smoothness
                : targetExp;
        int progressWidth = (int)(BAR_WIDTH * expProgress);

        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, barPositionCache.x, barPositionCache.y, BAR_WIDTH, BAR_HEIGHT);

        if (progressWidth > 0) {
            if (config.enableGradient) {
                float intensity = config.minGradientIntensity + (1f - config.minGradientIntensity) * expProgress;
                int color = (int)(intensity * 255) << 24 | 0x80FF20;
                context.fill(
                        barPositionCache.x,
                        barPositionCache.y,
                        barPositionCache.x + progressWidth,
                        barPositionCache.y + BAR_HEIGHT,
                        color
                );
            } else {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, barPositionCache.x, barPositionCache.y, progressWidth, BAR_HEIGHT);
            }
        }
    }

    private void renderExperienceLevel(DrawContext context, RenderTickCounter ignoredTickCounter) {
        if (autohudDetected || incompatibleModDetected) return;
        PlayerEntity player = client.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        int currentLevel = player.experienceLevel;
        String levelText = String.valueOf(currentLevel);

        if (currentLevel != lastLevel) {
            int textWidthCache = client.textRenderer.getWidth(levelText);
            levelTextPosition.set((windowSizeCache.x >> 1) - (textWidthCache >> 1), windowSizeCache.y - 35);
            lastLevel = currentLevel;
        }

        int textColorValue = 0x80FF20;

        if (config.outlineEffect && isChristmasSeason()) {
            updateOutlineColor();
            int outlineColorValue = ((int)(outlineColor.x * 255) << 16 | ((int)(outlineColor.y * 255) << 8 | (int)(outlineColor.z * 255)));

            for (int x = -OUTLINE_THICKNESS; x <= OUTLINE_THICKNESS; x++) {
                for (int y = -OUTLINE_THICKNESS; y <= OUTLINE_THICKNESS; y++) {
                    if (x != 0 || y != 0) {
                        context.drawText(client.textRenderer, levelText, levelTextPosition.x + x, levelTextPosition.y + y, outlineColorValue | 0xFF000000, false);
                    }
                }
            }
        }

        context.drawText(client.textRenderer, levelText, levelTextPosition.x, levelTextPosition.y, textColorValue, true);
    }
}
