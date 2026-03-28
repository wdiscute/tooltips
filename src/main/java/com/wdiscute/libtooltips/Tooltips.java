package com.wdiscute.libtooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.tysontheember.emberstextapi.EmbersTextAPI;
import net.tysontheember.emberstextapi.immersivemessages.api.ImmersiveMessage;
import net.tysontheember.emberstextapi.immersivemessages.api.MarkupParser;
import net.tysontheember.emberstextapi.immersivemessages.api.TextSpan;

import java.util.List;

@Mod(Tooltips.MOD_ID)
public class Tooltips
{
    public static final String MOD_ID = "libtooltips";


    public Tooltips(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.addListener(Tooltips::modifyItemTooltip);
    }

    public static void modifyItemTooltip(ItemTooltipEvent event)
    {
        List<Component> tooltipComponents = event.getToolTip();
        ItemStack stack = event.getItemStack();

        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String namespace = rl.getNamespace();
        String path = rl.getPath();
        String baseTooltip = "tooltip." + namespace + "." + path;
        String baseTooltipNoShift = "tooltip.always." + namespace + "." + path;

        if (I18n.exists(baseTooltipNoShift + ".0"))
        {
            for (int i = 0; i < 100; i++)
            {
                if (!I18n.exists(baseTooltipNoShift + "." + i))
                    break;
                tooltipComponents.add(Component.translatable(baseTooltipNoShift + "." + i));
            }
        }

        if (I18n.exists(baseTooltip + ".0"))
        {
            if (event.getFlags().hasShiftDown())
            {
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.shift_down"));
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));

                for (int i = 0; i < 100; i++)
                {
                    if (!I18n.exists(baseTooltip + "." + i))
                        break;
                    tooltipComponents.add(Component.translatable(baseTooltip + "." + i));
                }
            } else
            {
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.shift_up"));
            }
        }
    }
}
