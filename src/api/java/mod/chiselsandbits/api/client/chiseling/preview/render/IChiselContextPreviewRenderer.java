package mod.chiselsandbits.api.client.chiseling.preview.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Renderer which is used to render the chiseling preview.
 */
@OnlyIn(Dist.CLIENT)
public interface IChiselContextPreviewRenderer
{

    /**
     * The id of teh renderer.
     * Used to give the player a selection option for the preview renderer.
     *
     * @return The id of the preview renderer.
     */
    ResourceLocation getId();

    /**
     * Invoked by the engine to render previews of the given {@link IChiselingContext}.
     *
     * @param matrixStack The matrix stack to render into.
     * @param currentContextSnapshot The current snapshot to render.
     */
    void renderExistingContextsBoundingBox(final MatrixStack matrixStack, final IChiselingContext currentContextSnapshot);
}
