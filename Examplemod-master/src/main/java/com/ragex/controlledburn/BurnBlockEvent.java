package com.ragex.controlledburn;

import cpw.mods.fml.common.eventhandler.Cancelable;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

@Cancelable
public class BurnBlockEvent extends BlockEvent
{
    public final Block block;
    public final int meta;

    public BurnBlockEvent(World world, int x, int y, int z, Block block, int meta)
    {
        super(world, x, y, z, block, meta);
        this.block = block;
        this.meta = meta;
    }
}
