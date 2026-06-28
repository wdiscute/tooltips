package com.wdiscute.libtooltips;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public class KeybindProcessor
{
    public static MutableComponent process(String text, ItemStack stack, Entity entity)
    {
        String[] parts = text.split(";");

        if (parts.length < 2)
            return Component.translatable(text);

        String keybind = "";
        String textToAdd = "";

        for (String part : parts)
            if (part.startsWith("keybind="))
                keybind = part.substring(8);
            else
                textToAdd = part;

        String finalKeybind = keybind;
        List<KeyMapping> list = Arrays.stream(Minecraft.getInstance().options.keyMappings)
                .filter(o -> finalKeybind.equals(o.getName())).toList();

        if (list.isEmpty())
            return Component.literal(textToAdd).append(Component.translatable(keybind).getString());

        String maybeLeftShift = list.get(0).getKey().getDisplayName().getString();
        if(maybeLeftShift.equals("Left Shift")) maybeLeftShift = "Shift";
        return Component.literal(textToAdd + maybeLeftShift);
    }
}
