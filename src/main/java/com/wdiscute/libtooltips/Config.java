package com.wdiscute.libtooltips;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SPEED = BUILDER
            .comment("Color Cycling Speed")
            .defineInRange("speed", 5, 0, Integer.MAX_VALUE);


    static final ModConfigSpec SPEC = BUILDER.build();
}
