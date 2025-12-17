package com.ragex.tools;

import com.ragex.mctools.MCTools;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionTool
{
    public static final IllegalArgumentException ILLEGAL_ARGUMENT_EXCEPTION = new IllegalArgumentException();


    /* =========================
       FIELD ACCESS
       ========================= */

    public static Field getField(Class clazz, String... possibleNames)
    {
        return getField(false, clazz, possibleNames);
    }

    public static Field getField(boolean printFound, Class clazz, String... possibleNames)
    {
        try
        {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields)
            {
                for (String name : possibleNames)
                {
                    if (field.getName().equals(name))
                    {
                        field.setAccessible(true);

                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                        if (printFound) System.out.println("Found field: " + name);
                        return field;
                    }
                }
            }
        }
        catch (Exception ignored) {}

        return null;
    }


    /* =========================
       METHOD ACCESS
       ========================= */

    public static Method getMethod(Class clazz, String... possibleNames)
    {
        return getMethod(false, clazz, null, possibleNames);
    }

    public static Method getMethod(boolean printFound, Class clazz, String... possibleNames)
    {
        return getMethod(printFound, clazz, null, possibleNames);
    }

    public static Method getMethod(Class clazz, int paramCount, String... possibleNames)
    {
        return getMethod(false, clazz, paramCount, possibleNames);
    }

    public static Method getMethod(boolean printFound, Class clazz, int paramCount, String... possibleNames)
    {
        Class[] paramTypes = new Class[paramCount];
        return getMethod(printFound, clazz, paramTypes, possibleNames);
    }

    public static Method getMethod(Class clazz, Class[] paramTypes, String... possibleNames)
    {
        return getMethod(false, clazz, paramTypes, possibleNames);
    }

    public static Method getMethod(boolean printFound, Class clazz, Class[] paramTypes, String... possibleNames)
    {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods)
        {
            for (String name : possibleNames)
            {
                if (!method.getName().equals(name)) continue;

                if (paramTypes == null)
                {
                    method.setAccessible(true);
                    if (printFound) System.out.println("Found method: " + name);
                    return method;
                }

                Class[] methodParams = method.getParameterTypes();
                if (methodParams.length != paramTypes.length) continue;

                boolean match = true;
                for (int i = 0; i < paramTypes.length; i++)
                {
                    if (paramTypes[i] != null && !paramTypes[i].equals(methodParams[i]))
                    {
                        match = false;
                        break;
                    }
                }

                if (match)
                {
                    method.setAccessible(true);
                    if (printFound) System.out.println("Found method: " + name);
                    return method;
                }
            }
        }
        return null;
    }


    /* =========================
       CLASS LOOKUP
       ========================= */

    public static Class getClassByName(String name)
    {
        try
        {
            return Class.forName(name);
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }
    }

    public static Class getInternalClass(Class clazz, String... possibleNames)
    {
        Class[] classes = clazz.getDeclaredClasses();
        for (Class c : classes)
        {
            for (String name : possibleNames)
            {
                if (c.getSimpleName().equals(name)) return c;
            }
        }
        return null;
    }


    /* =========================
       INSTANCE CREATION
       ========================= */

    public static Object getInstance(String className, int constructorIndex, Object... args)
    {
        return getInstance(getClassByName(className), constructorIndex, args);
    }

    public static Object getInstance(Class clazz, int constructorIndex, Object... args)
    {
        try
        {
            return clazz.getConstructors()[constructorIndex].newInstance(args);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            throw ILLEGAL_ARGUMENT_EXCEPTION;
        }
    }


    /* =========================
       FIELD SET / GET
       ========================= */

    public static void set(Class clazz, String fieldName, Object obj, Object value)
    {
        set(clazz, new String[]{fieldName}, obj, value);
    }

    public static void set(Class clazz, String[] fieldNames, Object obj, Object value)
    {
        Field field = getField(clazz, fieldNames);
        if (field != null) set(field, obj, value);
    }

    public static void set(Field field, Object obj, Object value)
    {
        try
        {
            field.set(obj, value);
        }
        catch (IllegalAccessException e)
        {
            MCTools.crash(e, false);
        }
    }

    public static Object get(Class clazz, String fieldName, Object obj)
    {
        return get(clazz, new String[]{fieldName}, obj);
    }

    public static Object get(Class clazz, String[] fieldNames, Object obj)
    {
        Field field = getField(clazz, fieldNames);
        return field == null ? null : get(field, obj);
    }

    public static Object get(Field field, Object obj)
    {
        try
        {
            return field.get(obj);
        }
        catch (IllegalAccessException e)
        {
            MCTools.crash(e, false);
            return null;
        }
    }


    /* =========================
       METHOD INVOCATION
       ========================= */

    public static Object invoke(Class clazz, String methodName, Object obj, Object... args)
    {
        return invoke(clazz, new String[]{methodName}, obj, args);
    }

    public static Object invoke(Class clazz, String[] methodNames, Object obj, Object... args)
    {
        Method method = getMethod(clazz, args.length, methodNames);
        return method == null ? null : invoke(method, obj, args);
    }

    public static Object invoke(Method method, Object obj, Object... args)
    {
        try
        {
            method.setAccessible(true);
            return method.invoke(obj, args);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            MCTools.crash(e, false);
            return null;
        }
    }
}
