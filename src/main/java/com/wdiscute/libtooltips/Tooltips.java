package com.wdiscute.libtooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Tooltips.MOD_ID)
public class Tooltips
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "libtooltips";

    public static float hue;

    @OnlyIn(Dist.DEDICATED_SERVER)
    public Tooltips()
    {

    }


    @OnlyIn(Dist.CLIENT)
    public Tooltips(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.addListener(Tooltips::modifyItemTooltip);
        NeoForge.EVENT_BUS.addListener(Tooltips::renderFrame);
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderFrame(RenderFrameEvent.Post event)
    {
        Tooltips.hue += 0.001f;
    }


    public static void modifyItemTooltip(ItemTooltipEvent event)
    {
        List<Component> tooltipComponents = event.getToolTip();
        ItemStack stack = event.getItemStack();

        if (I18n.exists("tooltip." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace() + "." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + ".0"))
        {
            if (event.getFlags().hasShiftDown())
            {
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.shift_down"));
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));

                for (int i = 0; i < 100; i++)
                {
                    if (!I18n.exists("tooltip." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace() + "." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "." + i))
                        break;

                    String s = I18n.get("tooltip." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace() + "." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "." + i);

                    tooltipComponents.add(DecodeTranslationKeyTags(s));

                }

            }
            else
            {
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.shift_up"));
            }

        }
    }

    public static Component DecodeTranslationKeyTags(String translationKey)
    {
        String s = I18n.get(translationKey);

        if (s.contains("<rgb>"))
        {
            return Component.empty()
                    .append(s.substring(0, s.indexOf("<rgb>")))
                    .append(RGBEachLetter(Tooltips.hue, s.substring(s.indexOf("<rgb>") + 5, s.indexOf("</rgb>")), 0.01f))
                    .append(s.substring(s.indexOf("</rgb>") + 6));
        }
        else if (s.contains("<gradient"))
        {
            float min = Float.parseFloat("0." + s.substring(s.indexOf("<gradient") + 10, s.indexOf("<gradient") + 12));
            float max = Float.parseFloat("0." + s.substring(s.indexOf("</gradient") + 11, s.indexOf("</gradient") + 13));

            return Component.empty()
                    .append(s.substring(0, s.indexOf("<gradient")))
                    .append(Gradient(Tooltips.hue, s.substring(s.indexOf("<gradient") + 13, s.indexOf("</gradient")), min, max))
                    .append(s.substring(s.indexOf("</gradient") + 14));
        }
        else
        {
            return Component.translatable(translationKey);
        }

    }


    public static Component Gradient(float hue, String text, float min, float max)
    {
        MutableComponent c = Component.empty();

        for (int i = 0; i < text.length(); i++)
        {
            String s = text.substring(i, i + 1);


            float pingPongedHue = mapHuePingPong(i * 0.01f + hue, min, max);

            int color = HueToRGBInt(pingPongedHue);

            Component l = Component.literal(s).withColor(color);

            c.append(l);
        }

        return c;
    }

    public static float mapHuePingPong(float h, float min, float max)
    {
        float t = Math.abs(2f * (h % 1) - 1f);
        return min + t * (max - min);
    }


    public static Component RGBEachLetter(float hue, String text, float speed)
    {

        MutableComponent c = Component.empty();

        hue += 0.001f;

        for (int i = 0; i < text.length(); i++)
        {
            String s = text.substring(i, i + 1);

            int color = HueToRGBInt(i * speed + hue);

            Component l = Component.literal(s).withColor(color);

            c.append(l);
        }

        return c;
    }


    public static int HueToRGBInt(float hue)
    {
        int r = 0, g = 0, b = 0;

        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float f = h - (float) Math.floor(h);
        float q = 1.0f - f;
        float t = 1.0f - (1.0f - f);
        switch ((int) h)
        {
            case 0:
                r = (int) (255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (255.0f + 0.5f);
                break;
            case 2:
                g = (int) (255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = 0;
                b = (int) (255.0f + 0.5f);
                break;
            case 5:
                r = (int) (255.0f + 0.5f);
                g = 0;
                b = (int) (q * 255.0f + 0.5f);
                break;
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b);
    }



}
