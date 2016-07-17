package mod.chiselsandbits.share;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.data.IVoxelAccess;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

public class ShareWorldData
{

	public static class SharedWorldBlock
	{
		public SharedWorldBlock(
				final byte[] bytes )
		{
			if ( bytes.length > 0 && bytes[0] == 1 ) // block
			{
				blob = null;
				isBlob = false;
				blockName = new String( bytes, 1, bytes.length - 1 );
			}
			else if ( bytes.length > 0 && bytes[0] == 2 ) // blob )
			{
				isBlob = true;
				blockName = null;

				final byte[] tmp = new byte[bytes.length - 1];
				System.arraycopy( bytes, 1, tmp, 0, tmp.length );
				blob = new VoxelBlobStateReference( tmp, 0 );
			}
			else
			{
				isBlob = false;
				blob = null;
				blockName = "minecraft:air";
			}
		}

		final boolean isBlob;

		final String blockName;
		final IVoxelAccess blob;

		public IBlockState getState()
		{
			return ModUtil.getStateFromString( blockName, "" );
		}
	};

	private int xSize;
	private int ySize;
	private int zSize;
	private int xySize;

	int[] blocks;
	SharedWorldBlock[] models;

	public int getXSize()
	{
		return xSize;
	}

	public int getYSize()
	{
		return ySize;
	}

	public int getZSize()
	{
		return zSize;
	}

	public ShareWorldData(
			String data ) throws IOException
	{
		final String header = "[C&B](";
		final String footer = ")[C&B]";

		int start = data.indexOf( header );
		final int end = data.indexOf( footer );

		if ( start == -1 || end == -1 )
		{
			throw new IOException( "Unable to locate C&B Data." );
		}

		start += header.length();
		data = data.substring( start, end );
		final byte[] compressed = Base64.getDecoder().decode( data );
		readCompressed( compressed );
	}

	public ShareWorldData(
			final byte[] compressed ) throws IOException
	{
		readCompressed( compressed );
	}

	public ShareWorldData(
			final BufferedImage img ) throws IOException
	{
		final byte[] data = new byte[img.getWidth() * img.getHeight() * 4];

		final ScreenshotDecoder sdecoder = new ScreenshotDecoder();
		final byte[] compressed = sdecoder.imageDecode( data );

		readCompressed( compressed );
	}

	private void readCompressed(
			final byte[] compressed ) throws IOException
	{
		byte[] uncompressed = null;

		final InflaterInputStream in = new InflaterInputStream( new ByteArrayInputStream( compressed ) );
		final ByteArrayOutputStream bout = new ByteArrayOutputStream( compressed.length );

		int b;
		while ( ( b = in.read() ) != -1 )
		{
			bout.write( b );
		}
		bout.close();
		in.close();

		uncompressed = bout.toByteArray();

		final ShareFormatReader reader = new ShareFormatReader( uncompressed );

		final int format = reader.readInt();

		if ( format != 1 )
		{
			throw new IOException( "Invalid format!" );
		}

		xSize = reader.readInt();
		ySize = reader.readInt();
		zSize = reader.readInt();
		xySize = xSize * ySize;

		final int bits = reader.readInt();

		blocks = new int[xSize * ySize * zSize];
		for ( int x = 0; x < blocks.length; x++ )
		{
			blocks[x] = reader.readBits( bits );
		}

		reader.snapToByte();

		final int modelCount = reader.readInt();
		models = new SharedWorldBlock[modelCount];
		for ( int x = 0; x < models.length; x++ )
		{
			models[x] = new SharedWorldBlock( reader.readBytes() );
		}

		structureData = new byte[Math.min( reader.consumedBytes(), uncompressed.length )];
		System.arraycopy( uncompressed, 0, structureData, 0, structureData.length );
	}

	private byte[] structureData;

	public byte[] getStuctureData() throws IOException
	{
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream( structureData.length );

		try
		{
			final DeflaterOutputStream zipStream = new DeflaterOutputStream( byteStream );
			try
			{
				zipStream.write( structureData );
			}
			finally
			{
				zipStream.close();
			}
		}
		finally
		{
			byteStream.close();
		}

		return byteStream.toByteArray();
	}

	public IVoxelAccess getBlob(
			final int x,
			final int y,
			final int z )
	{
		if ( x >= 0 && y >= 0 && z >= 0 && x < xSize && y < ySize && z < zSize )
		{
			final int modelid = blocks[x + y * xSize + z * xySize];
			if ( models.length > modelid && modelid >= 0 )
			{
				final SharedWorldBlock swb = models[modelid];

				if ( swb.blob == null )
				{
					final IBlockState bs = swb.getState();
					final int stateID = Block.getStateId( bs );
					if ( BlockBitInfo.supportsBlock( bs ) )
					{
						return new VoxelBlobStateReference( stateID, 0 );
					}
					return new VoxelBlobStateReference( 0, 0 );
				}

				return swb.blob;
			}
		}

		return new VoxelBlobStateReference( 0, 0 );
	}

}