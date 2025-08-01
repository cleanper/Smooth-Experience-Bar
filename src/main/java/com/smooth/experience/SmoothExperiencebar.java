package com.smooth.experience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Math;

public class SmoothExperiencebar {
	private static final ResourceLocation ICONS = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
	private static final float SMOOTHNESS = 0.1f;
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
	private int lastProgressWidth = -1;

	public SmoothExperiencebar() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onRenderGui(RenderGuiEvent.Post event) {
		renderExperienceBar(event.getGuiGraphics());
		renderExperienceLevel(event.getGuiGraphics());
	}

	private void renderExperienceBar(GuiGraphics context) {
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
		expProgress = Math.lerp(expProgress, targetExp, SMOOTHNESS);
		int progressWidth = (int)(BAR_WIDTH * expProgress);

		if (progressWidth != lastProgressWidth) {
			context.flush();
			context.blit(ICONS, cachedX, cachedY, BACKGROUND_U, BACKGROUND_V, BAR_WIDTH, BAR_HEIGHT);
			context.blit(ICONS, cachedX, cachedY, BACKGROUND_U, FOREGROUND_V, progressWidth, BAR_HEIGHT);
			lastProgressWidth = progressWidth;
		}
	}

	private void renderExperienceLevel(GuiGraphics context) {
		Player player = client.player;
		if (player == null || player.isCreative() || player.isSpectator()) return;

		int scaledWidth = client.getWindow().getGuiScaledWidth();
		int scaledHeight = client.getWindow().getGuiScaledHeight();
		int x = scaledWidth / 2;
		int y = scaledHeight - 31 - 4;
		String levelText = String.valueOf(player.experienceLevel);
		int textWidth = client.font.width(levelText);

		context.drawString(client.font, levelText, x - textWidth / 2, y, 0x80FF20, true);
	}
}
