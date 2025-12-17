package com.ragex.mctools.event;

import com.ragex.tools.Tools;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

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
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END) return;
        if (!(event.world instanceof WorldServer)) return;

        WorldServer world = (WorldServer) event.world;

        // 1.7.10: get game rule as string then parse (getGameRuleStringValue exists in 1.7.10)
        String tickRule = world.getGameRules().getGameRuleStringValue("randomTickSpeed");
        int tickSpeed = 3; // sane default
        try {
            tickSpeed = Integer.parseInt(tickRule);
        } catch (NumberFormatException ignored) {}

        if (tickSpeed <= 0) return;

        world.theProfiler.startSection("FLib BlockTick");

        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof ChunkProviderServer) {
            ChunkProviderServer cps = (ChunkProviderServer) provider;

            List<?> loadedChunks = null;

            // Try direct field access first (common in many 1.7.10 mappings)
            try {
                Field f = ChunkProviderServer.class.getDeclaredField("loadedChunks");
                f.setAccessible(true);
                Object val = f.get(cps);
                if (val instanceof List) {
                    loadedChunks = (List<?>) val;
                }
            } catch (Throwable t) {
                // ignore and try other fallbacks
            }

            // Fallback: id2ChunkMap (some mappings / versions expose a map of chunks)
            if (loadedChunks == null) {
                try {
                    Field f = ChunkProviderServer.class.getDeclaredField("id2ChunkMap");
                    f.setAccessible(true);
                    Object mapObj = f.get(cps);
                    if (mapObj instanceof Map) {
                        loadedChunks = new ArrayList<>(((Map<?, ?>) mapObj).values());
                    } else if (mapObj != null) {
                        // handle fastutil Long2ObjectMap or similar via reflection
                        Method valuesMethod = mapObj.getClass().getMethod("values");
                        Object values = valuesMethod.invoke(mapObj);
                        if (values instanceof Collection) {
                            loadedChunks = new ArrayList<>((Collection<?>) values);
                        }
                    }
                } catch (Throwable t) {
                    loadedChunks = Collections.emptyList();
                }
            }

            for (Object obj : loadedChunks) {
                if (!(obj instanceof Chunk)) continue;
                Chunk chunk = (Chunk) obj;

                ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
                if (storageArray == null) continue;

                for (ExtendedBlockStorage storage : storageArray) {
                    if (storage == null || !storage.getNeedsRandomTick()) continue;

                    for (int i = 0; i < tickSpeed; i++) {
                        int r = Tools.random(Integer.MAX_VALUE); // your existing Tools.random
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
                        for (IBlockTickAction action : actions) {
                            action.run(data);
                        }
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
