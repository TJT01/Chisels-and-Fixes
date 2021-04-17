package mod.chiselsandbits.api.item.chisel;

import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.item.click.ILeftClickControllingItem;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;

public interface IChiselingItem extends ILeftClickControllingItem, IWithModeItem<IChiselMode>
{
}
