package com.wdiscute.libtooltips.mixin;

import com.wdiscute.libtooltips.MotionEffect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientTextTooltip.class)
public abstract class ClientTextTooltipMixin
{
    @Shadow
    @Final
    private FormattedCharSequence text;

    // fall back to vanilla drawInBatch when no motion effects are present
    // translation key can only carry one motion effect at a time (similar to existing api)
    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true)
    private void libtooltips$renderMotion(Font font, int x, int y, Matrix4f pose, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci)
    {
        if (!MotionEffect.hasMotion(this.text))
            return;

        ci.cancel();

        double time = System.currentTimeMillis() / 1000.0;
        float[] cursorX = {x};
        Matrix4f charPose = new Matrix4f();

        this.text.accept((index, style, codePoint) ->
        {
            MotionEffect.Entry entry = MotionEffect.parse(style.getInsertion());

            FormattedCharSequence single = FormattedCharSequence.codepoint(codePoint, style);
            // advance by true width, avoids the extra spacing that occurs from drawInBatch and results in failure to wrap
            float advance = font.width(single);

            charPose.set(pose);

            if (entry != null)
            {
                double diff = entry.amplitude() * Math.sin(2 * Math.PI * ((time + entry.offset()) / entry.wave()));
                if (entry.type() == MotionEffect.Type.ROTATE)
                {
                    float pivotX = cursorX[0] + advance / 2f;

                    charPose.translate(pivotX, y, 0);
                    charPose.rotateZ((float) Math.toRadians(diff));
                    charPose.translate(-pivotX, -y, 0);
                }
                else
                {
                    charPose.translate(0, (float) diff, 0);
                }
            }

            font.drawInBatch(single, cursorX[0], y, -1, true, charPose, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
            cursorX[0] += advance;

            return true;
        });
    }
}
