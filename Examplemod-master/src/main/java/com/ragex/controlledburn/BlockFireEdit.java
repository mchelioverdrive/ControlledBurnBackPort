package com.ragex.controlledburn;

import com.ragex.mctools.ImprovedRayTracing;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockTNT;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Map;
import java.util.Random;

import static com.ragex.controlledburn.FireConfig.*;

public class BlockFireEdit extends BlockFire
{
    /* ---------------------------------- helpers ---------------------------------- */

    private static boolean tryBurnBlockSpecial(World world, int x, int y, int z, Block block, int meta)
    {
        Block blockTo = null;
        int metaTo = 0;

        for (Map.Entry<FireData.FireDataFilter, FireData.BlockMeta> entry : FireData.blockTransformationMap.entrySet())
        {
            FireData.FireDataFilter filter = entry.getKey();
            FireData.BlockMeta result = entry.getValue();

            if (filter.matches(world, x, y, z, block, meta))
            {
                blockTo = result.block;   // access the public field directly
                metaTo = result.meta;     // access the public field directly
                break; // only the first matching entry is applied
            }
        }

        if (blockTo != null)
        {
            world.setBlock(x, y, z, blockTo, metaTo, 3);
            return true;
        }

        return false;
    }





    @Override
    public int tickRate(World world)
    {
        return globalMultipliers.tickDelay;
    }

    /* ---------------------------------- tick ---------------------------------- */

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand)
    {
        if (!world.getGameRules().getGameRuleBooleanValue("doFireTick")) return;
        if (!world.blockExists(x, y, z)) return;

        if (!canPlaceBlockAt(world, x, y, z))
        {
            world.setBlockToAir(x, y, z);
            return;
        }

        int age = world.getBlockMetadata(x, y, z);
        Block below = world.getBlock(x, y - 1, z);

        boolean fireSourceBelow =
                below.isFireSource(world, x, y - 1, z, ForgeDirection.UP) ||
                        getFlammability(below) < 0;

        /* rain extinguish */
        if (!fireSourceBelow && !specialToggles.ignoreRain &&
                world.isRaining() &&
                rand.nextFloat() < 0.2F + age * 0.03F)
        {
            world.setBlockToAir(x, y, z);
            return;
        }

        /* age increase */
        if (age < ControlledBurn.maxFireAge() && rand.nextInt(3) == 0)
        {
            world.setBlockMetadataWithNotify(x, y, z, age + 1, 4);
            age++;
        }

        /* natural extinguish */
        if (!fireSourceBelow && !canNeighborCatchFire(world, x, y, z))
        {
            if (!world.isSideSolid(x, y - 1, z, ForgeDirection.UP) || age > 3)
            {
                world.setBlockToAir(x, y, z);
                return;
            }
        }

        boolean humid =
                world.getBiomeGenForCoords(x, z).isHighHumidity() &&
                        !specialToggles.ignoreHumidBiomes;

        int humidMod = humid ? -50 : 0;

        /* burn adjacent */
        if (globalMultipliers.burnSpeedMultiplier != 0)
        {
            tryBurnAdjacent(world, x, y - 1, z, 250 + humidMod, rand, age, ForgeDirection.UP);
            tryBurnAdjacent(world, x, y + 1, z, 250 + humidMod, rand, age, ForgeDirection.DOWN);
            tryBurnAdjacent(world, x + 1, y, z, 300 + humidMod, rand, age, ForgeDirection.WEST);
            tryBurnAdjacent(world, x - 1, y, z, 300 + humidMod, rand, age, ForgeDirection.EAST);
            tryBurnAdjacent(world, x, y, z + 1, 300 + humidMod, rand, age, ForgeDirection.NORTH);
            tryBurnAdjacent(world, x, y, z - 1, 300 + humidMod, rand, age, ForgeDirection.SOUTH);
        }

        world.scheduleBlockUpdate(
                x, y, z,
                this,
                tickRate(world) + rand.nextInt(globalMultipliers.tickDelayRandomization + 1)
        );
    }

    /* ---------------------------------- placement ---------------------------------- */

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z)
    {
        return (world.isSideSolid(x, y - 1, z, ForgeDirection.UP) ||
                canNeighborCatchFire(world, x, y, z))
                && (!specialToggles.noLightningFire || !callerNameContains("Lightning"));
    }

    private boolean callerNameContains(String s)
    {
        s = s.toLowerCase();
        for (StackTraceElement e : Thread.currentThread().getStackTrace())
        {
            if (e.getClassName().toLowerCase().contains(s)) return true;
        }
        return false;
    }

    /* ---------------------------------- burn logic ---------------------------------- */

    private void tryBurnAdjacent(World world, int x, int y, int z,
                                 int chance, Random rand, int age, ForgeDirection face)
    {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        if (rand.nextInt(chance) < block.getFlammability(world, x, y, z, face) &&
                !MinecraftForge.EVENT_BUS.post(new BurnBlockEvent(world, x, y, z, block, meta)))
        {
            if (!tryBurnBlockSpecial(world, x, y, z, block, meta))
            {
                world.setBlockToAir(x, y, z);
            }

            if (block == Blocks.tnt)
            {
                world.setBlockToAir(x, y, z);
                world.createExplosion(null, x + 0.5, y + 0.5, z + 0.5, 4.0F, true);
            }
        }
    }

    /* ---------------------------------- neighbors ---------------------------------- */

    private boolean canNeighborCatchFire(World world, int x, int y, int z)
    {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
        {
            if (canCatchFire(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, dir.getOpposite()))
                return true;
        }
        return false;
    }

    /* ---------------------------------- client ---------------------------------- */

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random rand)
    {
        if (rand.nextInt(24) == 0)
        {
            world.playSound(
                    x + 0.5, y + 0.5, z + 0.5,
                    "fire.fire",
                    1.0F + rand.nextFloat(),
                    0.3F + rand.nextFloat() * 0.7F,
                    false
            );
        }

        world.spawnParticle(
                "largesmoke",
                x + rand.nextDouble(),
                y + rand.nextDouble(),
                z + rand.nextDouble(),
                0, 0, 0
        );
    }
}

