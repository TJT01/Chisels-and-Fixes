package mod.chiselsandbits.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.fluids.FluidStack;

import static net.minecraft.util.Direction.*;

public class FluidCuboidUtils
{

    private FluidCuboidUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: FluidCuboidUtils. This is a utility class");
    }

    /**
     * Renders a fluid block with offset from the matrices and from x1/y1/z1 to x2/y2/z2 using block model coordinates, so from 0-16
     */
    public static void renderScaledFluidCuboid(
      FluidStack fluid,
      MatrixStack matrices,
      IVertexBuilder renderer,
      int combinedLight,
      final int combinedOverlay, float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2)
    {
        renderFluidCuboid(fluid, matrices, renderer, combinedLight, combinedOverlay, x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    /**
     * Renders a fluid block with offset from the matrices and from x1/y1/z1 to x2/y2/z2 inside the block local coordinates, so from 0-1
     */
    public static void renderFluidCuboid(
      FluidStack fluid,
      MatrixStack matrices,
      IVertexBuilder renderer,
      int combinedLight,
      final int combinedOverlay,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2)
    {
        int color = fluid.getFluid().getAttributes().getColor(fluid);
        renderFluidCuboid(fluid, matrices, renderer, combinedLight, combinedOverlay, x1, y1, z1, x2, y2, z2, color);
    }

    /**
     * Renders a fluid block with offset from the matrices and from x1/y1/z1 to x2/y2/z2 inside the block local coordinates, so from 0-1
     */
    public static void renderFluidCuboid(
      FluidStack fluid,
      MatrixStack matrices,
      IVertexBuilder renderer,
      int combinedLight,
      final int combinedOverlay,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int color)
    {
        TextureAtlasSprite still =
          Minecraft.getInstance().getTextureAtlas(PlayerContainer.BLOCK_ATLAS).apply(fluid.getFluid().getAttributes().getStillTexture(fluid));
        TextureAtlasSprite flowing =
          Minecraft.getInstance().getTextureAtlas(PlayerContainer.BLOCK_ATLAS).apply(fluid.getFluid().getAttributes().getFlowingTexture(fluid));

        renderFluidCuboid(still, flowing, color, matrices, renderer, combinedOverlay, combinedLight, x1, y1, z1, x2, y2, z2);
    }

    public static void renderFluidCuboid(
      TextureAtlasSprite still,
      TextureAtlasSprite flowing,
      int color,
      MatrixStack matrices,
      IVertexBuilder renderer,
      final int combinedOverlay,
      int combinedLight,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2)
    {
        matrices.pushPose();
        matrices.translate(x1, y1, z1);

        // x/y/z2 - x/y/z1 is because we need the width/height/depth
        putTexturedQuad(renderer, matrices.last(), still, x2 - x1, y2 - y1, z2 - z1, DOWN, color, combinedOverlay, combinedLight, false);
        putTexturedQuad(renderer, matrices.last(), flowing, x2 - x1, y2 - y1, z2 - z1, NORTH, color, combinedOverlay, combinedLight, true);
        putTexturedQuad(renderer, matrices.last(), flowing, x2 - x1, y2 - y1, z2 - z1, Direction.EAST, color, combinedOverlay, combinedLight, true);
        putTexturedQuad(renderer, matrices.last(), flowing, x2 - x1, y2 - y1, z2 - z1, Direction.SOUTH, color, combinedOverlay, combinedLight, true);
        putTexturedQuad(renderer, matrices.last(), flowing, x2 - x1, y2 - y1, z2 - z1, Direction.WEST, color, combinedOverlay, combinedLight, true);
        putTexturedQuad(renderer, matrices.last(), still, x2 - x1, y2 - y1, z2 - z1, UP, color, combinedOverlay, combinedLight, false);
        matrices.popPose();
    }

    public static void putTexturedQuad(
      IVertexBuilder renderer, MatrixStack.Entry matrix, TextureAtlasSprite sprite, float w, float h, float d, Direction face,
      int color, final int overlay, int brightness, boolean flowing)
    {
        putTexturedQuad(renderer, matrix, sprite, w, h, d, face, color, overlay, brightness, flowing, false, false);
    }

    public static void putTexturedQuad(
      IVertexBuilder renderer, MatrixStack.Entry matrix, TextureAtlasSprite sprite, float w, float h, float d, Direction face,
      int color, final int overlay, int brightness, boolean flowing, boolean flipHorizontally, boolean flipVertically)
    {
        int l1 = brightness >> 0x10 & 0xFFFF;
        int l2 = brightness & 0xFFFF;

        int o1 = overlay >> 0x10 & 0xFFFF;
        int o2 = overlay & 0xFFFF;

        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;

        putTexturedQuad(renderer, matrix, sprite, w, h, d, face, r, g, b, a, l1, l2, o1, o2, flowing, flipHorizontally, flipVertically);
    }

    /* Fluid cuboids */
// x and x+w has to be within [0,1], same for y/h and z/d
    public static void putTexturedQuad(
      IVertexBuilder renderer, MatrixStack.Entry matrices, TextureAtlasSprite sprite, float w, float h, float d, Direction face,
      int r, int g, int b, int a, int light1, int light2, final int overlay1, final int overlay2, boolean flowing, boolean flipHorizontally, boolean flipVertically)
    {
        // safety
        if (sprite == null)
        {
            return;
        }
        float minU;
        float maxU;
        float minV;
        float maxV;

        double size = 16f;
        if (flowing)
        {
            size = 8f;
        }

        double xt1 = 0;
        double xt2 = w;
        while (xt2 > 1f)
        {
            xt2 -= 1f;
        }
        double yt1 = 0;
        double yt2 = h;
        while (yt2 > 1f)
        {
            yt2 -= 1f;
        }
        double zt1 = 0;
        double zt2 = d;
        while (zt2 > 1f)
        {
            zt2 -= 1f;
        }

        // flowing stuff should start from the bottom, not from the start
        if (flowing)
        {
            double tmp = 1d - yt1;
            yt1 = 1d - yt2;
            yt2 = tmp;
        }

        switch (face)
        {
            case DOWN:
            case UP:
                minU = sprite.getU(xt1 * size);
                maxU = sprite.getU(xt2 * size);
                minV = sprite.getV(zt1 * size);
                maxV = sprite.getV(zt2 * size);
                break;
            case NORTH:
            case SOUTH:
                minU = sprite.getU(xt2 * size);
                maxU = sprite.getU(xt1 * size);
                minV = sprite.getV(yt1 * size);
                maxV = sprite.getV(yt2 * size);
                break;
            case WEST:
            case EAST:
                minU = sprite.getU(zt2 * size);
                maxU = sprite.getU(zt1 * size);
                minV = sprite.getV(yt1 * size);
                maxV = sprite.getV(yt2 * size);
                break;
            default:
                minU = sprite.getU0();
                maxU = sprite.getU1();
                minV = sprite.getV0();
                maxV = sprite.getV1();
        }

        if (flipHorizontally)
        {
            float tmp = minV;
            minV = maxV;
            maxV = tmp;
        }

        if (flipVertically)
        {
            float tmp = minU;
            minU = maxU;
            maxU = tmp;
        }

        final Matrix4f worldMatrix = matrices.pose();
        final Matrix3f normalMatrix = matrices.normal();

        switch (face)
        {
            case DOWN:
                renderer.vertex(worldMatrix, 0, 0, 0)
                  .color(r, g, b, a)
                  .uv(minU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, 0, 0)
                  .color(r, g, b, a)
                  .uv(maxU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, 0, d)
                  .color(r, g, b, a)
                  .uv(maxU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, 0, d)
                  .color(r, g, b, a)
                  .uv(minU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ())
                  .endVertex();
                break;
            case UP:
                renderer.vertex(worldMatrix, 0, h, 0)
                  .color(r, g, b, a)
                  .uv(minU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, UP.getStepX(), UP.getStepY(), UP.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, h, d)
                  .color(r, g, b, a)
                  .uv(minU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, UP.getStepX(), UP.getStepY(), UP.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, h, d)
                  .color(r, g, b, a)
                  .uv(maxU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, UP.getStepX(), UP.getStepY(), UP.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, h, 0)
                  .color(r, g, b, a)
                  .uv(maxU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, UP.getStepX(), UP.getStepY(), UP.getStepZ())
                  .endVertex();
                break;
            case NORTH:
                renderer.vertex(worldMatrix, 0, 0, 0)
                  .color(r, g, b, a)
                  .uv(minU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, h, 0)
                  .color(r, g, b, a)
                  .uv(minU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, h, 0)
                  .color(r, g, b, a)
                  .uv(maxU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, 0, 0)
                  .color(r, g, b, a)
                  .uv(maxU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ())
                  .endVertex();
                break;
            case SOUTH:
                renderer.vertex(worldMatrix, 0, 0, d)
                  .color(r, g, b, a)
                  .uv(maxU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, 0, d)
                  .color(r, g, b, a)
                  .uv(minU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, h, d)
                  .color(r, g, b, a)
                  .uv(minU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, h, d)
                  .color(r, g, b, a)
                  .uv(maxU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ())
                  .endVertex();
                break;
            case WEST:
                renderer.vertex(worldMatrix, 0, 0, 0)
                  .color(r, g, b, a)
                  .uv(maxU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, 0, d)
                  .color(r, g, b, a)
                  .uv(minU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, h, d)
                  .color(r, g, b, a)
                  .uv(minU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, 0, h, 0)
                  .color(r, g, b, a)
                  .uv(maxU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ())
                  .endVertex();
                break;
            case EAST:
                renderer.vertex(worldMatrix, w, 0, 0)
                  .color(r, g, b, a)
                  .uv(minU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, h, 0)
                  .color(r, g, b, a)
                  .uv(minU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, h, d)
                  .color(r, g, b, a)
                  .uv(maxU, minV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ())
                  .endVertex();
                renderer.vertex(worldMatrix, w, 0, d)
                  .color(r, g, b, a)
                  .uv(maxU, maxV)
                  .overlayCoords(overlay1, overlay2)
                  .uv2(light1, light2)
                  .normal(normalMatrix, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ())
                  .endVertex();
                break;
        }
    }
}
