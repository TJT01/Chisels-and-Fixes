package mod.chiselsandbits.api.change.changes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Represents a single change that has been created with bits.
 */
public interface IChange extends INBTSerializable<CompoundNBT>
{
    /**
     * Checks if the change can still be undone.
     *
     * @param player The player for which the can undo check is performed.
     * @return True when the change can be undone.
     */
    boolean canUndo(final PlayerEntity player);

    /**
     * Checks if the change can still be redone.
     *
     * @param player The player for which the can redo check is performed.
     * @return True when the change can be redone.
     */
    boolean canRedo(final PlayerEntity player);

    /**
     * Undoes the change.
     * @param player The player for which undoes the change.
     * @throws IllegalChangeAttempt when the change can not be undone.
     */
    void undo(final PlayerEntity player) throws IllegalChangeAttempt;

    /**
     * Redoes the change
     * @param player The player for which redoes the change.
     * @throws IllegalChangeAttempt when the change can not be redone.
     */
    void redo(final PlayerEntity player) throws IllegalChangeAttempt;
}
