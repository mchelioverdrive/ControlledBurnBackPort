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
            // 1) old & new block refs
            BlockFire oldFire = (BlockFire) Blocks.fire;
            BlockFireEdit newFire = (BlockFireEdit) new BlockFireEdit()
                    .setHardness(0.0F)
                    .setLightLevel(1.0F)
                    .setBlockName("fire");

            // 2) replace Blocks.fire static
            ReflectionTool.set(
                    Blocks.class,
                    new String[]{"field_150480_ab", "fire"},
                    null,
                    newFire
            );

            // 3) get the registry and id/key for the old fire
            Object registry = Block.blockRegistry; // RegistryNamespaced
            int fireId = Block.getIdFromBlock(oldFire);

            // getNameForObject should exist; return type is typically String in 1.7.10
            Method getNameForObject = registry.getClass().getMethod("getNameForObject", Object.class);
            Object registryKey = getNameForObject.invoke(registry);

            // 4) try to call the registration method that exists in this MC/Forge: addObject(...) (1.7.10)
            boolean replacedInRegistry = false;
            try {
                // try common 1.7.10 signature: addObject(int, String, Object)
                Method addObject = registry.getClass().getMethod("addObject", int.class, String.class, Object.class);
                addObject.setAccessible(true);
                addObject.invoke(registry, fireId, registryKey.toString(), newFire);
                replacedInRegistry = true;
            } catch (NoSuchMethodException nsme) {
                // some mappings/versions may have 'register(int, K, V)' instead — try that
                try {
                    Method register = registry.getClass().getMethod("register", int.class, registryKey.getClass(), Object.class);
                    register.setAccessible(true);
                    register.invoke(registry, fireId, registryKey, newFire);
                    replacedInRegistry = true;
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
                    // fall through to the generic-search attempt below
                }
            }

            // 4b) fallback: search for any 3-arg method with first parameter int and try to invoke it
            if (!replacedInRegistry) {
                for (Method m : registry.getClass().getMethods()) {
                    if (m.getParameterTypes().length == 3 && m.getParameterTypes()[0] == int.class) {
                        String name = m.getName().toLowerCase();
                        if (name.contains("add") || name.contains("reg")) {
                            try {
                                m.setAccessible(true);
                                // try to pass registryKey (if a String is required, use toString())
                                Object keyArg = (m.getParameterTypes()[1] == String.class) ? registryKey.toString() : registryKey;
                                m.invoke(registry, fireId, keyArg, newFire);
                                replacedInRegistry = true;
                                break;
                            } catch (IllegalAccessException | InvocationTargetException ignored) { }
                        }
                    }
                }
            }

            if (!replacedInRegistry) {
                System.err.println("ControlledBurn: couldn't find registry add/register method via reflection. Registry replacement may fail.");
            }

            // 5) copy vanilla fire stats (iterate registry using iterator())
            // RegistryNamespaced implements Iterable<V> so iterator() is available in 1.7.10
            java.util.Iterator it = (java.util.Iterator) registry.getClass().getMethod("iterator").invoke(registry);
            while (it.hasNext()) {
                Block b = (Block) it.next();
                if (b != Blocks.air && oldFire.getEncouragement(b) > 0) {
                    // use newFire.setFireInfo to copy encouragement/flamability
                    newFire.setFireInfo(
                            b,
                            oldFire.getEncouragement(b),
                            oldFire.getFlammability(b)
                    );
                }
            }

            // 6) sanity log
            System.out.println("ControlledBurn: replaced Blocks.fire; Blocks.fire=" + Blocks.fire +
                    " oldFire=" + oldFire + " id=" + fireId + " key=" + registryKey);

        } catch (Throwable t) {
            System.err.println("ControlledBurn: replaceVanillaFire failed");
            t.printStackTrace();
        }
    }




}
