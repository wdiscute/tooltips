package com.wdiscute.libtooltips;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MotionEffect
{
    private static final String MARKER = "LTFX:";

    public enum Type
    {
        BOB, ROTATE
    }

    public record Entry(Type type, float amplitude, float wave, double offset) {}

    // motion components shared logic
    // amplitude means bob height in pixels or rotate in degrees.
    public static MutableComponent build(Type type, String text, ItemStack stack, Entity entity, float defaultAmplitude, float defaultWave)
    {
        float amplitude = defaultAmplitude;
        float wave = defaultWave;
        float phase = 1f;
        boolean uniform = false;

        String remaining = text;
        while (true)
        {
            int semi = remaining.indexOf(';');
            if (semi < 0)
                break;

            String option = remaining.substring(0, semi);
            boolean matched = true;
            try
            {
                if (option.equals("uniform")) uniform = true;
                else if (option.startsWith("amplitude=")) amplitude = Float.parseFloat(option.substring(10));
                else if (option.startsWith("angle=")) amplitude = Float.parseFloat(option.substring(6));
                else if (option.startsWith("a=")) amplitude = Float.parseFloat(option.substring(2));
                else if (option.startsWith("wave=")) wave = Float.parseFloat(option.substring(5));
                else if (option.startsWith("w=")) wave = Float.parseFloat(option.substring(2));
                else if (option.startsWith("phase=")) phase = Float.parseFloat(option.substring(6));
                else if (option.startsWith("p=")) phase = Float.parseFloat(option.substring(2));
                else matched = false;
            } catch (NumberFormatException e) { matched = false; }

            if (!matched)
                break;

            remaining = remaining.substring(semi + 1);
        }

        String content = remaining;

        MutableComponent resolved = Tooltips.resolveTagsToComponent(content, stack, entity);

        List<MutableComponent> chars = flattenToChars(resolved);

        MutableComponent result = Component.empty();
        int length = chars.size();
        for (int i = 0; i < length; i++)
        {
            double offset = length <= 1 ? 0 : phase * i / (length - 1);

            MutableComponent c = chars.get(i);
            String marker = buildMarker(type, amplitude, wave, uniform ? 0 : offset);

            result.append(c.setStyle(c.getStyle().withInsertion(marker)));
        }

        return result;
    }

    public static String buildMarker(Type type, float amplitude, float wave, double offset)
    {
        return MARKER + type.name().toLowerCase() + ":a=" + amplitude + ",w=" + wave + ",o=" + offset;
    }

    public static boolean hasMotion(FormattedCharSequence sequence)
    {
        boolean[] found = {false};

        sequence.accept((index, style, codePoint) ->
        {
            String insertion = style.getInsertion();
            if (insertion != null && insertion.startsWith(MARKER))
            {
                found[0] = true;
                return false;
            }
            return true;
        });

        return found[0];
    }

    public static Entry parse(String insertion)
    {
        if (insertion == null || !insertion.startsWith(MARKER))
            return null;

        String[] kv = insertion.substring(MARKER.length()).split(":", 2);
        if (kv.length < 2)
            return null;

        Type type = kv[0].equalsIgnoreCase("rotate") ? Type.ROTATE : Type.BOB;
        float amplitude = 0f;
        float wave = 1f;
        double offset = 0;

        for (String option : kv[1].split(","))
        {
            try
            {
                if (option.startsWith("a="))
                    amplitude = Float.parseFloat(option.substring(2));
                else if (option.startsWith("w="))
                    wave = Float.parseFloat(option.substring(2));
                else if (option.startsWith("o="))
                    offset = Double.parseDouble(option.substring(2));
            } catch (NumberFormatException ignored) {}
        }

        return new Entry(type, amplitude, wave, offset);
    }

    // flattens the possibly nested translation key ie a color/rgb inside motion tag
    public static List<MutableComponent> flattenToChars(Component component)
    {
        List<MutableComponent> out = new ArrayList<>();
        flatten(component, out);
        return out;
    }

    private static void flatten(Component component, List<MutableComponent> out)
    {
        if (component.getContents() instanceof PlainTextContents plain)
        {
            String text = plain.text();
            for (int i = 0; i < text.length(); i++)
                out.add(Component.literal(String.valueOf(text.charAt(i))).withStyle(component.getStyle()));
        }

        for (Component sibling : component.getSiblings())
            flatten(sibling, out);
    }
}
