package mod.chiselsandbits.client.tool.mode.icon;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.client.chiseling.preview.render.IChiselContextPreviewRenderer;
import mod.chiselsandbits.api.client.chiseling.preview.render.IChiselContextPreviewRendererRegistry;
import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRenderer;
import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRendererRegistry;
import mod.chiselsandbits.api.config.Configuration;
import mod.chiselsandbits.client.chiseling.preview.render.ConfigurableColoredVoxelShapeChiselContextPreviewRenderer;
import mod.chiselsandbits.client.chiseling.preview.render.NoopChiselContextPreviewRenderer;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public class SelectedToolModeRendererRegistry implements ISelectedToolModeIconRendererRegistry
{
    private static final SelectedToolModeRendererRegistry INSTANCE = new SelectedToolModeRendererRegistry();

    public static SelectedToolModeRendererRegistry getInstance()
    {
        return INSTANCE;
    }

    private final Map<ResourceLocation, ISelectedToolModeIconRenderer> rendererMap = Maps.newConcurrentMap();

    private SelectedToolModeRendererRegistry()
    {
       this.register(
          new RootGroupTopLeftSelectedToolModeIconRenderer(),
          new NoopSelectedToolModeIconRenderer()
        );
    }

    @Override
    public ISelectedToolModeIconRenderer getCurrent()
    {
        return rendererMap.getOrDefault(new ResourceLocation(Configuration.getInstance().getClient().toolModeRenderer.get()),
          rendererMap.get(RootGroupTopLeftSelectedToolModeIconRenderer.ID));
    }

    @Override
    public ISelectedToolModeIconRendererRegistry register(final ISelectedToolModeIconRenderer... renderers)
    {
        for (final ISelectedToolModeIconRenderer renderer : renderers)
        {
            if (rendererMap.put(renderer.getId(), renderer) != null)
                throw new IllegalArgumentException("The renderer id: " + renderer.getId() + " is already in use!");
        }
        return this;
    }
}
