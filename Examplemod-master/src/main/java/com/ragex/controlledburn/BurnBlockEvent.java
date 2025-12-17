package com.ragex.controlledburn;

import cpw.mods.fml.common.eventhandler.Cancelable;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

@Cancelable
public class BurnBlockEvent extends BlockEvent
{
    public BurnBlockEvent(World world, int x, int y, int z, Block block, int meta) {
        // Note the argument order: x, y, z, world, block, meta
        super(x, y, z, world, block, meta);
    }
}
