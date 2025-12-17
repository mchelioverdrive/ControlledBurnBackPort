package com.ragex.tools;

import com.ragex.lwjgl.Quaternion;
//import com.ragex.tools.datastructures.Pair;
import sun.misc.Cleaner;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Tools
{
    public static final String REGEX_ANY = ".*";
    public static final String REGEX_SPECIAL_CHARACTERS_TEST = ".*[.+*?^$()\\[\\]{}|\\\\].*";

    protected static final Random random = new Random();
    protected static PrintStream out = null, err = null;

    /* =========================
       Formatting / Math
       ========================= */

    public static String formatNicely(double d)
    {
        int i = (int) d;

        if ((int) (d + 1e-8) != i) d = ++i;
        else if ((int) (d - 1e-8) != i) d = i;

        double ad = Math.abs(d);
        if (ad == 0) return "0";

        if (ad >= 1000)
        {
            int n = 0;
            while (Math.abs(i) > 10)
            {
                i /= 10;
                n++;
            }
            return i + "x10^" + n;
        }

        if (ad >= 100) return "" + (int) d;

        if (ad >= 0.1)
        {
            String s = String.format("%.1f", d);
            return s.endsWith(".0") ? "" + i : s;
        }

        int n = 0;
        while (Math.abs(d) < 1)
        {
            d *= 10;
            n++;
        }
        String base = String.format("%.1f", d);
        return base.endsWith(".0") ? i + "x10^-" + n : base + "x10^-" + n;
    }

    public static Quaternion rotatedQuaternion(Quaternion v, Quaternion axis, double theta)
    {
        return rotatedQuaternion(v, axis, theta, TrigLookupTable.TRIG_TABLE_1048576);
    }

    public static Quaternion rotatedQuaternion(Quaternion v, Quaternion axis, double theta, TrigLookupTable trig)
    {
        double sin = trig.sin(theta * 0.5);
        double cos = trig.cos(theta * 0.5);

        Quaternion q = new Quaternion(
                (float) (sin * axis.x),
                (float) (sin * axis.y),
                (float) (sin * axis.z),
                (float) cos
        );

        Quaternion qc = new Quaternion(
                (float) -(sin * axis.x),
                (float) -(sin * axis.y),
                (float) -(sin * axis.z),
                (float) cos
        );

        return Quaternion.mul(Quaternion.mul(q, v, null), qc, null);
    }

    /* =========================
       Expression Parser
       ========================= */

    public static double calc(final String str)
    {
        return new Object()
        {
            int pos = -1, ch;

            void nextChar()
            {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int c)
            {
                while (ch == ' ') nextChar();
                if (ch == c)
                {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse()
            {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression()
            {
                double x = parseTerm();
                for (;;)
                {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm()
            {
                double x = parseFactor();
                for (;;)
                {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor()
            {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int start = pos;

                if (eat('('))
                {
                    x = parseExpression();
                    eat(')');
                }
                else if ((ch >= '0' && ch <= '9') || ch == '.')
                {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(start, pos));
                }
                else if (ch >= 'a' && ch <= 'z')
                {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(start, pos);
                    x = parseFactor();

                    if ("sqrt".equals(func)) x = Math.sqrt(x);
                    else if ("sin".equals(func)) x = TrigLookupTable.TRIG_TABLE_1048576.sin(degtorad(x));
                    else if ("cos".equals(func)) x = TrigLookupTable.TRIG_TABLE_1048576.cos(degtorad(x));
                    else if ("tan".equals(func)) x = TrigLookupTable.TRIG_TABLE_1048576.tan(degtorad(x));
                    else throw new RuntimeException("Unknown function: " + func);
                }
                else throw new RuntimeException("Unexpected: " + (char) ch);

                if (eat('^')) x = Math.pow(x, parseFactor());
                return x;
            }
        }.parse();
    }

    /* =========================
       Random helpers (1.7-safe)
       ========================= */

    public static double random()
    {
        return random.nextDouble();
    }

    public static int random(int max)
    {
        return (int) (random.nextDouble() * max);
    }

    public static double random(double max)
    {
        return random.nextDouble() * max;
    }

    public static <T> T choose(T[] arr)
    {
        return arr[random(arr.length)];
    }

    public static <T> T choose(List<T> arr)
    {
        return arr.get(random(arr.size()));
    }

    /* =========================
       ByteBuffer / Unsafe
       ========================= */

    public static void freeDirectByteBuffer(ByteBuffer buffer)
    {
        try
        {
            Field f = buffer.getClass().getDeclaredField("cleaner");
            f.setAccessible(true);
            Cleaner cleaner = (Cleaner) f.get(buffer);
            if (cleaner != null) cleaner.clean();
        }
        catch (Exception ignored) {}
    }

    public static ByteBuffer allocateNative(int bytes)
    {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    /* =========================
       Misc
       ========================= */

    public static double degtorad(double deg)
    {
        return deg * Math.PI / 180D;
    }

    public static double radtodeg(double rad)
    {
        return rad * 180D / Math.PI;
    }

    public static int posMod(int a, int b)
    {
        a %= b;
        return a < 0 ? a + b : a;
    }

    public static boolean regexMatches(String regex, String s)
    {
        return regex != null && s != null && Pattern.matches(regex, s);
    }

    public static boolean hasRegexSpecialCharacters(String s)
    {
        return regexMatches(REGEX_SPECIAL_CHARACTERS_TEST, s);
    }
}
