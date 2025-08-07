package com.smooth.experience;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("smooth_experience_bar")
public class SmoothExperienceBarMod {
    public SmoothExperienceBarMod() {
        MinecraftForge.EVENT_BUS.register(new SmoothExperiencebar());
    }
}
