package mod.chiselsandbits.utils;

import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ChunkSectionUtils
{

    private static final Logger LOGGER = LogManager.getLogger();

    private ChunkSectionUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ChunkSectionUtils. This is a utility class");
    }

    public static CompoundNBT serializeNBT(final ChunkSection chunkSection) {
        final CompoundNBT compressedSectionData = new CompoundNBT();

        chunkSection.getStates().write(
          compressedSectionData,
          NbtConstants.PALETTE,
          NbtConstants.BLOCK_STATES
        );

        return compressedSectionData;
    }

    public static CompoundNBT serializeNBTCompressed(final ChunkSection chunkSection) {
        final CompoundNBT compressedSectionData = new CompoundNBT();

        chunkSection.getStates().write(
          compressedSectionData,
          NbtConstants.PALETTE,
          NbtConstants.BLOCK_STATES
        );

        try
        {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(compressedSectionData, outputStream);
            final byte[] compressedData = outputStream.toByteArray();
            final CompoundNBT gzipCompressedTag = new CompoundNBT();
            gzipCompressedTag.putBoolean(NbtConstants.DATA_IS_COMPRESSED, true);
            gzipCompressedTag.putByteArray(NbtConstants.COMPRESSED_DATA, compressedData);
            return gzipCompressedTag;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to compress chiseled block data.", e);
            return compressedSectionData;
        }
    }

    public static void deserializeNBT(final ChunkSection chunkSection, final CompoundNBT nbt) {
        if (nbt.isEmpty())
            return;

        if (nbt.contains(NbtConstants.DATA_IS_COMPRESSED, Constants.NBT.TAG_BYTE)
              && nbt.getBoolean(NbtConstants.DATA_IS_COMPRESSED)
              && nbt.contains(NbtConstants.COMPRESSED_DATA, Constants.NBT.TAG_BYTE_ARRAY)) {
            try
            {
                final byte[] compressedData = nbt.getByteArray(NbtConstants.COMPRESSED_DATA);
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
                final CompoundNBT compoundTag = CompressedStreamTools.readCompressed(inputStream);

                chunkSection.getStates().read(
                  compoundTag.getList(NbtConstants.PALETTE, Constants.NBT.TAG_COMPOUND),
                  compoundTag.getLongArray(NbtConstants.BLOCK_STATES)
                );

                chunkSection.recalcBlockCounts();
                return;
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to decompress chiseled block entity data. Resetting data.");
                ChunkSectionUtils.fillFromBottom(chunkSection, Blocks.AIR.defaultBlockState(), StateEntrySize.current().getBitsPerBlock());
                chunkSection.recalcBlockCounts();
                return;
            }
        }

        chunkSection.getStates().read(
          nbt.getList(NbtConstants.PALETTE, Constants.NBT.TAG_COMPOUND),
          nbt.getLongArray(NbtConstants.BLOCK_STATES)
        );

        chunkSection.recalcBlockCounts();
    }

    public static ChunkSection rotate90Degrees(final ChunkSection source, final Direction.Axis axis, final int rotationCount) {
        if (rotationCount == 0)
            return source;

        final Vector3d centerVector = new Vector3d(7.5d, 7.5d, 7.5d);

        final ChunkSection target = new ChunkSection(0);

        for (int x = 0; x < 16; x++)
        {
            for (int y = 0; y < 16; y++)
            {
                for (int z = 0; z < 16; z++)
                {
                    final Vector3d workingVector = new Vector3d(x, y, z);
                    Vector3d rotatedVector = workingVector.subtract(centerVector);
                    for (int i = 0; i < rotationCount; i++)
                    {
                        rotatedVector = VectorUtils.rotate90Degrees(rotatedVector, axis);
                    }

                    final BlockPos sourcePos = new BlockPos(workingVector);
                    final Vector3d offsetPos = rotatedVector.add(centerVector).multiply(1000,1000,1000);
                    final BlockPos targetPos = new BlockPos(new Vector3d(Math.round(offsetPos.x()), Math.round(offsetPos.y()), Math.round(offsetPos.z())).multiply(1/1000d,1/1000d,1/1000d));

                    target.setBlockState(
                      targetPos.getX(),
                      targetPos.getY(),
                      targetPos.getZ(),
                      source.getBlockState(
                        sourcePos.getX(),
                        sourcePos.getY(),
                        sourcePos.getZ()
                      )
                    );
                }
            }
        }

        return target;
    }

    public static ChunkSection cloneSection(final ChunkSection lazyChunkSection)
    {
        final ChunkSection clone = new ChunkSection(0);
        deserializeNBT(clone, serializeNBT(lazyChunkSection));

        return clone;
    }

    public static void fillFromBottom(
      final ChunkSection chunkSection,
      final BlockState blockState,
      final int amount
    ) {
        final int loopCount = Math.max(0, Math.min(amount, StateEntrySize.current().getBitsPerBlock()));
        if (loopCount == 0)
            return;

        int count = 0;
        for (int y = 0; y < StateEntrySize.current().getBitsPerBlockSide(); y++)
        {
            for (int x = 0; x < StateEntrySize.current().getBitsPerBlockSide(); x++)
            {
                for (int z = 0; z < StateEntrySize.current().getBitsPerBlockSide(); z++)
                {
                    chunkSection.setBlockState(
                      x, y, z,
                      blockState
                    );

                    count++;
                    if (count == loopCount)
                        return;
                }
            }
        }
    }

    public static ChunkSection mirror(final ChunkSection lazyChunkSection, final Direction.Axis axis)
    {
        final ChunkSection result = new ChunkSection(0);

        for (int y = 0; y < StateEntrySize.current().getBitsPerBlockSide(); y++)
        {
            for (int x = 0; x < StateEntrySize.current().getBitsPerBlockSide(); x++)
            {
                for (int z = 0; z < StateEntrySize.current().getBitsPerBlockSide(); z++)
                {
                    final BlockState blockState = lazyChunkSection.getBlockState(x, y, z);

                    final int mirroredX = axis == Direction.Axis.X ? (StateEntrySize.current().getBitsPerBlockSide() - x - 1) : x;
                    final int mirroredY = axis == Direction.Axis.Y ? (StateEntrySize.current().getBitsPerBlockSide() - y - 1) : y;
                    final int mirroredZ = axis == Direction.Axis.Z ? (StateEntrySize.current().getBitsPerBlockSide() - z - 1) : z;

                    result.setBlockState(
                      mirroredX, mirroredY, mirroredZ,
                      blockState,
                      false
                    );
                }
            }
        }

        return result;
    }
}
