package mod.chiselsandbits.chiseledblock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob.BlobStats;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.chiseledblock.data.VoxelNeighborRenderTracker;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.core.api.BitAccess;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.integration.mcmultipart.MCMultipartProxy;
import mod.chiselsandbits.interfaces.IChiseledTileContainer;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import mod.chiselsandbits.render.chiseledblock.tesr.ChisledBlockRenderChunkTESR;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityBlockChiseled extends TileEntity implements IChiseledTileContainer
{

	public static final String block_prop = "b";
	public static final String side_prop = "s";
	public static final String voxel_prop = "v";
	public static final String versioned_voxel_prop = "X";
	public static final String light_opacity_prop = "l";
	public static final String light_prop = "lv";

	private IExtendedBlockState state;
	public IChiseledTileContainer occlusionState;

	public TileEntityBlockChiseled()
	{

	}

	public IChiseledTileContainer getTileContainer()
	{
		if ( occlusionState != null )
		{
			return occlusionState;
		}

		return this;
	}

	@Override
	public boolean isBlobOccluded(
			final VoxelBlob blob )
	{
		return false;
	}

	@Override
	public void saveData()
	{
		super.markDirty();
	}

	@Override
	public void sendUpdate()
	{
		worldObj.markBlockForUpdate( pos );
	}

	public void copyFrom(
			final TileEntityBlockChiseled src )
	{
		state = src.state;
	}

	public IExtendedBlockState getBasicState()
	{
		return getState( false, 0 );
	}

	public IExtendedBlockState getRenderState()
	{
		return getState( true, 1 );
	}

	protected IExtendedBlockState getState(
			final boolean updateNeightbors,
			final int updateCost )
	{
		if ( state == null )
		{
			return (IExtendedBlockState) ChiselsAndBits.getBlocks().getChiseledDefaultState();
		}

		if ( updateNeightbors )
		{
			final boolean isDyanmic = this instanceof TileEntityBlockChiseledTESR;

			final VoxelNeighborRenderTracker vns = state.getValue( BlockChiseled.n_prop );
			if ( vns == null )
			{
				return state;
			}

			vns.update( isDyanmic, worldObj, pos );

			tesrUpdate( vns );

			final TileEntityBlockChiseled self = this;
			if ( vns.isAboveLimit() && !isDyanmic )
			{
				ChisledBlockRenderChunkTESR.addTask( new Runnable() {

					@Override
					public void run()
					{
						if ( self.worldObj != null && self.pos != null )
						{
							final TileEntity current = self.worldObj.getTileEntity( self.pos );
							if ( current == self )
							{
								final TileEntityBlockChiseledTESR TESR = new TileEntityBlockChiseledTESR();
								TESR.copyFrom( self );
								self.worldObj.setTileEntity( self.pos, TESR );
								self.worldObj.markBlockForUpdate( self.pos );
							}
							else
							{
								MCMultipartProxy.proxyMCMultiPart.convertTo( current, new TileEntityBlockChiseledTESR() );
							}
						}
					}

				} );
			}
			else if ( !vns.isAboveLimit() && isDyanmic )
			{
				ChisledBlockRenderChunkTESR.addTask( new Runnable() {

					@Override
					public void run()
					{
						if ( self.worldObj != null && self.pos != null )
						{
							final TileEntity current = self.worldObj.getTileEntity( self.pos );
							if ( current == self )
							{
								final TileEntityBlockChiseled nonTesr = new TileEntityBlockChiseled();
								nonTesr.copyFrom( self );
								self.worldObj.setTileEntity( self.pos, nonTesr );
								self.worldObj.markBlockForUpdate( self.pos );
							}
							else
							{
								MCMultipartProxy.proxyMCMultiPart.convertTo( current, new TileEntityBlockChiseled() );
							}
						}
					}

				} );
			}
		}

		return state;
	}

	protected void tesrUpdate(
			final VoxelNeighborRenderTracker vns )
	{

	}

	public BlockBitInfo getBlockInfo(
			final Block alternative )
	{
		return BlockBitInfo.getBlockInfo( getBlockState( alternative ) );
	}

	public IBlockState getBlockState(
			final Block alternative )
	{
		final Integer stateID = getBasicState().getValue( BlockChiseled.block_prop );

		if ( stateID != null )
		{
			final IBlockState state = Block.getStateById( stateID );
			if ( state != null )
			{
				return state;
			}
		}

		return alternative.getDefaultState();
	}

	public void setState(
			final IExtendedBlockState state )
	{
		this.state = state;
	}

	@Override
	public boolean shouldRefresh(
			final World world,
			final BlockPos pos,
			final IBlockState oldState,
			final IBlockState newState )
	{
		return oldState.getBlock() != newState.getBlock();
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	public Packet getDescriptionPacket()
	{
		final NBTTagCompound nbttagcompound = new NBTTagCompound();
		writeChisleData( nbttagcompound );

		if ( nbttagcompound.hasNoTags() )
		{
			return null;
		}

		return new S35PacketUpdateTileEntity( pos, 255, nbttagcompound );
	}

	@Override
	public void onDataPacket(
			final NetworkManager net,
			final S35PacketUpdateTileEntity pkt )
	{
		readChisleData( pkt.getNbtCompound() );
		if ( worldObj != null )
		{
			worldObj.markBlockForUpdate( pos );
		}
	}

	public final void writeChisleData(
			final NBTTagCompound compound )
	{
		final Integer b = getBasicState().getValue( BlockChiseled.block_prop );
		final Integer s = getBasicState().getValue( BlockChiseled.side_prop );
		final Float l = getBasicState().getValue( BlockChiseled.opacity_prop );
		final Integer lv = getBasicState().getValue( BlockChiseled.light_prop );
		final VoxelBlobStateReference vbs = getBasicState().getValue( BlockChiseled.v_prop );

		if ( b == null || vbs == null )
		{
			return;
		}

		if ( b != null && vbs != null )
		{
			compound.setFloat( light_opacity_prop, l == null ? 1.0f : l );
			compound.setInteger( light_prop, lv == null ? 0 : lv );
			compound.setInteger( block_prop, b );
			compound.setInteger( side_prop, s );
			compound.setByteArray( versioned_voxel_prop, vbs.getByteArray() );
		}
	}

	public final void readChisleData(
			final NBTTagCompound compound )
	{
		final Integer oldLV = getBasicState().getValue( BlockChiseled.light_prop );

		final int sideFlags = compound.getInteger( side_prop );
		int b = compound.getInteger( block_prop );
		final float l = compound.getFloat( light_opacity_prop );
		final int lv = compound.getInteger( light_prop );
		byte[] v = compound.getByteArray( versioned_voxel_prop );

		if ( v == null || v.length == 0 )
		{
			final byte[] vx = compound.getByteArray( voxel_prop );
			if ( v != null && vx.length > 0 )
			{
				final VoxelBlob bx = new VoxelBlob();

				try
				{
					bx.fromLegacyByteArray( vx );
				}
				catch ( final IOException e )
				{
				}

				v = bx.blobToBytes( VoxelBlob.VERSION_COMPACT );

				Log.info( "Converted: " + vx.length + " -> " + v.length );
			}
		}

		if ( b == 0 )
		{
			// if load fails default to cobble stone...
			b = Block.getStateId( Blocks.cobblestone.getDefaultState() );
		}

		IExtendedBlockState newstate = getBasicState()
				.withProperty( BlockChiseled.side_prop, sideFlags )
				.withProperty( BlockChiseled.block_prop, b )
				.withProperty( BlockChiseled.light_prop, lv )
				.withProperty( BlockChiseled.opacity_prop, l )
				.withProperty( BlockChiseled.v_prop, new VoxelBlobStateReference( v, getPositionRandom( pos ) ) );

		final VoxelNeighborRenderTracker tracker = newstate.getValue( BlockChiseled.n_prop );

		if ( tracker == null )
		{
			newstate = newstate.withProperty( BlockChiseled.n_prop, new VoxelNeighborRenderTracker() );
		}
		else
		{
			tracker.isDynamic();
		}

		setState( newstate );

		if ( oldLV == null || oldLV != lv )
		{
			if ( worldObj != null )
			{
				worldObj.checkLight( pos );
			}
		}
	}

	@Override
	public void writeToNBT(
			final NBTTagCompound compound )
	{
		super.writeToNBT( compound );
		writeChisleData( compound );
	}

	@Override
	public void readFromNBT(
			final NBTTagCompound compound )
	{
		super.readFromNBT( compound );
		readChisleData( compound );
	}

	public void fillWith(
			final IBlockState blockType )
	{
		final int ref = Block.getStateId( blockType );

		IExtendedBlockState state = getBasicState()
				.withProperty( BlockChiseled.side_prop, 0xFF )
				.withProperty( BlockChiseled.opacity_prop, 1.0f )
				.withProperty( BlockChiseled.light_prop, blockType.getBlock().getLightValue() )
				.withProperty( BlockChiseled.v_prop, new VoxelBlobStateReference( Block.getStateId( blockType ), getPositionRandom( pos ) ) );

		final VoxelNeighborRenderTracker tracker = state.getValue( BlockChiseled.n_prop );

		if ( tracker == null )
		{
			state = state.withProperty( BlockChiseled.n_prop, new VoxelNeighborRenderTracker() );
		}
		else
		{
			tracker.isDynamic();
		}

		// required for placing bits
		if ( ref != 0 )
		{
			state = state.withProperty( BlockChiseled.block_prop, ref );
		}

		setState( state );

		getTileContainer().saveData();
	}

	private long getPositionRandom(
			final BlockPos pos )
	{
		if ( pos != null && FMLCommonHandler.instance().getSide() == Side.CLIENT )
		{
			return MathHelper.getPositionRandom( pos );
		}

		return 0;
	}

	public VoxelBlobStateReference getBlobStateReference()
	{
		return getBasicState().getValue( BlockChiseled.v_prop );
	}

	public VoxelBlob getBlob()
	{
		VoxelBlob vb = null;
		final VoxelBlobStateReference vbs = getBlobStateReference();

		if ( vbs != null )
		{
			vb = vbs.getVoxelBlob();

			if ( vb == null )
			{
				vb = new VoxelBlob();
				vb.fill( Block.getStateId( Blocks.cobblestone.getDefaultState() ) );
			}
		}
		else
		{
			vb = new VoxelBlob();
		}

		return vb;
	}

	public IBlockState getPreferedBlock()
	{
		return ChiselsAndBits.getBlocks().getConversionWithDefault( getBlockState( Blocks.stone ).getBlock().getMaterial() ).getDefaultState();
	}

	public void setBlob(
			final VoxelBlob vb )
	{
		setBlob( vb, true );
	}

	public void setBlob(
			final VoxelBlob vb,
			final boolean triggerUpdates )
	{
		final Integer olv = getBasicState().getValue( BlockChiseled.light_prop );

		final BlobStats common = vb.getVoxelStats();
		final float opacity = vb.getOpacity();
		final float light = common.blockLight;
		final int lv = Math.max( 0, Math.min( 15, (int) ( light * 15 ) ) );

		// are most of the bits in the center solid?
		final int sideFlags = vb.getSideFlags( 5, 11, 4 * 4 );

		if ( worldObj == null )
		{
			if ( common.mostCommonState == 0 )
			{
				common.mostCommonState = getBasicState().getValue( BlockChiseled.block_prop );
			}

			IExtendedBlockState newState = getBasicState()
					.withProperty( BlockChiseled.side_prop, sideFlags )
					.withProperty( BlockChiseled.v_prop, new VoxelBlobStateReference( vb.blobToBytes( VoxelBlob.VERSION_COMPACT ), getPositionRandom( pos ) ) )
					.withProperty( BlockChiseled.light_prop, lv )
					.withProperty( BlockChiseled.opacity_prop, opacity )
					.withProperty( BlockChiseled.n_prop, new VoxelNeighborRenderTracker() )
					.withProperty( BlockChiseled.block_prop, common.mostCommonState );

			final VoxelNeighborRenderTracker tracker = newState.getValue( BlockChiseled.n_prop );

			if ( tracker == null )
			{
				newState = newState.withProperty( BlockChiseled.n_prop, new VoxelNeighborRenderTracker() );
			}
			else
			{
				tracker.isDynamic();
			}

			setState( newState );
			return;
		}

		if ( common.isFullBlock )
		{
			worldObj.setBlockState( pos, Block.getStateById( common.mostCommonState ), triggerUpdates ? 3 : 0 );
		}
		else if ( common.mostCommonState != 0 )
		{
			setState( getBasicState()
					.withProperty( BlockChiseled.side_prop, sideFlags )
					.withProperty( BlockChiseled.v_prop, new VoxelBlobStateReference( vb.blobToBytes( VoxelBlob.VERSION_COMPACT ), getPositionRandom( pos ) ) )
					.withProperty( BlockChiseled.light_prop, lv )
					.withProperty( BlockChiseled.opacity_prop, opacity )
					.withProperty( BlockChiseled.block_prop, common.mostCommonState ) );

			getTileContainer().saveData();
			getTileContainer().sendUpdate();

			// since its possible for bits to occlude parts.. update every time.
			final Block blk = worldObj.getBlockState( pos ).getBlock();
			MCMultipartProxy.proxyMCMultiPart.triggerPartChange( worldObj.getTileEntity( pos ) );
			worldObj.notifyBlockOfStateChange( pos, blk );

			if ( triggerUpdates )
			{
				worldObj.notifyNeighborsOfStateChange( pos, blk );
			}
		}
		else
		{
			ModUtil.removeChisledBlock( worldObj, pos );
		}

		if ( olv == null || olv != lv )
		{
			worldObj.checkLight( pos );
		}
	}

	static private class ItemStackGeneratedCache
	{
		public ItemStackGeneratedCache(
				final ItemStack itemstack,
				final VoxelBlobStateReference blobStateReference,
				final int rotations2 )
		{
			out = itemstack == null ? null : itemstack.copy();
			ref = blobStateReference;
			rotations = rotations2;
		}

		final ItemStack out;
		final VoxelBlobStateReference ref;
		final int rotations;
	};

	/**
	 * prevent mods that constantly ask for pick block from killing the
	 * client... ( looking at you waila )
	 **/
	private ItemStackGeneratedCache pickcache = null;

	public ItemStack getItemStack(
			final Block what,
			final EntityPlayer player )
	{
		final ItemStackGeneratedCache cache = pickcache;

		if ( player != null )
		{
			EnumFacing enumfacing = ModUtil.getPlaceFace( player );
			final int rotations = ModUtil.getRotationIndex( enumfacing );

			if ( cache != null && cache.rotations == rotations && cache.ref == getBlobStateReference() )
			{
				return cache.out.copy();
			}

			VoxelBlob vb = getBlob();

			int countDown = rotations;
			while ( countDown > 0 )
			{
				countDown--;
				enumfacing = enumfacing.rotateYCCW();
				vb = vb.spin( Axis.Y );
			}

			final BitAccess ba = new BitAccess( null, null, vb, VoxelBlob.NULL_BLOB );
			final ItemStack itemstack = ba.getBitsAsItem( enumfacing, ItemType.CHISLED_BLOCK );

			pickcache = new ItemStackGeneratedCache( itemstack, getBlobStateReference(), rotations );
			return itemstack;
		}
		else
		{
			if ( cache != null && cache.rotations == 0 && cache.ref == getBlobStateReference() )
			{
				return cache.out.copy();
			}

			final BitAccess ba = new BitAccess( null, null, getBlob(), VoxelBlob.NULL_BLOB );
			final ItemStack itemstack = ba.getBitsAsItem( null, ItemType.CHISLED_BLOCK );

			pickcache = new ItemStackGeneratedCache( itemstack, getBlobStateReference(), 0 );
			return itemstack;
		}
	}

	public boolean isSideSolid(
			final EnumFacing side )
	{
		final Integer sideFlags = getBasicState().getValue( BlockChiseled.side_prop );

		if ( sideFlags == null )
		{
			return true; // if torches or other blocks are on the block this
			// prevents a conversion from crashing.
		}

		return ( sideFlags & 1 << side.ordinal() ) != 0;
	}

	@SideOnly( Side.CLIENT )
	public boolean isSideOpaque(
			final EnumFacing side )
	{
		final Integer sideFlags = ChiseledBlockSmartModel.getSides( this );
		return ( sideFlags & 1 << side.ordinal() ) != 0;
	}

	public void postChisel(
			final VoxelBlob vb )
	{
		setBlob( vb );
	}

	public void rotateBlock(
			final EnumFacing axis )
	{
		setBlob( getBlob().spin( axis.getAxis() ) );
	}

	public boolean canMerge(
			final VoxelBlob voxelBlob )
	{
		final VoxelBlob vb = getBlob();
		final IChiseledTileContainer occ = getTileContainer();

		if ( vb.canMerge( voxelBlob ) && !occ.isBlobOccluded( voxelBlob ) )
		{
			return true;
		}

		return false;
	}

	public List<AxisAlignedBB> getOcclusionBoxes()
	{
		final VoxelBlobStateReference ref = getBlobStateReference();

		if ( ref != null )
		{
			return ref.getOcclusionBoxes();
		}
		else
		{
			return Collections.emptyList();
		}
	}

}
