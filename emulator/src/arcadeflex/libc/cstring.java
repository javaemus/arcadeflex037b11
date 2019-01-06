package arcadeflex.libc;

import static arcadeflex.libc.ptr.*;
import static common.ptr.*;

/**
 *
 * @author shadow
 */
public class cstring {

    /**
     * MEMSET functions
     */
    public static void memset(char[] buf, int value, int size) {
        for (int mem = 0; mem < size; mem++) {
            buf[mem] = (char) value;
        }
    }

    public static void memset(short[] dst, int value, int size) {
        for (int mem = 0; mem < size; mem++) {
            dst[mem] = (short) value;
        }
    }

    public static void memset(int[] dst, int value, int size) {
        for (int mem = 0; mem < size; mem++) {
            dst[mem] = value;
        }
    }

    public static void memset(UBytePtr ptr, int value, int length) {
        for (int i = 0; i < length; i++) {
            ptr.write(i, value);
        }
    }

    public static void memset(UBytePtr ptr, int offset, int value, int length) {
        for (int i = 0; i < length; i++) {
            ptr.write(i + offset, value);
        }
    }

    public static void memset(ShortPtr buf, int value, int size) {
        for (int i = 0; i < size; i++) {
            buf.write(i, (short) value);
        }
    }

    /**
     * MEMCPY functions
     */
    public static void memcpy(UBytePtr dst, int dstoffs, UBytePtr src, int size) {
        for (int i = 0; i < Math.min(size, src.memory.length); i++) {
            dst.write(i + dstoffs, src.read(i));
        }
    }
    
    /**
     * STRCMP function
     */
    public static int strcmp(String str1, String str2) {
        return str1.compareTo(str2);
    }
}
