package com.ragex.controlledburn;

import com.ragex.tools.ReflectionTool;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import static com.ragex.controlledburn.FireConfig.specialToggles;

@Mod(
        modid = ControlledBurn.MODID,
        name = ControlledBurn.NAME,
        version = ControlledBurn.VERSION,
        dependencies = "required-after:fantasticlib"
        //pain
)
public class ControlledBurn
{
    public static final String MODID = "controlledburn";
    public static final String NAME = "Controlled Burn";
    public static final String VERSION = "1.7.10.024";

    public static int replaceBlockWithFireChanceRange;

    public static final BlockFire OLD_FIRE = (BlockFire) Blocks.fire;

    public ControlledBurn()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ------------------------------------------------------------------------- */
    /* Fire age helpers (metadata-based in 1.7.10)                                */
    /* ------------------------------------------------------------------------- */

    public static int minFireAge()
    {
        return 0;
    }

    public static int maxFireAge()
    {
        return 15;
    }

    public static int fireAgeRange()
    {
        return maxFireAge() - minFireAge();
    }

    /* ------------------------------------------------------------------------- */
    /* Events                                                                     */
    /* ------------------------------------------------------------------------- */

    @SubscribeEvent
    public void fluidPlacingBlock(BlockEvent.FluidPlaceBlockEvent event)
    {
        if (specialToggles.noLavaFire &&
                event.newBlock instanceof BlockFireEdit)
        {
            event.newBlock = event.block;
            event.newMetadata = event.blockMetadata;
        }
    }

    /* ------------------------------------------------------------------------- */
    /* Init                                                                       */
    /* ------------------------------------------------------------------------- */

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        replaceVanillaFire();
        FireData.update();
    }

    /* ------------------------------------------------------------------------- */
    /* Fire replacement                                                           */
    /* ------------------------------------------------------------------------- */

    private static void replaceVanillaFire()
    {
        BlockFireEdit newFire =
                (BlockFireEdit) new BlockFireEdit()
                        .setHardness(0.0F)
                        .setLightLevel(1.0F)
                        .setBlockName("fire");

        /* Replace Blocks.fire */
        ReflectionTool.set(
                Blocks.class,
                new String[]{"field_150480_ab", "fire"},
                null,
                newFire
        );

        /* Copy vanilla fire stats */
        for (Object o : Block.blockRegistry)
        {
            Block b = (Block) o;

            if (b != Blocks.air && OLD_FIRE.getEncouragement(b) > 0)
            {
                Blocks.fire.setFireInfo(
                        b,
                        OLD_FIRE.getEncouragement(b),
                        OLD_FIRE.getFlammability(b)
                );
            }
        }
    }
}
