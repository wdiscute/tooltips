package com.wdiscute.libtooltips;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
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

    public Tooltips(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.addListener(Tooltips::modifyItemTooltip);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    @Mod(value = MOD_ID, dist = Dist.CLIENT)
    public static class Client
    {
        public static final KeyMapping EXPAND = new KeyMapping("key.libtooltips.expand", GLFW.GLFW_KEY_LEFT_SHIFT, "key.category.libtooltips.libtooltips");

        public Client(ModContainer modContainer, IEventBus bus)
        {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

            bus.addListener(Client::registerKeybinds);

            registerProcessor("ltkeybind", KeybindProcessor::process);
            registerProcessor("ltrgb", RGBEffect::process);
            registerProcessor("ltcolor", ColorEffect::process);
        }

        public static void registerKeybinds(RegisterKeyMappingsEvent event)
        {
            event.register(Client.EXPAND);
        }
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
                if (I18n.get(baseTooltipNoShift + "." + i).equals("hide"))
                    break;
                tooltipComponents.add(Component.literal(spaces.toString()).append(resolveTagsToComponentFromTranslationKey(baseTooltipNoShift + "." + i, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.getAsInt()))));
            }
        }

        if (I18n.exists(baseTooltip + ".0"))
        {
            boolean shift = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), Client.EXPAND.getKey().getValue());
            if (shift)
            {
                tooltipComponents.add(resolveTagsToComponentFromTranslationKey("tooltip.libtooltips.generic.shift_down"));
                if (Config.LINE_BEFORE.getAsBoolean())
                    tooltipComponents.add(Component.translatable("tooltip.libtooltips.generic.empty"));

                for (int i = 0; i < 100; i++)
                {
                    if (!I18n.exists(baseTooltip + "." + i))
                        break;
                    if (I18n.get(baseTooltip + "." + i).equals("hide"))
                        break;
                    tooltipComponents.add(Component.literal(spaces.toString()).append(
                            resolveTagsToComponentFromTranslationKey(baseTooltip + "." + i, stack, event.getEntity()).withStyle(Style.EMPTY.withColor(Config.DEFAULT_COLOR.getAsInt()))));
                }

                if (Config.LINE_AFTER.getAsBoolean())
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
