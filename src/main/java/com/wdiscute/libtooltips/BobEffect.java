package com.wdiscute.libtooltips;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class BobEffect
{
    public static MutableComponent process(String text, ItemStack stack, Entity entity)
    {
        // amplitude = bob height in pixels, wave = seconds per cycle
        return MotionEffect.build(MotionEffect.Type.BOB, text, stack, entity, 1f, 2.5f);
    }
}
