package com.smooth.experience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Math;

public class SmoothExperiencebar {
	private static final ResourceLocation ICONS = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
	private static final float SMOOTHNESS = 0.3f;
	private static final float THRESHOLD = 0.002f;
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

	@SubscribeEvent
	public void onRenderGui(RenderGuiEvent.Post event) {
		Player player = client.player;
		if (player == null || player.isCreative() || player.isSpectator()) return;

		int scaledWidth = client.getWindow().getGuiScaledWidth();
		int scaledHeight = client.getWindow().getGuiScaledHeight();

		if (scaledWidth != lastScaledWidth || scaledHeight != lastScaledHeight) {
			cachedX = scaledWidth / 2 - 91;
			cachedY = scaledHeight - 32 + 3;
			lastScaledWidth = scaledWidth;
			lastScaledHeight = scaledHeight;
		}

        float targetExp = player.experienceProgress;
		float diff = Math.abs(targetExp - expProgress);

		if (diff > THRESHOLD) {
			expProgress = Math.lerp(expProgress, targetExp, SMOOTHNESS);
			if (diff < THRESHOLD * 2) {
				expProgress = targetExp;
			}

			int progressWidth = (int)(BAR_WIDTH * expProgress);
			GuiGraphics context = event.getGuiGraphics();
			context.blit(ICONS, cachedX, cachedY, BACKGROUND_U, BACKGROUND_V, BAR_WIDTH, BAR_HEIGHT);
			if (progressWidth > 0) {
				context.blit(ICONS, cachedX, cachedY, BACKGROUND_U, FOREGROUND_V, progressWidth, BAR_HEIGHT);
			}
		}

		renderExperienceLevel(event.getGuiGraphics());
	}

	private void renderExperienceLevel(GuiGraphics context) {
		Player player = client.player;
		if (player == null || player.isCreative() || player.isSpectator()) return;

		int x = lastScaledWidth / 2;
		int y = lastScaledHeight - 31 - 4;
		String levelText = String.valueOf(player.experienceLevel);
		context.drawString(client.font, levelText, x - client.font.width(levelText) / 2, y, 0x80FF20, true);
	}
}
