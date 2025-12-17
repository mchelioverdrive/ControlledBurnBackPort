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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    private static void replaceVanillaFire() {
        try {
            // old & new
            BlockFire oldFire = Blocks.fire;
            //we have to CAST this. WE CANNOT JUST DO BlockFire newFire = new BlockFireEdit()
            BlockFireEdit newFire = (BlockFireEdit) new BlockFireEdit()
                    .setHardness(0.0F)
                    .setLightLevel(1.0F)
                    .setBlockName("fire");

            // 1) Replace static Blocks.fire (handles obf/deobf)
            ReflectionTool.set(
                    Blocks.class,
                    new String[]{"field_150480_ab", "fire"},
                    null,
                    newFire
            );

            // 2) Copy vanilla fire stats for all blocks from blockRegistry
            for (Object obj : Block.blockRegistry) {
                if (!(obj instanceof Block)) continue;
                Block b = (Block) obj;
                if (b == Blocks.air) continue;
                if (oldFire.getEncouragement(b) > 0) {
                    newFire.setFireInfo(
                            b,
                            oldFire.getEncouragement(b),
                            oldFire.getFlammability(b)
                    );
                }
            }

            // 3) Sanity logs
            System.out.println("ControlledBurn: replaceVanillaFire done.");
            System.out.println("ControlledBurn: oldFire=" + oldFire + " newFire=" + newFire);
            System.out.println("ControlledBurn: Blocks.fire now = " + Blocks.fire);


        } catch (Throwable t) {
            System.err.println("ControlledBurn: replaceVanillaFire failed");
            t.printStackTrace();
        }
    }






}
