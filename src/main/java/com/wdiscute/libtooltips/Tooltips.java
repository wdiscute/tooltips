package com.wdiscute.libtooltips;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

@Mod(Tooltips.MOD_ID)
public class Tooltips
{
    public static final String MOD_ID = "libtooltips";


    public Tooltips(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.addListener(Tooltips::modifyItemTooltip);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    @Mod(value = MOD_ID, dist = Dist.CLIENT)
    public static class Client
    {
        public Client(ModContainer modContainer)
        {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    public static void modifyItemTooltip(ItemTooltipEvent event)
    {
        List<Component> tooltipComponents = event.getToolTip();
        ItemStack stack = event.getItemStack();

        Identifier rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String namespace = rl.getNamespace();
        String path = rl.getPath();
        String baseTooltip = "tooltip." + namespace + "." + path;
        String baseTooltipNoShift = "tooltip.always." + namespace + "." + path;
        StringBuilder spaces = new StringBuilder().repeat(" ", Config.SPACES_BEFORE_TOOLTIP.get());

        if (I18n.exists(baseTooltipNoShift + ".0"))
        {
            for (int i = 0; i < 100; i++)
            {
                if (!I18n.exists(baseTooltipNoShift + "." + i))
                    break;
                tooltipComponents.add(Component.literal(spaces.toString()).append(Component.translatable(baseTooltipNoShift + "." + i).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.getAsInt()))));
            }
        }

        if (I18n.exists(baseTooltip + ".0"))
        {
            if (event.getFlags().hasShiftDown())
            {
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.shift_down"));
                if (Config.LINE_BEFORE.getAsBoolean())
                    tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));

                for (int i = 0; i < 100; i++)
                {
                    if (!I18n.exists(baseTooltip + "." + i))
                        break;
                    tooltipComponents.add(Component.literal(spaces.toString()).append(Component.translatable(baseTooltip + "." + i).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.getAsInt()))));
                }

                if (Config.LINE_AFTER.getAsBoolean())
                    tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));

            }
            else
            {
                tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.shift_up"));
            }
        }
    }

    public static class Config
    {
        private static final ModConfigSpec.Builder BUILDER_CLIENT = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue LINE_BEFORE = BUILDER_CLIENT
                .translation("libtooltips.configuration.line_before")
                .define("line_before", false);

        public static final ModConfigSpec.BooleanValue LINE_AFTER = BUILDER_CLIENT
                .translation("libtooltips.configuration.line_after")
                .define("line_after", false);

        public static final ModConfigSpec.IntValue DEFAULT_COLOR = BUILDER_CLIENT
                .translation("libtooltips.configuration.default_color")
                .defineInRange("default_color", 0x777777, Integer.MIN_VALUE, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue SPACES_BEFORE_TOOLTIP = BUILDER_CLIENT
                .translation("libtooltips.configuration.spaces_before_tooltip")
                .defineInRange("spaces_before_tooltip", 2, 0, 10);


        static final ModConfigSpec SPEC = BUILDER_CLIENT.build();

    }
}
