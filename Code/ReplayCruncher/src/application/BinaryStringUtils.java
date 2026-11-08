package application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class BinaryStringUtils {
    private BinaryStringUtils() {}

    public static String readFixedString(ByteBuffer buf, int offset, int length) {
        byte[] bytes = new byte[length];
        int oldPos = buf.position();
        buf.position(offset);
        buf.get(bytes);
        buf.position(oldPos);
        int end = 0;
        while (end < bytes.length && bytes[end] != 0) end++;
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}