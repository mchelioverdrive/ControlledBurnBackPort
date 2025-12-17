package com.ragex.mctools;

import cpw.mods.fml.common.FMLCommonHandler;

public class MCTools {

    //nothing else is needed but our crash method because I am NOT rewriting this person's entire library!

    public static void crash(Exception e, boolean hardExit)
    {
        crash(e, 700, hardExit);
    }

    public static void crash(Exception e, int code, boolean hardExit)
    {
        e.printStackTrace();
        FMLCommonHandler.instance().exitJava(code, hardExit);
    }

}
