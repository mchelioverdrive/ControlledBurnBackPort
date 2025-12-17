package com.ragex.controlledburn;

import com.ragex.mctools.event.BlockTick;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

import static com.ragex.controlledburn.FireConfig.*;

public class FireData
{
    public static int replaceBlockWithFireChanceRange;

    public static LinkedHashMap<FireDataFilter, BlockMeta> blockTransformationMap = new LinkedHashMap<>();
    public static LinkedHashMap<FireDataFilter, Boolean> fireSourceBlocks = new LinkedHashMap<>();
    public static LinkedHashSet<FireDataFilter> blockSpreadsFire = new LinkedHashSet<>();


    /* --------------------------------------------------------------------- */
    /* Update                                                                */
    /* --------------------------------------------------------------------- */

    public static void update()
    {
        replaceBlockWithFireChanceRange =
                burnSpreadChances.maxBurnSpreadChance - burnSpreadChances.minBurnSpreadChance;

        HashSet<Block> blocks;
        String token;
        int flammability = 0, encouragement = 0;
        boolean sameFlammability, sameEncouragement, good;
        BiomeGenBase biome;
        FireDataFilter filter;

        /* ---------------- block-specific fire data ---------------- */

        for (String string : blockSettings)
        {
            String[] tokens = string.split(",");
            if (tokens.length != 3)
            {
                System.err.println("Wrong number of arguments for block-specific setting");
                continue;
            }

            blocks = blocksMatching(tokens[0].trim());
            if (blocks.isEmpty())
            {
                System.err.println("Block(s) not found: " + tokens[0]);
                continue;
            }

            sameFlammability = tokens[1].trim().equals("=");
            sameEncouragement = tokens[2].trim().equals("=");

            if (!sameFlammability) flammability = Integer.parseInt(tokens[1].trim());
            if (!sameEncouragement) encouragement = Integer.parseInt(tokens[2].trim());

            for (Block b : blocks)
            {
                Blocks.fire.setFireInfo(
                        b,
                        sameEncouragement ? ControlledBurn.OLD_FIRE.getEncouragement(b) : encouragement,
                        sameFlammability ? ControlledBurn.OLD_FIRE.getFlammability(b) : flammability
                );
            }
        }

        /* ---------------- block transformations ---------------- */

        blockTransformationMap.clear();
        for (String s : blockTransformations)
        {
            String[] tokens = s.split(",");
            if (tokens.length < 2)
            {
                System.err.println("Not enough arguments for transformation entry: " + s);
                continue;
            }

            ArrayList<BlockMeta> fromStates = blockMetasMatching(tokens[0].trim());
            ArrayList<BlockMeta> toStates = blockMetasMatching(tokens[1].trim(), true);

            if (fromStates.isEmpty() || toStates.isEmpty())
            {
                System.err.println("Invalid transformation entry: " + s);
                continue;
            }

            filter = new FireDataFilter();
            good = true;

            for (int i = 2; i < tokens.length; i++)
            {
                token = tokens[i].trim();
                try
                {
                    filter.dimensions.add(Integer.parseInt(token));
                }
                catch (NumberFormatException e)
                {
                    //biome = (BiomeGenBase) BiomeGenBase.biomeRegistry.getObject(token);
                    //if (biome != null) filter.biomes.add(biome);
                    //else
                    //{
                    //    System.err.println("Bad dimension or biome: " + token);
                    //    good = false;
                    //    break;
                    //}
                }
            }
            if (!good) continue;

            filter.blockMetas.addAll(fromStates);
            blockTransformationMap.put(filter, toStates.get(0));
        }

        /* ---------------- fire source blocks ---------------- */

        fireSourceBlocks.clear();
        for (String s : FireConfig.fireSourceBlocks)
        {
            String[] tokens = s.split(",");
            if (tokens.length < 2)
            {
                System.err.println("Invalid fire source entry: " + s);
                continue;
            }

            ArrayList<BlockMeta> fromStates = blockMetasMatching(tokens[0].trim());
            if (fromStates.isEmpty())
            {
                System.err.println("Invalid fire source blocks: " + s);
                continue;
            }

            filter = new FireDataFilter();
            good = true;

            for (int i = 2; i < tokens.length; i++)
            {
                token = tokens[i].trim();
                try
                {
                    filter.dimensions.add(Integer.parseInt(token));
                }
                catch (NumberFormatException e)
                {
                    //biomegenbase is not present in 1.7.10 forge
                    //biome = (BiomeGenBase) BiomeGenBase.biomeRegistry.getObject(token);
                    //if (biome != null) filter.biomes.add(biome);
                    //else
                    //{
                    //    System.err.println("Bad dimension or biome: " + token);
                    //    good = false;
                    //    break;
                    //}
                }
            }
            if (!good) continue;

            filter.blockMetas.addAll(fromStates);
            fireSourceBlocks.put(filter, Boolean.parseBoolean(tokens[1].trim()));
        }

        /* ---------------- blocks that spread fire ---------------- */

        blockSpreadsFire.clear();
        for (String s : FireConfig.blockSpreadsFire)
        {
            String[] tokens = s.split(",");
            ArrayList<BlockMeta> fromStates = blockMetasMatching(tokens[0].trim());
            if (fromStates.isEmpty())
            {
                System.err.println("Invalid fire spreading block: " + s);
                continue;
            }

            filter = new FireDataFilter();
            good = true;

            for (int i = 1; i < tokens.length; i++)
            {
                token = tokens[i].trim();
                try
                {
                    filter.dimensions.add(Integer.parseInt(token));
                }
                catch (NumberFormatException e)
                {
                    //biome = (BiomeGenBase) BiomeGenBase.biomeRegistry.getObject(token);
                    //if (biome != null) filter.biomes.add(biome);
                    //else
                    //{
                    //    System.err.println("Bad dimension or biome: " + token);
                    //    good = false;
                    //    break;
                    //}
                }
            }
            if (!good) continue;

            filter.blockMetas.addAll(fromStates);
            blockSpreadsFire.add(filter);
        }

        if (!blockSpreadsFire.isEmpty()) BlockTick.addAction(SpreadFireLikeLava.ACTION);
        else BlockTick.removeAction(SpreadFireLikeLava.ACTION);
    }

