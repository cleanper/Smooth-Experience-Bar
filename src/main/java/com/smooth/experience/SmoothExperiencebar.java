package com.smooth.experience;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.*;
import com.google.gson.*;
import org.joml.Math;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public final class SmoothExperiencebar implements ClientModInitializer {
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final RenderLayer EXPERIENCE_BAR_LAYER = RenderLayer.getText(ICONS);
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BACKGROUND_U = 0;
    private static final int BACKGROUND_V = 64;
    private static final int FOREGROUND_V = 69;
    private static final int OUTLINE_THICKNESS = 1;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Vector2i windowSizeCache = new Vector2i(-1, -1);
    private final Vector2i barPositionCache = new Vector2i();
    private final Vector2i levelTextPosition = new Vector2i();
    private final Vector4f textColor = new Vector4f(0.5f, 1.0f, 0.125f, 1.0f);
    private final Vector4f tmpColor = new Vector4f();
    private final Vector3f outlineColor = new Vector3f(1.0f, 0.0f, 0.0f);

    private float expProgress = 0;
    private int lastProgressWidth = -1;
    private int lastLevel = -1;
    private boolean shouldRender = true;
    private boolean configLoaded = false;

    private Config config;
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
        shouldRender = !FabricLoader.getInstance().isModLoaded("autohud");
        loadConfig();
        HudRenderCallback.EVENT.register(this::onRender);
    }

    private void onRender(DrawContext context, float tickDelta) {
        if (!shouldRender || !config.enabled) return;
        renderExperienceBar(context);
        renderExperienceLevel(context);
    }

    private void loadConfig() {
        if (configLoaded) return;
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
        try {
            Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(config));
        } catch (IOException ignored) {}
    }

    private boolean isChristmasSeason() {
        LocalDate now = LocalDate.now();
        return (now.getMonthValue() == 12 && now.getDayOfMonth() >= 15) ||
                (now.getMonthValue() == 1 && now.getDayOfMonth() <= 5);
    }

    private void updateOutlineColor() {
        float time = System.currentTimeMillis() * 0.001f * config.outlineSpeed;
        outlineColor.x = 0.5f + 0.5f * Math.sin(time * 1.1f);
        outlineColor.y = 0.5f + 0.5f * Math.sin(time * 1.3f + 2.094f);
        outlineColor.z = 0.5f + 0.5f * Math.sin(time * 1.5f + 4.188f);
    }

    private void renderExperienceBar(DrawContext context) {
        PlayerEntity player = client.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        Vector2i currentSize = new Vector2i(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        if (!currentSize.equals(windowSizeCache)) {
            windowSizeCache.set(currentSize);
            barPositionCache.set((currentSize.x >> 1) - 91, currentSize.y - 29);
        }

        float targetExp = player.experienceProgress;
        expProgress = config.enableSmoothing ? Math.lerp(expProgress, targetExp, config.smoothness) : targetExp;
        int progressWidth = (int)(BAR_WIDTH * expProgress);

        if (progressWidth != lastProgressWidth) {
            VertexConsumerProvider.Immediate consumers = context.getVertexConsumers();
            consumers.draw(EXPERIENCE_BAR_LAYER);
            context.drawTexture(ICONS, barPositionCache.x, barPositionCache.y, BACKGROUND_U, BACKGROUND_V, BAR_WIDTH, BAR_HEIGHT);

            if (progressWidth > 0) {
                if (config.enableGradient) {
                    float intensity = config.minGradientIntensity + (1f - config.minGradientIntensity) * expProgress;
                    tmpColor.set(textColor).mul(intensity);
                    context.setShaderColor(tmpColor.x, tmpColor.y, tmpColor.z, tmpColor.w);
                }
                context.drawTexture(ICONS, barPositionCache.x, barPositionCache.y, BACKGROUND_U, FOREGROUND_V, progressWidth, BAR_HEIGHT);
                context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            consumers.draw();
            lastProgressWidth = progressWidth;
        }
    }

    private void renderExperienceLevel(DrawContext context) {
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
            int outlineColorValue = ((int)(outlineColor.x * 255) << 16) |
                    ((int)(outlineColor.y * 255) << 8) |
                    (int)(outlineColor.z * 255);

            for (int x = -OUTLINE_THICKNESS; x <= OUTLINE_THICKNESS; x++) {
                for (int y = -OUTLINE_THICKNESS; y <= OUTLINE_THICKNESS; y++) {
                    if (x != 0 || y != 0) {
                        context.drawText(client.textRenderer, levelText,
                                levelTextPosition.x + x, levelTextPosition.y + y, outlineColorValue | 0xFF000000, false);
                    }
                }
            }
        }

        context.drawText(client.textRenderer, levelText, levelTextPosition.x, levelTextPosition.y, textColorValue, true);
    }
}
