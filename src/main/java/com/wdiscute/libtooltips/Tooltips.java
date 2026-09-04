package com.wdiscute.libtooltips;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.function.TriFunction;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(Tooltips.MOD_ID)
public class Tooltips
{
    public static final String MOD_ID = "libtooltips";

    public Tooltips()
    {
        ModLoadingContext modContainer = ModLoadingContext.get();

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        DistExecutor.safeRunWhenOn(Dist.CLIENT,
                () -> Client::init);
    }

    public static class Client
    {
        public static void init()
        {
            registerProcessor("ltkeybind", KeybindProcessor::process);
            registerProcessor("ltrgb", RGBEffect::process);
            registerProcessor("ltcolor", ColorEffect::process);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event)
        {
            modifyItemTooltip(event);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        public static final KeyMapping EXPAND = new KeyMapping("key.libtooltips.expand", GLFW.GLFW_KEY_LEFT_SHIFT, "key.category.libtooltips.libtooltips");

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
        {
            event.register(EXPAND);
        }
    }

    /**
     * Implement this interface if you wish your item to have tooltips without requiring translation keys or data components
     */
    public interface ItemTooltip
    {
        List<String> getAlwaysTooltips(ItemStack stack);

        List<String> getShiftTooltips(ItemStack stack);
    }

    private static final Pattern TAG_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>(.*?)</\\1>", Pattern.DOTALL);

    private static final Map<String, TriFunction<String, ItemStack, Entity, MutableComponent>> PROCESSORS = new HashMap<>();

    public static void registerProcessor(String tag, TriFunction<String, ItemStack, Entity, MutableComponent> processor)
    {
        PROCESSORS.put(tag.toLowerCase(), processor);
    }

    public static boolean hasTags(String input)
    {
        Matcher matcher = TAG_PATTERN.matcher(input);

        while (matcher.find())
        {
            String tag = matcher.group(1).toLowerCase();
            var processor = PROCESSORS.get(tag);

            if (processor != null)
                return true;
        }

        return false;
    }

    public static MutableComponent resolveTagsToComponentFromTranslationKey(String translationKey, @Nullable ItemStack stack, @Nullable Entity entity)
    {
        return resolveTagsToComponent(I18n.get(translationKey), stack, entity);
    }

    public static MutableComponent resolveTagsToComponentFromTranslationKey(String translationKey)
    {
        return resolveTagsToComponent(I18n.get(translationKey), null, null);
    }

    public static MutableComponent resolveTagsToComponent(String input)
    {
        return resolveTagsToComponent(input, null, null);
    }

    public static MutableComponent resolveTagsToComponent(String input, @Nullable ItemStack stack, @Nullable Entity entity)
    {
        MutableComponent result = Component.empty();

        Matcher matcher = TAG_PATTERN.matcher(input);

        int lastEnd = 0;

        while (matcher.find())
        {
            if (matcher.start() > lastEnd)
                result.append(Component.literal(input.substring(lastEnd, matcher.start())));

            String tag = matcher.group(1).toLowerCase();
            String content = matcher.group(2);

            var processor = PROCESSORS.get(tag);

            if (processor != null)
                result.append(processor.apply(content, stack, entity));
            else
                result.append(Component.literal(matcher.group()));

            lastEnd = matcher.end();
        }

        if (lastEnd < input.length())
            result.append(Component.literal(input.substring(lastEnd)));

        return result;
    }

