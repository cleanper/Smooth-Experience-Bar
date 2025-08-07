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

public class SmoothExperiencebar implements ClientModInitializer {
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final RenderLayer EXPERIENCE_BAR_LAYER = RenderLayer.getText(ICONS);
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BACKGROUND_U = 0;
    private static final int BACKGROUND_V = 64;
    private static final int FOREGROUND_V = 69;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Vector2i windowSizeCache = new Vector2i(-1, -1);
    private final Vector2i barPositionCache = new Vector2i();
    private final Vector2i levelTextPosition = new Vector2i();
    private final Vector4f textColor = new Vector4f(0.5f, 1.0f, 0.125f, 1.0f);
    private final Vector4f tmpColor = new Vector4f();
    private final Vector3f christmasColor = new Vector3f();

    private float expProgress = 0;
    private int lastProgressWidth = -1;
    private int lastLevel = -1;
    private long lastTime = 0;

    private Config config;
    private Path configPath;

    private static class Config {
        boolean enableGradient = false;
        float smoothness = 0.05f;
        float minGradientIntensity = 0.7f;
        boolean christmasMode = false;
        float christmasSpeed = 0.5f;
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            renderExperienceBar(context);
            renderExperienceLevel(context);
        });
    }

    private void loadConfig() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("smooth_experiencebar.json");
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                config = new Gson().fromJson(json, Config.class);
            } else {
                config = new Config();
                saveConfig();
            }
        } catch (IOException e) {
            config = new Config();
        }
    }

    private void saveConfig() {
        try {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(config);
            Files.writeString(configPath, json);
        } catch (IOException ignored) {}
    }

    private boolean isChristmasSeason() {
        LocalDate now = LocalDate.now();
        return (now.getMonthValue() == 12 && now.getDayOfMonth() >= 15) ||
                (now.getMonthValue() == 1 && now.getDayOfMonth() <= 5);
    }

    private void updateChristmasColor() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < 16) return;
        lastTime = currentTime;

        float time = currentTime * 0.001f * config.christmasSpeed;
        float r = 0.5f + 0.5f * Math.sin(time * 1.1f);
        float g = 0.5f + 0.5f * Math.sin(time * 1.3f + 2.094f);
        float b = 0.5f + 0.5f * Math.sin(time * 1.5f + 4.188f);
        christmasColor.set(r, g, b);
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
        expProgress = Math.lerp(expProgress, targetExp, config.smoothness);
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
        if (currentLevel != lastLevel) {
            String levelText = String.valueOf(currentLevel);
            int textWidthCache = client.textRenderer.getWidth(levelText);
            levelTextPosition.set((windowSizeCache.x >> 1) - (textWidthCache >> 1), windowSizeCache.y - 35);
            lastLevel = currentLevel;
        }

        if (config.christmasMode && isChristmasSeason()) {
            updateChristmasColor();
            int color = ((int)(christmasColor.x * 255) << 16) |
                    ((int)(christmasColor.y * 255) << 8) |
                    (int)(christmasColor.z * 255);
            context.drawText(client.textRenderer, String.valueOf(currentLevel), levelTextPosition.x, levelTextPosition.y, color | 0xFF000000, true);
        } else if (config.enableGradient) {
            tmpColor.set(textColor);
            context.setShaderColor(tmpColor.x, tmpColor.y, tmpColor.z, tmpColor.w);
            context.drawText(client.textRenderer, String.valueOf(currentLevel), levelTextPosition.x, levelTextPosition.y, 0x80FF20, true);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            context.drawText(client.textRenderer, String.valueOf(currentLevel), levelTextPosition.x, levelTextPosition.y, 0x80FF20, true);
        }
    }
}
