package com.ragex.mctools;

//import com.ragex.fantasticlib.Compat;
//import com.ragex.fantasticlib.config.FantasticConfig;
import com.ragex.tools.ReflectionTool;
import com.ragex.tools.Tools;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ImprovedRayTracing
{
    /* ====================================================================== */
    /* Filters                                                                 */
    /* ====================================================================== */

    public interface IBlockFilter
    {
        boolean test(Block block, int meta);
    }

    public static final ArrayList<IBlockFilter> BLOCK_ENDS_RAYTRACE_FILTERS = new ArrayList<IBlockFilter>();

    protected static final HashSet<Block> transparentBlocks = new HashSet<Block>();
    protected static final HashSet<Block> nonTransparentBlocks = new HashSet<Block>();
    protected static final HashSet<Material> transparentMaterials = new HashSet<Material>();
    protected static final HashSet<Material> nonTransparentMaterials = new HashSet<Material>();
    protected static final HashSet<Class<? extends Block>> transparentBlockSuperclasses = new HashSet<Class<? extends Block>>();
    protected static final HashSet<Class<? extends Block>> nonTransparentBlockSuperclasses = new HashSet<Class<? extends Block>>();
    protected static final HashSet<Class<? extends Block>> ignoredBlockClasses = new HashSet<Class<? extends Block>>();

    protected static final int ITERATION_WARNING_THRESHOLD = 200;
    protected static long lastWarning = -1;
    protected static int errorCount = 0;

    /* ====================================================================== */
    /* Visibility Logic                                                        */
    /* ====================================================================== */

    public static boolean canSeeThrough(World world, int x, int y, int z)
    {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        for (IBlockFilter filter : BLOCK_ENDS_RAYTRACE_FILTERS)
        {
            if (!filter.test(block, meta)) return false;
        }

        if (transparentBlocks.contains(block)) return true;
        if (nonTransparentBlocks.contains(block)) return false;

        Material material = block.getMaterial();
        if (transparentMaterials.contains(material)) return true;
        if (nonTransparentMaterials.contains(material)) return false;

        Class<? extends Block> cls = block.getClass();
        if (!ignoredBlockClasses.contains(cls))
        {
            for (Class<? extends Block> superCls : transparentBlockSuperclasses)
            {
                if (superCls.isAssignableFrom(cls)) return true;
            }

            for (Class<? extends Block> superCls : nonTransparentBlockSuperclasses)
            {
                if (superCls.isAssignableFrom(cls)) return false;
            }

            ignoredBlockClasses.add(cls);
        }

        if (material == Material.air) return true;
        if (material == Material.leaves) return true;
        if (material == Material.glass) return true;
        if (material == Material.ice) return true;
        if (material == Material.water) return true;
        if (material == Material.fire) return true;
        //if (material == Material.portal) return !Compat.betterportals;

        if (block instanceof BlockFence) return true;
        if (block instanceof BlockFenceGate) return true;
        if (block instanceof BlockTrapDoor) return true;
        //if (block instanceof BlockSlime) return true;

        if (block == Blocks.wooden_door || block == Blocks.iron_door)
        {
            return (meta & 8) != 0;
        }

        return false;
    }

    /* ====================================================================== */
    /* Raytrace Core                                                           */
    /* ====================================================================== */

    public static MovingObjectPosition rayTraceBlocks(
            World world,
            Vec3 start,
            Vec3 end,
            int maxBlocks,
            boolean physicsCheck,
            boolean checkFluids)
    {
        world.theProfiler.startSection("ImprovedRayTrace");

        int x = MathHelper.floor_double(start.xCoord);
        int y = MathHelper.floor_double(start.yCoord);
        int z = MathHelper.floor_double(start.zCoord);

        int endX = MathHelper.floor_double(end.xCoord);
        int endY = MathHelper.floor_double(end.yCoord);
        int endZ = MathHelper.floor_double(end.zCoord);

        for (int i = 0; i < maxBlocks; i++)
        {
            if (!world.blockExists(x, y, z))
            {
                world.theProfiler.endSection();
                return new MovingObjectPosition(x, y, z, -1, start);
            }

            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);

            if (block != Blocks.air)
            {
                if (physicsCheck || !canSeeThrough(world, x, y, z))
                {
                    AxisAlignedBB bb = block.getCollisionBoundingBoxFromPool(world, x, y, z);
                    if (bb != null)
                    {
                        MovingObjectPosition hit = bb.calculateIntercept(start, end);
                        if (hit != null)
                        {
                            world.theProfiler.endSection();
                            return hit;
                        }
                    }
                }

                if (checkFluids && (block instanceof BlockLiquid || block instanceof BlockFluidBase))
                {
                    AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
                    MovingObjectPosition hit = bb.calculateIntercept(start, end);
                    if (hit != null)
                    {
                        world.theProfiler.endSection();
                        return hit;
                    }
                }
            }

            if (x == endX && y == endY && z == endZ)
            {
                world.theProfiler.endSection();
                return null;
            }

            double dx = end.xCoord - start.xCoord;
            double dy = end.yCoord - start.yCoord;
            double dz = end.zCoord - start.zCoord;

            double tx = dx == 0 ? Double.MAX_VALUE : ((dx > 0 ? x + 1 : x) - start.xCoord) / dx;
            double ty = dy == 0 ? Double.MAX_VALUE : ((dy > 0 ? y + 1 : y) - start.yCoord) / dy;
            double tz = dz == 0 ? Double.MAX_VALUE : ((dz > 0 ? z + 1 : z) - start.zCoord) / dz;

            if (tx < ty && tx < tz) x += dx > 0 ? 1 : -1;
            else if (ty < tz) y += dy > 0 ? 1 : -1;
            else z += dz > 0 ? 1 : -1;
        }

        if (maxBlocks >= ITERATION_WARNING_THRESHOLD)
        {
            if (lastWarning == -1 || System.currentTimeMillis() - lastWarning > 300000)
            {
                System.err.println("WARNING: Raytrace exceeded iteration limit!");
                Tools.printStackTrace();
                lastWarning = System.currentTimeMillis();
            }
            else errorCount++;
        }

        world.theProfiler.endSection();
        return null;
    }

    /* ====================================================================== */
    /* Convenience Wrappers                                                    */
    /* ====================================================================== */

    public static boolean isUnobstructed(Entity entity, double distance, boolean physics)
    {
        Vec3 eyes = Vec3.createVectorHelper(
                entity.posX,
                entity.posY + entity.getEyeHeight(),
                entity.posZ
        );
        Vec3 end = eyes.addVector(
                entity.getLookVec().xCoord * distance,
                entity.getLookVec().yCoord * distance,
                entity.getLookVec().zCoord * distance
        );

        return rayTraceBlocks(entity.worldObj, eyes, end, ITERATION_WARNING_THRESHOLD, physics, false) == null;
    }
}
