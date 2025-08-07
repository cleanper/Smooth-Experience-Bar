package com.smooth.experience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Math;
import org.joml.Vector3f;

public class SmoothExperiencebar {
    private static final ResourceLocation ICONS = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    private static final float SMOOTHNESS = 0.15f;
    private static final float THRESHOLD = 0.001f;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BACKGROUND_U = 0;
    private static final int BACKGROUND_V = 64;
    private static final int FOREGROUND_V = 69;

    private final Minecraft client = Minecraft.getInstance();
    private float expProgress = 0;
    private int lastScaledWidth = -1;
    private int lastScaledHeight = -1;
    private int cachedX = 0;
    private int cachedY = 0;
    private final Vector3f color = new Vector3f(0.5f, 1.0f, 0.125f);
    private int colorInt = 0;
    private final boolean hasAutoHud;

    public SmoothExperiencebar() {
        updateColorInt();
        hasAutoHud = ModList.get().isLoaded("autohud");
    }

    private void updateColorInt() {
        int r = (int)(color.x * 255);
        int g = (int)(color.y * 255);
        int b = (int)(color.z * 255);
        colorInt = (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (hasAutoHud) return;

        Player player = client.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        boolean shouldShow = false;
        if (client.gameMode != null) {
            shouldShow = client.gameMode.canHurtPlayer() && !player.isSpectator();
        }
        if (!shouldShow) return;

        float targetExp = player.experienceProgress;
        int scaledWidth = client.getWindow().getGuiScaledWidth();
        int scaledHeight = client.getWindow().getGuiScaledHeight();

        if (scaledWidth != lastScaledWidth || scaledHeight != lastScaledHeight) {
            cachedX = scaledWidth / 2 - 91;
            cachedY = scaledHeight - 32 + 3;
            lastScaledWidth = scaledWidth;
            lastScaledHeight = scaledHeight;
        }

        float diff = Math.abs(targetExp - expProgress);
        if (diff > THRESHOLD) {
            expProgress = Math.fma(targetExp - expProgress, SMOOTHNESS, expProgress);
            if (diff < THRESHOLD * 3) {
                expProgress = targetExp;
            }
        } else {
            expProgress = targetExp;
        }

        GuiGraphics context = event.getGuiGraphics();
        renderExperienceBar(context);
        renderExperienceLevel(context);
    }

    private void renderExperienceBar(GuiGraphics context) {
        context.pose().pushPose();
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.blit(ICONS, cachedX, cachedY, BACKGROUND_U, BACKGROUND_V, BAR_WIDTH, BAR_HEIGHT);

        int progressWidth = (int)(BAR_WIDTH * expProgress);
        if (progressWidth > 0) {
            context.blit(ICONS, cachedX, cachedY, BACKGROUND_U, FOREGROUND_V, progressWidth, BAR_HEIGHT);
        }

        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.pose().popPose();
    }

    private void renderExperienceLevel(GuiGraphics context) {
        Player player = client.player;
        if (player == null) return;

        context.pose().pushPose();
        int x = lastScaledWidth / 2;
        int y = lastScaledHeight - 31 - 4;
        String levelText = String.valueOf(player.experienceLevel);
        context.drawString(client.font, levelText, x - client.font.width(levelText) / 2, y, colorInt, false);
        context.pose().popPose();
    }
}
