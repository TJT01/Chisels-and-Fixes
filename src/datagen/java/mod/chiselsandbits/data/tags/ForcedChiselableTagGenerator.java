package mod.chiselsandbits.data.tags;

import mod.chiselsandbits.api.data.tag.AbstractChiselableTagGenerator;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForcedChiselableTagGenerator extends AbstractChiselableTagGenerator
{
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent event)
    {
        event.getGenerator().addProvider(new ForcedChiselableTagGenerator(event.getGenerator(), event.getExistingFileHelper()));
    }

    private ForcedChiselableTagGenerator(final DataGenerator generator, final ExistingFileHelper existingFileHelper)
    {
        super(generator,
          existingFileHelper,
          Mode.FORCED
        );
    }

    @Override
    protected void addElements(final Builder<Block> builder)
    {
        builder.add(Blocks.GRASS_BLOCK)
          .addTag(BlockTags.LEAVES);
    }
}
