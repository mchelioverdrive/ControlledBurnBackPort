package com.ragex.controlledburn;

import com.ragex.tools.ReflectionTool;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;

import java.io.File;

import static com.ragex.controlledburn.FireConfig.specialToggles;

@Mod(
        modid = ControlledBurn.MODID,
        name = ControlledBurn.NAME,
        version = ControlledBurn.VERSION
        //dependencies = "required-after:fantasticlib"
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

    public static Configuration config;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // initialize FireConfig using Forge's suggested config file
        FireConfig.init(event.getSuggestedConfigurationFile());
        replaceVanillaFire();
    }


    /* ------------------------------------------------------------------------- */
    /* Events                                                                     */
    /* ------------------------------------------------------------------------- */

    //@SubscribeEvent
    //public void fluidPlacingBlock(BlockEvent.FluidPlaceBlockEvent event)
    //{
    //    if (specialToggles.noLavaFire &&
    //            event.newBlock instanceof BlockFireEdit)
    //    {
    //        event.newBlock = event.block;
    //        event.newMetadata = event.blockMetadata;
    //    }
    //}
    //todo the thing

    /* ------------------------------------------------------------------------- */
    /* Init                                                                       */
    /* ------------------------------------------------------------------------- */

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        //replaceVanillaFire();
        FireData.update();
    }

    /* ------------------------------------------------------------------------- */
    /* Fire replacement                                                           */
    /* ------------------------------------------------------------------------- */

    private static void replaceVanillaFire()
    {
        try {
            BlockFire oldFire = (BlockFire) Blocks.fire;

            BlockFireEdit newFire = (BlockFireEdit) new BlockFireEdit()
                    .setHardness(0.0F)
                    .setLightLevel(1.0F)
                    .setBlockName("fire");

            // 1) replace the static field in Blocks.class (your ReflectionTool)
            ReflectionTool.set(
                    Blocks.class,
                    new String[]{"field_150480_ab", "fire"},
                    null,
                    newFire
            );

            // 2) replace blocksList entry so getBlockById(...) returns newFire
            try {
                int id = Block.getIdFromBlock(oldFire);
                if (Block.blocksList != null && id >= 0 && id < Block.blocksList.length) {
                    Block.blocksList[id] = newFire;
                }
            } catch (Throwable t) {
                System.err.println("ControlledBurn: failed to update Block.blocksList: " + t);
                t.printStackTrace();
            }

            // 3) copy vanilla fire stats from oldFire -> newFire
            for (Object o : Block.blockRegistry) {
                Block b = (Block) o;
                if (b != Blocks.air && oldFire.getEncouragement(b) > 0) {
                    Blocks.fire.setFireInfo(
                            b,
                            oldFire.getEncouragement(b),
                            oldFire.getFlammability(b)
                    );
                }
            }

            // 4) sanity log (very useful)
            System.out.println("ControlledBurn: replaced Blocks.fire; Blocks.fire=" + Blocks.fire +
                    " oldFire=" + oldFire + " id=" + Block.getIdFromBlock(oldFire));

        } catch (Throwable t) {
            System.err.println("ControlledBurn: replaceVanillaFire failed");
            t.printStackTrace();
        }
    }

}
