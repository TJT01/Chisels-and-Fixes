package mod.chiselsandbits.api.measuring;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.UUID;

/**
 * A manager for dealing with measurements made by different players in different worlds.
 */
public interface IMeasuringManager
{
    static IMeasuringManager getInstance() {
        return IChiselsAndBitsAPI.getInstance().getMeasuringManager();
    }

    /**
     * Determines the active measurements in a given world.
     *
     * @param world The world in question.
     * @return A collection of measurements which are active in the given world.
     */
    default Collection<? extends IMeasurement> getInWorld(final World world) {
        return getInWorld(world.dimension().location());
    }

    /**
     * Determines the active measurements in a given world which is identified by the given world key.
     *
     * @param worldKey The world key in question.
     * @return A collection of measurements which are active in the given world.
     */
    Collection<? extends IMeasurement> getInWorld(final ResourceLocation worldKey);

    /**
     * Determines the active measurements for a given player.
     *
     * @param playerEntity The player in question.
     * @return A collection of measurements which are active for the given player.
     */
    default Collection<? extends IMeasurement> getForPlayer(final PlayerEntity playerEntity) {
        return getForPlayer(playerEntity.getUUID());
    }

    /**
     * Determines the active measurements for a given player represented by his unique UUID.
     *
     * @param playerId The if of the player in question.
     * @return A collection of measurements which are active for the given player.
     */
    Collection<? extends IMeasurement> getForPlayer(final UUID playerId);

    /**
     * Creates a new measurement for the given player in the given world.
     *
     * @param world The world to create the measurement in.
     * @param playerEntity The player to create the given measurement for.
     * @param from The start-point of the measurement.
     * @param to The end-point of the measurement.
     * @param mode The measurement mode.
     * @return The newly created and processed measurement.
     */
    IMeasurement create(
      final World world,
      final PlayerEntity playerEntity,
      final Vector3d from,
      final Vector3d to,
      final MeasuringMode mode
    );

    /**
     * Resets all measurements for a given player. (Removing them from the game and memory).
     *
     * @param playerEntity The player to remove the measurements for.
     */
    default void resetMeasurementsFor(PlayerEntity playerEntity) {
        resetMeasurementsFor(playerEntity.getUUID());
    }

    /**
     * Resets all measurements for a given player represented by the given id. (Removing them from the game and memory).
     *
     * @param playerId The id of the player to remove the measurements for.
     */
    void resetMeasurementsFor(UUID playerId);
}

