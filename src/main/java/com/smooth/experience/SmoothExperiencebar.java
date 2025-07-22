package com.smooth.experience;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class SmoothExperiencebar implements ClientModInitializer {
	private static final MinecraftClient client = MinecraftClient.getInstance();
	private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
	private static float playerExpProgress = 0;
	private static float villagerExpProgress = 0;
	private static final float SMOOTHNESS = 0.15f;
	private static int lastPlayerExpLevel = 0;
	private static int lastVillagerExpLevel = 0;

	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register(this::renderExperienceBar);
	}

	private void renderExperienceBar(DrawContext context, float tickDelta) {
		if (client.player == null) return;

		PlayerEntity player = client.player;
		float targetPlayerExp = player.experienceProgress;
		playerExpProgress = lerp(playerExpProgress, targetPlayerExp);
		int playerExpLevel = player.experienceLevel;

		int villagerExpLevel;
		if (client.currentScreen != null && client.currentScreen.getTitle().getString().contains("Trading")) {
			float targetVillagerExp = getVillagerExperienceProgress();
			villagerExpProgress = lerp(villagerExpProgress, targetVillagerExp);
			villagerExpLevel = (int) (villagerExpProgress * 10);
		} else {
			villagerExpProgress = 0;
			villagerExpLevel = 0;
		}

		renderPlayerExperienceBar(context, playerExpProgress, playerExpLevel);
		lastPlayerExpLevel = playerExpLevel;

		if (villagerExpLevel > 0) {
			renderVillagerExperienceBar(context, villagerExpProgress, villagerExpLevel);
			lastVillagerExpLevel = villagerExpLevel;
		} else {
			lastVillagerExpLevel = 0;
		}
	}

	private float getVillagerExperienceProgress() {
		if (client.player == null || client.player.currentScreenHandler == null) return 0;
		if (client.player.currentScreenHandler.slots.size() <= 2) return 0;
		return client.player.currentScreenHandler.getSlot(2).getStack().getCount() / 10f;
	}

	private float lerp(float current, float target) {
		return current + (target - current) * SmoothExperiencebar.SMOOTHNESS;
	}

	private void renderPlayerExperienceBar(DrawContext context, float progress, int level) {
		if (client.interactionManager == null || client.interactionManager.hasExperienceBar()) {
			int width = client.getWindow().getScaledWidth();
			int height = client.getWindow().getScaledHeight();
			int x = width / 2 - 91;
			int y = height - 32 + 3;

			context.drawTexture(ICONS, x, y, 0, 64, 182, 5);
			context.drawTexture(ICONS, x, y, 0, 69, (int)(182 * progress), 5);

			if (level != lastPlayerExpLevel || progress >= 0.99f) {
				String levelText = String.valueOf(level);
				int textWidth = client.textRenderer.getWidth(levelText);
				context.drawText(client.textRenderer, levelText, x + 91 - textWidth / 2, y - 9, 8453920, true);
			}
		}
	}

	private void renderVillagerExperienceBar(DrawContext context, float progress, int level) {
		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		int x = width / 2 - 91;
		int y = height - 59;

		context.drawTexture(ICONS, x, y, 0, 64, 182, 5);
		context.drawTexture(ICONS, x, y, 0, 84, (int)(182 * progress), 5);

		if (level != lastVillagerExpLevel || progress >= 0.99f) {
			String levelText = String.valueOf(level);
			int textWidth = client.textRenderer.getWidth(levelText);
			context.drawText(client.textRenderer, levelText, x + 91 - textWidth / 2, y - 9, 0x20A0FF, true);
		}
	}
}
