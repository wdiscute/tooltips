package com.wdiscute.libtooltips.mixin;

import com.wdiscute.libtooltips.Tooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class GetNameMixin
{

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    public void getHoverNameMixin(CallbackInfoReturnable<Component> cir)
    {
        ItemStack stack = (ItemStack) (Object) this;

        String input = cir.getReturnValue().getString();

        if(Tooltips.hasTags(input))
        {
            MutableComponent mutableComponent = Tooltips.resolveTagsToComponent(input, stack, Minecraft.getInstance().player);

            cir.setReturnValue(mutableComponent);
        }
    }
}
