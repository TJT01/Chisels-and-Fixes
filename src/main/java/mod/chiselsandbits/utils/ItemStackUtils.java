package mod.chiselsandbits.utils;

import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.item.withhighlight.IWithHighlightItem;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.api.util.SingleBlockBlockReader;
import mod.chiselsandbits.item.ChiseledBlockItem;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemStackUtils
{

    private ItemStackUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ItemStackUtils. This is a utility class");
    }

    /**
     * Mimics pick block.
     *
     * @param blockState the block and state we are creating an ItemStack for.
     * @return ItemStack fromt the BlockState.
     */
    public static ItemStack getItemStackFromBlockState(@NotNull final BlockState blockState)
    {
        if (blockState.getBlock() instanceof IFluidBlock)
        {
            return FluidUtil.getFilledBucket(new FluidStack(((IFluidBlock) blockState.getBlock()).getFluid(), 1000));
        }
        final Item item = getItem(blockState);
        if (item != Items.AIR && item != null)
        {
            return new ItemStack(item, 1);
        }

        return new ItemStack(blockState.getBlock(), 1);
    }

    public static Item getItem(@NotNull final BlockState blockState)
    {
        final Block block = blockState.getBlock();
        if (block.equals(Blocks.LAVA))
        {
            return Items.LAVA_BUCKET;
        }
        else if (block instanceof CropsBlock)
        {
            final ItemStack stack = ((CropsBlock) block).getCloneItemStack(new SingleBlockBlockReader(blockState), BlockPos.ZERO, blockState);
            if (!stack.isEmpty())
            {
                return stack.getItem();
            }

            return Items.WHEAT_SEEDS;
        }
        // oh no...
        else if (block instanceof FarmlandBlock || block instanceof GrassPathBlock)
        {
            return Blocks.DIRT.asItem();
        }
        else if (block instanceof FireBlock)
        {
            return Items.FLINT_AND_STEEL;
        }
        else if (block instanceof FlowerPotBlock)
        {
            return Items.FLOWER_POT;
        }
        else if (block == Blocks.BAMBOO_SAPLING)
        {
            return Items.BAMBOO;
        }
        else
        {
            return block.asItem();
        }
    }

    public static ItemStack getModeItemStackFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IWithModeItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IWithModeItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getHighlightItemStackFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IWithHighlightItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IWithHighlightItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getMultiStateItemStackFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IMultiStateItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IMultiStateItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getPatternItemStackFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IPatternItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IPatternItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static Hand getPatternHandFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return Hand.MAIN_HAND;
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IPatternItem)
        {
            return Hand.OFF_HAND;
        }

        return Hand.MAIN_HAND;
    }

    public static ItemStack getBitItemStackFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IBitItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IBitItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static BlockState getHeldBitBlockStateFromPlayer(@Nullable final PlayerEntity playerEntity)
    {
        if (playerEntity == null)
        {
            return Blocks.AIR.defaultBlockState();
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IBitItem)
        {
            return ((IBitItem) playerEntity.getMainHandItem().getItem()).getBitState(playerEntity.getMainHandItem());
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IBitItem)
        {
            return ((IBitItem) playerEntity.getOffhandItem().getItem()).getBitState(playerEntity.getOffhandItem());
        }
        ;

        return Blocks.AIR.defaultBlockState();
    }

    public static BlockState getStateFromItem(
      final ItemStack is)
    {
        try
        {
            if (!is.isEmpty() && is.getItem() instanceof BlockItem)
            {
                final BlockItem blockItem = (BlockItem) is.getItem();
                return blockItem.getBlock().defaultBlockState();
            }
        }
        catch (final Throwable ignored)
        {
        }

        return Blocks.AIR.defaultBlockState();
    }
}