    public static void modifyItemTooltip(ItemTooltipEvent event)
    {
        if (!Config.ENABLE_TOOLTIPS.get()) return;

        List<Component> tooltipComponents = event.getToolTip();
        ItemStack stack = event.getItemStack();

        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String namespace = rl.getNamespace();
        String path = rl.getPath();
        String baseTooltip = "tooltip." + namespace + "." + path;
        String baseTooltipNoShift = "tooltip.always." + namespace + "." + path;
        StringBuilder spaces = new StringBuilder().append(" ".repeat(Math.max(0, Config.SPACES_BEFORE_TOOLTIP.get())));

        List<String> interfaceShift;
        List<String> interfaceAlways;
        if (stack.getItem() instanceof ItemTooltip it)
        {
            interfaceShift = it.getShiftTooltips(stack);
            interfaceAlways = it.getAlwaysTooltips(stack);
        }
        else
        {
            interfaceShift = List.of();
            interfaceAlways = List.of();
        }

        //always tooltips
        if (I18n.exists(baseTooltipNoShift + ".0") || !interfaceAlways.isEmpty())
        {
            //add tooltips from data component
            //for (String string : compAlways)
            //    tooltipComponents.add(Component.literal(spaces.toString())
            //            .append(resolveTagsToComponentFromTranslationKey(string, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.get()))));

            //add tooltips from interface
            if (stack.getItem() instanceof ItemTooltip it)
                for (String string : it.getAlwaysTooltips(stack))
                    tooltipComponents.add(Component.literal(spaces.toString()).append(resolveTagsToComponentFromTranslationKey(string, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.get()))));

            //add tooltips from translation keys
            for (int i = 0; i < 100; i++)
            {
                //break if tooltip doesn't exist
                if (!I18n.exists(baseTooltipNoShift + "." + i))
                    break;

                //break if tooltip is "hide"
                if (I18n.get(baseTooltipNoShift + "." + i).equals("hide"))
                    break;

                tooltipComponents.add(Component.literal(spaces.toString()).append(resolveTagsToComponentFromTranslationKey(baseTooltipNoShift + "." + i, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.get()))));
            }
        }

        //shift tooltips
        if (I18n.exists(baseTooltip + ".0") || !interfaceShift.isEmpty())
        {
            boolean shift = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), ClientModEvents.EXPAND.getKey().getValue());
            if (shift)
            {
                tooltipComponents.add(resolveTagsToComponentFromTranslationKey("tooltip.libtooltips.generic.shift_down"));
                if (Config.LINE_BEFORE.get())
                    tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));

                //add tooltips from data component
                //for (String string : compShift)
                //    tooltipComponents.add(Component.literal(spaces.toString())
                //            .append(resolveTagsToComponentFromTranslationKey(string, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.get()))));

                //add tooltips from interface
                if (stack.getItem() instanceof ItemTooltip it)
                    for (String string : it.getShiftTooltips(stack))
                        tooltipComponents.add(Component.literal(spaces.toString()).append(resolveTagsToComponentFromTranslationKey(string, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.get()))));

                //add tooltips from translation keys
                for (int i = 0; i < 100; i++)
                {
                    //break if tooltip doesn't exist
                    if (!I18n.exists(baseTooltip + "." + i))
                        break;

                    //break if tooltip is "hide"
                    if (I18n.get(baseTooltip + "." + i).equals("hide"))
                        break;

                    tooltipComponents.add(Component.literal(spaces.toString()).append(
                            resolveTagsToComponentFromTranslationKey(baseTooltip + "." + i, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.get()))));
                }

                if (Config.LINE_AFTER.get())
                    tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));
            }
            else
            {
                tooltipComponents.add(resolveTagsToComponentFromTranslationKey("tooltip.libtooltips.generic.shift_up"));
            }
        }
    }

    public static class Config
    {
        private static final ForgeConfigSpec.Builder BUILDER_CLIENT = new ForgeConfigSpec.Builder();

        public static final ForgeConfigSpec.BooleanValue ENABLE_TOOLTIPS = BUILDER_CLIENT
                .translation("libtooltips.configuration.disable_tooltips")
                .define("disable_tooltips", true);


        public static final ForgeConfigSpec.BooleanValue LINE_BEFORE = BUILDER_CLIENT
                .translation("libtooltips.configuration.line_before")
                .define("line_before", false);

        public static final ForgeConfigSpec.BooleanValue LINE_AFTER = BUILDER_CLIENT
                .translation("libtooltips.configuration.line_after")
                .define("line_after", false);

        public static final ForgeConfigSpec.IntValue DEFAULT_COLOR = BUILDER_CLIENT
                .translation("libtooltips.configuration.default_color")
                .defineInRange("default_color", 0x777777, Integer.MIN_VALUE, Integer.MAX_VALUE);

        public static final ForgeConfigSpec.IntValue SPACES_BEFORE_TOOLTIP = BUILDER_CLIENT
                .translation("libtooltips.configuration.spaces_before_tooltip")
                .defineInRange("spaces_before_tooltip", 2, 0, 10);


        static final ForgeConfigSpec SPEC = BUILDER_CLIENT.build();

    }
}
