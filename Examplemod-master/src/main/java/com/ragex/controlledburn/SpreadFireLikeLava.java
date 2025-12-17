package com.ragex.controlledburn;

import com.ragex.mctools.event.BlockTick;
import com.ragex.tools.Tools;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class SpreadFireLikeLava
{
    public static final BlockTick.IBlockTickAction ACTION = new BlockTick.IBlockTickAction()
    {
        @Override
        public boolean run(BlockTick.BlockTickData event)
        {
            World world = event.world;
            if (!world.getGameRules().getGameRuleBooleanValue("doFireTick")) return false;

            int x = event.x;
            int y = event.y;
            int z = event.z;

            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);

            boolean matches = false;
            for (FireData.FireDataFilter filter : FireData.blockSpreadsFire)
            {
                if (filter.matches(world, x, y, z, block, meta))
                {
                    matches = true;
                    break;
                }
            }
            if (!matches) return false;

            int flareHeight = Tools.random(3);

            if (flareHeight > 0)
            {
                int cx = x;
                int cy = y;
                int cz = z;

                for (int i = 0; i < flareHeight; i++)
                {
                    cx += Tools.random(3) - 1;
                    cy += 1;
                    cz += Tools.random(3) - 1;

                    if (cy >= world.getHeight() || !world.blockExists(cx, cy, cz)) return false;

                    Block b = world.getBlock(cx, cy, cz);

                    if (world.isAirBlock(cx, cy, cz))
                    {
                        if (isSurroundingBlockFlammable(world, cx, cy, cz))
                        {
                            world.setBlock(cx, cy, cz, Blocks.fire);
                            return true;
                        }
                    }
                    else if (b.getMaterial().blocksMovement())
                    {
                        return false;
                    }
                }
                return false;
            }
            else
            {
                boolean result = false;

                for (int i = 0; i < 3; i++)
                {
                    int cx = x + Tools.random(3) - 1;
                    int cz = z + Tools.random(3) - 1;

                    if (y >= world.getHeight() || !world.blockExists(cx, y, cz)) return false;

                    if (world.isAirBlock(cx, y + 1, cz)
                            && isSurroundingBlockFlammable(world, cx, y, cz))
                    {
                        world.setBlock(cx, y + 1, cz, Blocks.fire);
                        result = true;
                    }
                }
                return result;
            }
        }
    };

    /* --------------------------------------------------------------------- */
    /* Helpers                                                               */
    /* --------------------------------------------------------------------- */

    protected static boolean isSurroundingBlockFlammable(World world, int x, int y, int z)
    {
        for (int side = 0; side < 6; side++)
        {
            int nx = x + net.minecraft.util.Facing.offsetsXForSide[side];
            int ny = y + net.minecraft.util.Facing.offsetsYForSide[side];
            int nz = z + net.minecraft.util.Facing.offsetsZForSide[side];

            if (!world.blockExists(nx, ny, nz)) continue;

            Block block = world.getBlock(nx, ny, nz);
            if (Blocks.fire.getEncouragement(block) > 0) return true;
        }
        return false;
    }
}
