package mod.chiselsandbits.block.entities;

import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemFactory;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.util.SingleBlockBlockReader;
import mod.chiselsandbits.api.util.SingleBlockWorldReader;
import mod.chiselsandbits.block.BitStorageBlock;
import mod.chiselsandbits.registrars.ModTileEntityTypes;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.plaf.nimbus.State;
import java.util.Objects;

import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class BitStorageBlockEntity extends TileEntity implements IItemHandler, IFluidHandler
{

    private final LazyOptional<IItemHandler>  itemHandler  = LazyOptional.of(() -> this);
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> this);
    private BlockState state   = null;
    private Fluid      myFluid = null;
    private int        bits    = 0;

    private int oldLV = -1;

    public BitStorageBlockEntity()
    {
        super(ModTileEntityTypes.BIT_STORAGE.get());
    }

    @Override
    public void onDataPacket(
      final NetworkManager net,
      final SUpdateTileEntityPacket pkt)
    {
        load(null, pkt.getTag());
    }

    @Override
    public void load(@Nullable final BlockState state, final @NotNull CompoundNBT nbt)
    {
        super.load(state, nbt);
        final String fluid = nbt.getString("fluid");

        if (fluid.equals(""))
        {
            if (nbt.contains("state")) {
                final CompoundNBT stateCompound = nbt.getCompound("state");
                this.state = NBTUtil.readBlockState(stateCompound);
            }
            else
            {
                final int rawState = nbt.getInt("blockstate");
                if (rawState != -1)
                {
                    this.state = IBlockStateIdManager.getInstance().getBlockStateFrom(rawState);
                }
                else
                {
                    this.state = null;
                }
            }
        }
        else
        {
            myFluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluid));
            this.state = myFluid.defaultFluidState().createLegacyBlock();
        }

        bits = nbt.getInt("bits");
    }

    @Override
    public @NotNull CompoundNBT save(final @NotNull CompoundNBT compound)
    {
        final CompoundNBT nbt = super.save(compound);
        nbt.putString("fluid", myFluid == null ? "" : Objects.requireNonNull(myFluid.getRegistryName()).toString());
        nbt.put("state", myFluid != null || state == null ? new CompoundNBT() : NBTUtil.writeBlockState(state));
        nbt.putInt("bits", bits);
        return nbt;
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        final CompoundNBT t = new CompoundNBT();
        return new SUpdateTileEntityPacket(getBlockPos(), 0, save(t));
    }

    @Override
    public @NotNull CompoundNBT getUpdateTag()
    {
        final CompoundNBT nbttagcompound = new CompoundNBT();
        return save(nbttagcompound);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
      final @NotNull Capability<T> capability,
      final Direction facing)
    {
        if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return itemHandler.cast();
        }

        if (capability == net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
        {
            return fluidHandler.cast();
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(
      final int slot)
    {
        if (bits > 0 && slot == 0 && (myFluid != null || state != null))
        {
            if (myFluid != null)
            {
                return getFluidBitStack(myFluid, bits);
            }
            else
            {
                return getBlockBitStack(state, bits);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @Nonnull
    ItemStack insertItem(
      final int slot,
      final ItemStack stack,
      final boolean simulate)
    {
        if (!stack.isEmpty() && stack.getItem() instanceof IBitItem)
        {
            final IBitItem bitItem = (IBitItem) stack.getItem();
            final BlockState blk = bitItem.getBitState(stack);

            final ItemStack fluidInsertion = attemptFluidBitStackInsertion(stack, simulate, blk);
            if (fluidInsertion != stack)
            {
                return fluidInsertion;
            }

            return attemptSolidBitStackInsertion(stack, simulate, blk);
        }
        else if (!stack.isEmpty() && IEligibilityManager.getInstance().analyse(stack.getItem()).canBeChiseled() && myFluid == null)
        {

            final BlockState stackState = ItemStackUtils.getStateFromItem(stack);
            if (stackState.getBlock() != Blocks.AIR)
            {
                if (this.state == null || state.isAir(new SingleBlockBlockReader(
                  state,
                  getBlockPos(),
                  getLevel()
                ), getBlockPos()))
                {
                    this.state = stackState;
                    this.bits = 4096;
                }
            }
        }
        return stack;
    }

    public int insertBits(
      final int count,
      final BlockState state,
      final boolean simulate)
    {
        return attemptSolidBitStackInsertion(IBitItemManager.getInstance().create(state, count), simulate, state).getCount();
    }

    @Override
    public @NotNull ItemStack extractItem(
      final int slot,
      final int amount,
      final boolean simulate)
    {
        return extractBits(slot, Math.min(amount, IBitItemManager.getInstance().getMaxStackSize()), simulate);
    }

    @Override
    public int getSlotLimit(
      final int slot)
    {
        return StateEntrySize.current().getBitsPerBlock();
    }

    @Override
    public boolean isItemValid(final int slot, @NotNull final ItemStack stack)
    {
        return !stack.isEmpty() && (stack.getItem() instanceof IBitItem || IEligibilityManager.getInstance().canBeChiseled(stack.getItem()));
    }

    @NotNull
    private ItemStack attemptFluidBitStackInsertion(final ItemStack stack, final boolean simulate, final BlockState blk)
    {
        Fluid f = null;
        for (final Fluid fl : ForgeRegistries.FLUIDS)
        {
            if (fl.defaultFluidState().createLegacyBlock().getBlock() == blk.getBlock())
            {
                f = fl;
                break;
            }
        }

        if (f == null)
        {
            return stack;
        }

        final ItemStack bitItem = getFluidBitStack(myFluid, bits);
        final boolean canInsert = bitItem.isEmpty() || ItemStack.tagMatches(bitItem, stack) && bitItem.getItem() == stack.getItem() || state == null;

        if (canInsert)
        {
            final int merged = bits + stack.getCount();
            final int amount = Math.min(merged, StateEntrySize.current().getBitsPerBlock());

            if (!simulate)
            {
                final Fluid oldFluid = myFluid;
                final BlockState oldState = state;
                final int oldBits = bits;

                myFluid = f;
                state = myFluid.defaultFluidState().createLegacyBlock();
                bits = amount;

                if (bits != oldBits || myFluid != oldFluid || oldState != null)
                {
                    saveAndUpdate();
                }
            }

            if (amount < merged)
            {
                final ItemStack out = stack.copy();
                out.setCount(merged - amount);
                return out;
            }

            return ItemStack.EMPTY;
        }
        return stack;
    }

    @NotNull
    private ItemStack attemptSolidBitStackInsertion(final ItemStack stack, final boolean simulate, final BlockState blk)
    {
        Fluid f = null;
        for (final Fluid fl : ForgeRegistries.FLUIDS)
        {
            if (fl.defaultFluidState().createLegacyBlock().getBlock() == blk.getBlock())
            {
                f = fl;
                break;
            }
        }

        if (f != null)
        {
            return stack;
        }

        final ItemStack bitItem = getBlockBitStack(blk, bits);
        final boolean canInsert = bitItem.isEmpty() || ItemStack.tagMatches(bitItem, stack) && bitItem.getItem() == stack.getItem();

        if (canInsert)
        {
            final int merged = bits + stack.getCount();
            final int amount = Math.min(merged, StateEntrySize.current().getBitsPerBlock());

            if (!simulate)
            {
                final Fluid oldFluid = myFluid;
                final BlockState oldBlockState = this.state;
                final int oldBits = bits;

                myFluid = null;
                state = blk;
                bits = amount;

                if (bits != oldBits || state != oldBlockState || oldFluid != null)
                {
                    saveAndUpdate();
                }
            }

            if (amount < merged)
            {
                final ItemStack out = stack.copy();
                out.setCount(merged - amount);
                return out;
            }

            return ItemStack.EMPTY;
        }
        return stack;
    }

    public FluidStack getBitsAsFluidStack()
    {
        if (myFluid == null && state != null)
        {
            return FluidStack.EMPTY;
        }

        if (bits > 0 && myFluid != null)
        {
            return new FluidStack(myFluid, bits);
        }

        return null;
    }

    public boolean extractBits(
      final PlayerEntity playerIn,
      final double hitX,
      final double hitY,
      final double hitZ,
      final BlockPos pos)
    {
        if (!playerIn.isShiftKeyDown())
        {
            final ItemStack is = extractItem(0, 64, false);
            if (!is.isEmpty())
            {
                if (is.getItem() instanceof IBitItem)
                {
                    final IBitItem bitItem = (IBitItem) is.getItem();
                    final BlockState blockState = bitItem.getBitState(is);

                    BitInventoryUtils.insertIntoOrSpawn(
                      playerIn,
                      blockState,
                      is.getCount()
                    );
                }
            }
            return true;
        }

        return false;
    }

    public @Nonnull
    ItemStack extractBits(
      final int slot,
      final int amount,
      final boolean simulate)
    {
        final ItemStack contents = getStackInSlot(slot);

        if (!contents.isEmpty() && amount > 0)
        {
            // how many to extract?
            contents.setCount(Math.min(amount, contents.getCount()));

            // modulate?
            if (!simulate)
            {
                final int oldBits = bits;

                bits -= contents.getCount();
                if (bits <= 0)
                {
                    bits = 0;
                    state = null;
                    myFluid = null;
                }

                if (bits != oldBits)
                {
                    saveAndUpdate();
                }
            }

            return contents;
        }

        return ItemStack.EMPTY;
    }

    private void saveAndUpdate()
    {
        if (level == null || getLevel() == null)
        {
            return;
        }

        setChanged();
        getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);

        final int lv = getLightValue();
        if (oldLV != lv)
        {
            getLevel().getLightEngine().checkBlock(getBlockPos());
            oldLV = lv;
        }
    }

    public @Nonnull
    ItemStack getFluidBitStack(
      final Fluid liquid,
      final int amount)
    {
        if (liquid == null)
        {
            return ItemStack.EMPTY;
        }

        return IBitItemManager.getInstance().create(
          liquid.defaultFluidState().createLegacyBlock(),
          amount
        );
    }

    public @Nonnull
    ItemStack getBlockBitStack(
      final BlockState blockState,
      final int amount)
    {
        if (blockState == null || blockState.getBlockState() == null)
        {
            return ItemStack.EMPTY;
        }

        return IBitItemManager.getInstance().create(
          blockState,
          amount
        );
    }

    public int getLightValue()
    {
        final BlockState workingState = myFluid == null ? state : myFluid.defaultFluidState().createLegacyBlock();
        if (workingState == null)
        {
            return 0;
        }

        return workingState.getLightValue(
          new SingleBlockWorldReader(
            workingState,
            getBlockPos(),
            getLevel()
          ),
          getBlockPos()
        );
    }

    public boolean addAllPossibleBits(
      final PlayerEntity playerIn)
    {
        if (playerIn != null && playerIn.isShiftKeyDown() && state != null && !state.isAir(
          new SingleBlockBlockReader(
            state,
            state.getBlock(),
            getBlockPos(),
            getLevel()
          ),
          getBlockPos()
        ))
        {
            final IBitInventory bitInventory = IBitInventoryManager.getInstance().create(playerIn);
            final int extractionAmount = Math.min(
              StateEntrySize.current().getBitsPerBlock() - bits,
              bitInventory.getMaxExtractAmount(state)
            );

            bitInventory.extract(state, extractionAmount);

            bits += extractionAmount;
            setChanged();
        }

        return false;
    }

    public boolean addHeldBits(
      final @Nonnull ItemStack current,
      final PlayerEntity playerIn)
    {
        if (playerIn.isShiftKeyDown() || this.bits == 0)
        {
            if (current.getItem() instanceof IBitItem || IEligibilityManager.getInstance().canBeChiseled(current.getItem()))
            {
                final ItemStack resultStack = insertItem(0, current, false);
                if (!playerIn.isCreative())
                {
                    playerIn.inventory.setItem(playerIn.inventory.selected, resultStack);
                    playerIn.inventory.setChanged();
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public int getTanks()
    {
        return 1;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(final int tank)
    {
        return getAccessableFluid();
    }

    public FluidStack getAccessableFluid()
    {
        if (myFluid == null && state != null)
        {
            return FluidStack.EMPTY;
        }

        int mb = (int) (((float) bits / StateEntrySize.current().getBitsPerBlock()) * 1000);

        if (mb > 0 && myFluid != null)
        {
            return new FluidStack(myFluid, mb);
        }

        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(final int tank)
    {
        return FluidAttributes.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(final int tank, @NotNull final FluidStack stack)
    {
        if (getAccessableFluid().isEmpty() && state == null)
        {
            return true;
        }

        if (state != null)
        {
            return false;
        }

        return Objects.equals(getAccessableFluid().getFluid().getRegistryName(), stack.getFluid().getRegistryName());
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action)
    {
        if (resource == null || state != null)
        {
            return 0;
        }

        if (resource.getAmount() > 0)
        {
            final int bitCount = (int) (resource.getAmount() * (StateEntrySize.current().getBitsPerBlock() / 1000f));
            final ItemStack bitItems = getFluidBitStack(resource.getFluid(), bitCount);
            final ItemStack leftOver = insertItem(0, bitItems, action.simulate());

            if (leftOver.isEmpty())
            {
                return resource.getAmount();
            }

            int mbUsedUp = (int) (leftOver.getCount() / (StateEntrySize.current().getBitsPerBlock() / 1000f));

            return resource.getAmount() - mbUsedUp;
        }

        return 0;
    }

    @NotNull
    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action)
    {
        if (resource == null || state != null)
        {
            return FluidStack.EMPTY;
        }

        final FluidStack a = getAccessableFluid();

        if (a != null && resource.containsFluid(a)) // right type of fluid.
        {
            final int requestedMbAmount = resource.getAmount();

            final int mbThatCanBeRemoved = Math.min(a.getAmount(), requestedMbAmount);
            if (mbThatCanBeRemoved > 0)
            {
                a.setAmount(mbThatCanBeRemoved);

                if (action.execute())
                {
                    final int bitCount = (int) (mbThatCanBeRemoved * (StateEntrySize.current().getBitsPerBlock() / 1000f));
                    extractBits(0, bitCount, false);
                }

                return a;
            }
        }

        return FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action)
    {
        if (maxDrain <= 0 || state != null)
        {
            return FluidStack.EMPTY;
        }

        final FluidStack a = getAccessableFluid();

        if (a != null) // right type of fluid.
        {

            final int mbThatCanBeRemoved = Math.min(a.getAmount(), maxDrain);
            if (mbThatCanBeRemoved > 0)
            {
                a.setAmount(mbThatCanBeRemoved);

                if (action.execute())
                {
                    final int bitCount = (int) (mbThatCanBeRemoved * (StateEntrySize.current().getBitsPerBlock() / 1000f));
                    extractBits(0, bitCount, false);
                }

                return a;
            }
        }

        return FluidStack.EMPTY;
    }

    public BlockState getState()
    {
        return state;
    }

    public Fluid getMyFluid()
    {
        return myFluid;
    }

    public int getBits()
    {
        return bits;
    }

    public Direction getFacing() {
        return getLevel().getBlockState(getBlockPos()).getValue(BitStorageBlock.FACING);
    }
}
