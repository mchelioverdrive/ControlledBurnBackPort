package com.ragex.mctools.event;

import com.ragex.tools.Tools;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockTick
{
    /* --------------------------------------------------------------------- */
    /* Action interface (Java 7 compatible)                                   */
    /* --------------------------------------------------------------------- */

    public interface IBlockTickAction
    {
        boolean run(BlockTickData data);
    }

    protected static final ArrayList<IBlockTickAction> actions = new ArrayList<IBlockTickAction>();

    public static void addAction(IBlockTickAction action)
    {
        if (actions.isEmpty()) MinecraftForge.EVENT_BUS.register(BlockTick.class);
        actions.add(action);
    }

    public static void removeAction(IBlockTickAction action)
    {
        actions.remove(action);
        if (actions.isEmpty()) MinecraftForge.EVENT_BUS.unregister(BlockTick.class);
    }

    /* --------------------------------------------------------------------- */
    /* World Tick                                                             */
    /* --------------------------------------------------------------------- */

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END) return;

        WorldServer world = (WorldServer) event.world;
        if (world.getWorldInfo().getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES) return;

        int tickSpeed = world.getGameRules().getGameRuleIntValue("randomTickSpeed");
        if (tickSpeed <= 0) return;

        world.theProfiler.startSection("FLib BlockTick");

        List loadedChunks = world.getChunkProvider().loadedChunks;
        for (Object obj : loadedChunks)
        {
            Chunk chunk = (Chunk) obj;

            ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
            for (ExtendedBlockStorage storage : storageArray)
            {
                if (storage == null || !storage.getNeedsRandomTick()) continue;

                for (int i = 0; i < tickSpeed; i++)
                {
                    int r = Tools.random(Integer.MAX_VALUE);
                    int x = r & 15;
                    int z = (r >> 8) & 15;
                    int y = (r >> 16) & 15;

                    int worldX = (chunk.xPosition << 4) + x;
                    int worldY = storage.getYLocation() + y;
                    int worldZ = (chunk.zPosition << 4) + z;

                    Block block = storage.getBlockByExtId(x, y, z);
                    if (block == null) continue;

                    int meta = storage.getExtBlockMetadata(x, y, z);

                    BlockTickData data = new BlockTickData(world, worldX, worldY, worldZ, block, meta);

                    for (IBlockTickAction action : actions)
                    {
                        action.run(data);
                    }
                }
            }
        }

        world.theProfiler.endSection();
    }

    /* --------------------------------------------------------------------- */
    /* Data Container                                                         */
    /* --------------------------------------------------------------------- */

    public static class BlockTickData
    {
        public final World world;
        public final int x, y, z;
        public final Block block;
        public final int meta;

        public BlockTickData(World world, int x, int y, int z, Block block, int meta)
        {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
        }
    }
}
