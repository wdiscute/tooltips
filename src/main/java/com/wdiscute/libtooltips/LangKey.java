package com.wdiscute.libtooltips;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class LangKey
{
    public static MutableComponent process(String text, ItemStack stack, Entity entity)
    {
        return Tooltips.resolveTagsToComponentFromTranslationKey(text);
    }
}
