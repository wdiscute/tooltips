package com.wdiscute.libtooltips;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class RotateEffect
{
    public static MutableComponent process(String text, ItemStack stack, Entity entity)
    {
        // amplitude = swing in degrees, wave = seconds per cycle
        return MotionEffect.build(MotionEffect.Type.ROTATE, text, stack, entity, 15f, 4f);
    }
}
