package mod.chiselsandbits.chiseling.modes.sphere;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.world.IInWorldStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.batched.IBatchMutation;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.api.util.SingleBlockBlockReader;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.registrars.ModMetadataKeys;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SphereChiselMode extends ForgeRegistryEntry<IChiselMode> implements IChiselMode
{
    private final int diameter;
    private final IFormattableTextComponent displayName;
    private final IFormattableTextComponent multiLineDisplayName;
    private final ResourceLocation          iconName;

    SphereChiselMode(
      final int diameter,
      final IFormattableTextComponent displayName,
      final IFormattableTextComponent multiLineDisplayName,
      final ResourceLocation iconName)
    {
        this.diameter = diameter;
        this.displayName = displayName;
        this.multiLineDisplayName = multiLineDisplayName;
        this.iconName = iconName;
    }

    @Override
    public ClickProcessingState onLeftClickBy(
      final PlayerEntity playerEntity, final IChiselingContext context)
    {
        final Either<ClickProcessingState, Vector3d> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          face -> Vector3d.atLowerCornerOf(face.getOpposite().getNormal()),
          facing -> facing.multiply(-1, -1, -1)
        );

        if (context.isSimulation())
            return ClickProcessingState.DEFAULT;

        context.setComplete();

        if (rayTraceHandle.left().isPresent())
            return rayTraceHandle.left().get();

        if (!rayTraceHandle.right().isPresent())
            throw new IllegalArgumentException("Missing both a click processing result as well as a center vector for sphere processing");

        context.setMetadata(ModMetadataKeys.ANCHOR.get(), rayTraceHandle.right().get());

        return context.getMutator().map(mutator -> {
            try (IBatchMutation ignored =
                   mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity)))
            {
                final Map<BlockState, Integer> resultingBitCount = Maps.newHashMap();

                final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                                                            .map(factory -> factory.apply(mutator))
                                                            .orElse((s) -> true);

                mutator.inWorldMutableStream()
                  .forEach(state -> {
                      if (!filter.test(state))
                          return;

                      final BlockState currentState = state.getState();
                      if (context.tryDamageItem()) {
                          resultingBitCount.putIfAbsent(currentState, 0);
                          resultingBitCount.computeIfPresent(currentState, (s, currentCount) -> currentCount + 1);

                          state.clear();
                      }
                  });

                resultingBitCount.forEach((blockState, count) -> BitInventoryUtils.insertIntoOrSpawn(
                  playerEntity,
                  blockState,
                  count
                ));

            }

            return new ClickProcessingState(true, Event.Result.ALLOW);
        }).orElse(ClickProcessingState.DEFAULT);
    }

    @Override
    public void onStoppedLeftClicking(final PlayerEntity playerEntity, final IChiselingContext context)
    {
        //Noop.
    }

    @SuppressWarnings("deprecation")
    @Override
    public ClickProcessingState onRightClickBy(final PlayerEntity playerEntity, final IChiselingContext context)
    {
        final Either<ClickProcessingState, Vector3d> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          face -> Vector3d.atLowerCornerOf(face.getNormal()),
          Function.identity()
        );

        if (context.isSimulation())
            return ClickProcessingState.DEFAULT;

        if (rayTraceHandle.left().isPresent())
            return rayTraceHandle.left().get();

        if (!rayTraceHandle.right().isPresent())
            throw new IllegalArgumentException("Missing both a click processing result as well as a center vector for sphere processing");

        context.setMetadata(ModMetadataKeys.ANCHOR.get(), rayTraceHandle.right().get());

        return context.getMutator().map(mutator -> {
            final BlockState heldBlockState = ItemStackUtils.getHeldBitBlockStateFromPlayer(playerEntity);
            if (heldBlockState.isAir(new SingleBlockBlockReader(heldBlockState), BlockPos.ZERO))
                return ClickProcessingState.DEFAULT;

            final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                                                        .map(factory -> factory.apply(mutator))
                                                        .orElse((s) -> true);
            final int missingBitCount = (int) mutator.stream()
                                                .filter(state -> state.getState().isAir(new SingleBlockBlockReader(state.getState()), BlockPos.ZERO) && filter.test(state))
                                                .count();

            final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(playerEntity);

            context.setComplete();
            if (playerBitInventory.canExtract(heldBlockState, missingBitCount) || playerEntity.isCreative()) {
                if (!playerEntity.isCreative())
                {
                    playerBitInventory.extract(heldBlockState, missingBitCount);
                }

                try (IBatchMutation ignored =
                       mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity)))
                {
                    mutator.inWorldMutableStream()
                      .filter(state -> state.getState().isAir(new SingleBlockBlockReader(state.getState()), BlockPos.ZERO) && filter.test(state))
                      .forEach(state -> state.overrideState(heldBlockState)); //We can use override state here to prevent the try-catch block.
                }
            }

            return new ClickProcessingState(true, Event.Result.ALLOW);
        }).orElse(ClickProcessingState.DEFAULT);
    }

    @Override
    public void onStoppedRightClicking(final PlayerEntity playerEntity, final IChiselingContext context)
    {
        //Noop.
    }

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context)
    {
        return context.getMutator().map(mutator -> mutator);
    }

    @Override
    public boolean isStillValid(final PlayerEntity playerEntity, final IChiselingContext context, final ChiselingOperation modeOfOperation)
    {
        final Optional<Vector3d> rayTraceHandle = this.processRayTraceIntoCenter(
          playerEntity,
          face -> Vector3d.atLowerCornerOf(face.getNormal()),
          Function.identity()
        );

        final Optional<Vector3d> contextAnchor = context.getMetadata(ModMetadataKeys.ANCHOR.get());
        return rayTraceHandle.map(d -> contextAnchor.filter(d::equals).isPresent()).orElseGet(() -> !contextAnchor.isPresent());
    }

    private Either<ClickProcessingState, Vector3d> processRayTraceIntoContext(
      final PlayerEntity playerEntity,
      final IChiselingContext context,
      final Function<Direction, Vector3d> placementFacingAdapter,
      final Function<Vector3d, Vector3d> fullFacingVectorAdapter
    ) {
        final RayTraceResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != RayTraceResult.Type.BLOCK || !(rayTraceResult instanceof BlockRayTraceResult))
        {
            return Either.left(ClickProcessingState.DEFAULT);
        }

        final BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) rayTraceResult;
        final Vector3d hitVector = blockRayTraceResult.getLocation().add(
          placementFacingAdapter.apply(blockRayTraceResult.getDirection())
            .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final Vector3d centeredHitVector = Vector3d.atLowerCornerOf(
          new BlockPos(
            hitVector.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide())
          )
        ).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());

        final Vector3d center = centeredHitVector.add(
          new Vector3d(
            (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
            (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
            (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide()
          ).multiply(
            fullFacingVectorAdapter.apply(Vector3d.atLowerCornerOf(blockRayTraceResult.getDirection().getNormal())
            )
          )
        );

        context.setStateFilter(areaAccessor -> {
            if (areaAccessor instanceof IWorldAreaAccessor)
                return new SphereAreaFilter(context.getModeOfOperandus(), ((IWorldAreaAccessor) areaAccessor).getInWorldStartPoint(), center);

            return new SphereAreaFilter(context.getModeOfOperandus(), Vector3d.ZERO, center);
        });

        BlockPosStreamProvider.getForRange(diameter)
          .forEach(bitPos -> {
              final Vector3d target = center
                .add(Vector3d.atLowerCornerOf(bitPos.subtract(new Vector3i(diameter / 2, diameter / 2, diameter / 2))).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));

              context.include(
                target
              );
          });

        return Either.right(center);
    }

    private Optional<Vector3d> processRayTraceIntoCenter(
      final PlayerEntity playerEntity,
      final Function<Direction, Vector3d> placementFacingAdapter,
      final Function<Vector3d, Vector3d> fullFacingVectorAdapter
    ) {
        final RayTraceResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != RayTraceResult.Type.BLOCK || !(rayTraceResult instanceof BlockRayTraceResult))
        {
            return Optional.empty();
        }

        final BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) rayTraceResult;
        final Vector3d hitVector = blockRayTraceResult.getLocation().add(
          placementFacingAdapter.apply(blockRayTraceResult.getDirection())
            .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final Vector3d centeredHitVector = Vector3d.atLowerCornerOf(
          new BlockPos(
            hitVector.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide())
          )
        ).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());

        final Vector3d center = centeredHitVector.add(
          new Vector3d(
            (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
            (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
            (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide()
          ).multiply(
            fullFacingVectorAdapter.apply(Vector3d.atLowerCornerOf(blockRayTraceResult.getDirection().getNormal())
            )
          )
        );

        return Optional.of(center);
    }

    @Override
    public @NotNull ResourceLocation getIcon()
    {
        return iconName;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return this.displayName;
    }

    @Override
    public ITextComponent getMultiLineDisplayName()
    {
        return this.multiLineDisplayName;
    }

    @NotNull
    @Override
    public Optional<IToolModeGroup> getGroup()
    {
        return Optional.of(
          ModChiselModeGroups.SPHERE
        );
    }

    private final class SphereAreaFilter implements Predicate<IStateEntryInfo> {

        private final ChiselingOperation modeOfOperandus;
        private final Vector3d startPoint;
        private final Vector3d center;

        private SphereAreaFilter(final ChiselingOperation modeOfOperandus, final Vector3d startPoint, final Vector3d center) {
            this.modeOfOperandus = modeOfOperandus;
            this.startPoint = startPoint;
            this.center = center;
        }

        @Override
        public boolean test(final IStateEntryInfo stateEntryInfo)
        {
            if (!(stateEntryInfo instanceof IInWorldStateEntryInfo))
                return false;

            final IInWorldStateEntryInfo inWorldStateEntryInfo = (IInWorldStateEntryInfo) stateEntryInfo;

            return inWorldStateEntryInfo.getInWorldStartPoint().distanceTo(center) <= (diameter / 2f / StateEntrySize.current().getBitsPerBlockSide()) &&
                     (!stateEntryInfo.getState().isAir() || modeOfOperandus.processesAir());
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof SphereAreaFilter))
            {
                return false;
            }

            final SphereAreaFilter that = (SphereAreaFilter) o;

            if (!Objects.equals(startPoint, that.startPoint))
            {
                return false;
            }
            return Objects.equals(center, that.center);
        }

        @Override
        public int hashCode()
        {
            int result = startPoint != null ? startPoint.hashCode() : 0;
            result = 31 * result + (center != null ? center.hashCode() : 0);
            return result;
        }
    }
}