    /* --------------------------------------------------------------------- */
    /* Matching helpers                                                       */
    /* --------------------------------------------------------------------- */

    protected static HashSet<Block> blocksMatching(String id)
    {
        HashSet<Block> result = new HashSet<>();

        Block block = (Block) Block.blockRegistry.getObject(id);
        if (block != null && block != Blocks.air) result.add(block);
        else if (id.startsWith("ore:") || id.startsWith("oredict:"))
        {
            String ore = id.replace("ore:", "").replace("oredict:", "");
            for (ItemStack stack : OreDictionary.getOres(ore))
            {
                block = Block.getBlockFromItem(stack.getItem());
                if (block != null && block != Blocks.air) result.add(block);
            }
        }

        return result;
    }

    protected static ArrayList<BlockMeta> blockMetasMatching(String id)
    {
        return blockMetasMatching(id, false);
    }

    protected static ArrayList<BlockMeta> blockMetasMatching(String id, boolean allowAir)
    {
        ArrayList<BlockMeta> result = new ArrayList<>();

        String[] tokens = id.split(":");
        String domain = "minecraft", name, meta = "*";

        if (tokens.length == 1) name = tokens[0];
        else if (tokens.length == 2)
        {
            domain = tokens[0];
            name = tokens[1];
        }
        else
        {
            domain = tokens[0];
            name = tokens[1];
            meta = tokens[2];
        }

        HashSet<Block> blocks;
        if (domain.equals("ore") || domain.equals("oredict")) blocks = blocksMatching(domain + ":" + name);
        else
        {
            Block block = (Block) Block.blockRegistry.getObject(new ResourceLocation(domain, name));
            if (block == null || (!allowAir && block == Blocks.air)) return result;
            blocks = new HashSet<>();
            blocks.add(block);
        }

        for (Block block : blocks)
        {
            if (meta.equals("*"))
            {
                for (int i = 0; i < 16; i++) result.add(new BlockMeta(block, i));
            }
            else
            {
                try
                {
                    result.add(new BlockMeta(block, Integer.parseInt(meta)));
                }
                catch (NumberFormatException ignored) {}
            }
        }

        return result;
    }

    /* --------------------------------------------------------------------- */
    /* Filter                                                                 */
    /* --------------------------------------------------------------------- */

    public static class FireDataFilter
    {
        public ArrayList<Integer> dimensions = new ArrayList<>();
        public ArrayList<BiomeGenBase> biomes = new ArrayList<>();
        public ArrayList<BlockMeta> blockMetas = new ArrayList<>();

        public boolean matches(World world, int x, int y, int z, Block block, int meta)
        {
            if (!blockMetas.contains(new BlockMeta(block, meta))) return false;
            if (!dimensions.isEmpty() && !dimensions.contains(world.provider.dimensionId)) return false;
            if (!biomes.isEmpty() && !biomes.contains(world.getBiomeGenForCoords(x, z))) return false;
            return true;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(dimensions, biomes, blockMetas);
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof FireDataFilter)) return false;
            FireDataFilter f = (FireDataFilter) o;
            return dimensions.equals(f.dimensions)
                    && biomes.equals(f.biomes)
                    && blockMetas.equals(f.blockMetas);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Block + meta wrapper                                                   */
    /* --------------------------------------------------------------------- */

    public static class BlockMeta
    {
        public final Block block;
        public final int meta;

        public BlockMeta(Block block, int meta)
        {
            this.block = block;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof BlockMeta)) return false;
            BlockMeta bm = (BlockMeta) o;
            return bm.block == block && bm.meta == meta;
        }

        @Override
        public int hashCode()
        {
            return block.hashCode() * 31 + meta;
        }
    }
}
