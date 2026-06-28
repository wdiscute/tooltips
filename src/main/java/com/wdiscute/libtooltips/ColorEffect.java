package com.wdiscute.libtooltips;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ColorEffect
{
    public static MutableComponent process(String text, ItemStack stack, Entity entity)
    {
        String[] parts = text.split(";");

        if (parts.length < 2)
            return Component.translatable(text);

        List<Integer> colors = new ArrayList<>();

        float phase = 1f;
        float waveDuration = 10f;
        boolean uniform = false;

        for (int i = 0; i < parts.length - 1; i++)
        {
            try
            {
                if (parts[i].startsWith("uniform"))
                {
                    uniform = true;
                    continue;
                }

                if (parts[i].startsWith("wave="))
                {
                    waveDuration = Float.parseFloat(parts[i].substring(5));
                    continue;
                }

                if (parts[i].startsWith("w="))
                {
                    waveDuration = Float.parseFloat(parts[i].substring(2));
                    continue;
                }

                if (parts[i].startsWith("phase="))
                {
                    phase = Float.parseFloat(parts[i].substring(6));
                    continue;
                }

                if (parts[i].startsWith("p="))
                {
                    phase = Float.parseFloat(parts[i].substring(2));
                    continue;
                }

                if (parts[i].startsWith("color="))
                {
                    colors.add((int) Long.parseLong(parts[i].substring(6), 16));
                    continue;
                }

                if (parts[i].startsWith("c="))
                {
                    colors.add((int) Long.parseLong(parts[i].substring(2), 16));
                    continue;
                }
            } catch (NumberFormatException ignored)
            {
            }
        }

        if (colors.isEmpty())
            return Component.translatable(text);

        String translated = Component.translatable(parts[parts.length - 1]).getString();

        MutableComponent result = Component.empty();

        int length = translated.length();

        for (int i = 0; i < length; i++)
        {
            double offset = length == 1 ? 0 : phase * i / (length - 1);

            result.append(
                    Component.literal(String.valueOf(translated.charAt(i)))
                            .withStyle(Style.EMPTY.withColor(getColor(colors, uniform ? 0 : offset, waveDuration)))
            );
        }

        return result;
    }

    private static int getColor(List<Integer> colors, double offset, float duration)
    {
        if (colors.size() == 1)
            return colors.get(0);

        double time = System.currentTimeMillis() / 1000.0;

        double progress = ((time + offset) / duration) * colors.size();

        int index = (int) Math.floor(progress) % colors.size();
        int nextIndex = (index + 1) % colors.size();

        float t = (float) (progress - Math.floor(progress));

        return lerpColor(colors.get(index), colors.get(nextIndex), t);
    }

    // ChatGPT code. I am NOT doing bit shifting out of my own will.
    private static int lerpColor(int c1, int c2, float t)
    {
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
