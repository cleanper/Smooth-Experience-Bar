package com.smooth.experience;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Math;

public class SmoothExperiencebar implements ClientModInitializer {
	private static final Identifier ICONS=new Identifier("textures/gui/icons.png");
	private static final RenderLayer EXPERIENCE_BAR_LAYER=RenderLayer.getText(ICONS);
	private static final float SMOOTHNESS=0.1f;
	private static final int BAR_WIDTH=182;
	private static final int BAR_HEIGHT=5;
	private static final int BACKGROUND_U=0;
	private static final int BACKGROUND_V=64;
	private static final int FOREGROUND_V=69;
	private final MinecraftClient client=MinecraftClient.getInstance();
	private float expProgress=0;
	private int lastScaledWidth=-1;
	private int lastScaledHeight=-1;
	private int cachedX=0;
	private int cachedY=0;
	private int lastProgressWidth=-1;

	@Override public void onInitializeClient() {
		HudRenderCallback.EVENT.register((context,tickDelta)->{
			renderExperienceBar(context);
			renderExperienceLevel(context);
		});
	}

	private void renderExperienceBar(DrawContext context) {
		PlayerEntity player=client.player;
		if(player==null||player.isCreative()||player.isSpectator())return;
		int scaledWidth=client.getWindow().getScaledWidth();
		int scaledHeight=client.getWindow().getScaledHeight();
		if(scaledWidth!=lastScaledWidth||scaledHeight!=lastScaledHeight){
			cachedX=scaledWidth/2-91;
			cachedY=scaledHeight-32+3;
			lastScaledWidth=scaledWidth;
			lastScaledHeight=scaledHeight;
		}

		float targetExp=player.experienceProgress;
		expProgress=Math.lerp(expProgress,targetExp,SMOOTHNESS);
		int progressWidth=(int)(BAR_WIDTH*expProgress);
		if(progressWidth!=lastProgressWidth){
			VertexConsumerProvider.Immediate consumers=context.getVertexConsumers();
			consumers.draw(EXPERIENCE_BAR_LAYER);
			context.drawTexture(ICONS,cachedX,cachedY,BACKGROUND_U,BACKGROUND_V,BAR_WIDTH,BAR_HEIGHT);
			context.drawTexture(ICONS,cachedX,cachedY,BACKGROUND_U,FOREGROUND_V,progressWidth,BAR_HEIGHT);
			consumers.draw();
			lastProgressWidth=progressWidth;
		}
	}

	private void renderExperienceLevel(DrawContext context) {
		PlayerEntity player=client.player;
		if(player==null||player.isCreative()||player.isSpectator())return;
		int scaledWidth=client.getWindow().getScaledWidth();
		int scaledHeight=client.getWindow().getScaledHeight();
		int x=scaledWidth/2;
		int y=scaledHeight-31-4;
		String levelText=String.valueOf(player.experienceLevel);
		int textWidth=client.textRenderer.getWidth(levelText);
		context.drawText(client.textRenderer,levelText,x-textWidth/2,y,0x80FF20,true);
	}
}
