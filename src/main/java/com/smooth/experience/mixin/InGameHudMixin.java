package com.smooth.experience.mixin;

import com.smooth.experience.SmoothExperiencebar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(
            method = "renderExperienceBar(Lnet/minecraft/client/gui/DrawContext;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableVanillaExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        if (SmoothExperiencebar.autohudDetected) return;
        if (SmoothExperiencebar.shouldDisableVanillaExperienceBar()) {
            ci.cancel();
        }
    }
}
