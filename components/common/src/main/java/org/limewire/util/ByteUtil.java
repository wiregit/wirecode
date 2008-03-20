package org.limewire.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ByteUtil {
    private ByteUtil() {
    }

    /**
     * A wrapped version of {@link String#getBytes(String)} that changes the
     * unlikely encoding exception into a runtime exception. Returns empty array
     * if the passed in string is null.
     */
    public static byte[] toUTF8Bytes(String string) {
        if (string == null)
            return new byte[0];
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported?", ex);
        }
    }

    /**
     * A wrapped version of {@link String#String(byte[], String)} that changes
     * the unlikely encoding exception into a runtime exception. Returns null if
     * the passed in array is null.
     */
    public static String toStringFromUTF8Bytes(byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported?", ex);
        }
    }

    /**
     * @return a big-endian array of byteCount bytes matching the passed-in
     *         number: ie: 1L,4 becomes -> [0,0,0,1]
     * @param byteCount a number between 0 and 8, the size of the resulting
     *        array
     * @throws NegativeArraySizeException if byteCount < 0
     * @throws ArrayIndexOutOfBoundsException if byteCount > 8
     */
    public static byte[] convertToBytes(long i, int byteCount) {
        byte[] b = new byte[8];
        b[7] = (byte) (i);
        i >>>= 8;
        b[6] = (byte) (i);
        i >>>= 8;
        b[5] = (byte) (i);
        i >>>= 8;
        b[4] = (byte) (i);
        i >>>= 8;
        b[3] = (byte) (i);
        i >>>= 8;
        b[2] = (byte) (i);
        i >>>= 8;
        b[1] = (byte) (i);
        i >>>= 8;
        b[0] = (byte) (i);

        // We have an 8 byte array. Copy the interesting bytes into our new
        // array of size 'byteCount'
        byte[] bytes = new byte[byteCount];
        System.arraycopy(b, 8 - byteCount, bytes, 0, byteCount);
        return bytes;
    }

    /**
     * convert a big-endian array into a long. ie: 0,0,0,1 -> 1L. Accepts arrays
     * from 0 to 8 bytes long.
     */
    public static long toLongFromBytes(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = (value << 8) | (bytes[i] & 0xff);
        }
        return value;
    }

    /**
     * @return true if the two byte arrays are both null, both the same array,
     *         or are two arrays that have the same content.
     */
    public static boolean areEqual(byte[] array1, byte[] array2) {
        if (array1 == null && array2 == null)
            return true;
        if (array1 == null || array2 == null)
            return false;
        if (array1 == array2)
            return true;
        if (array1.length != array2.length)
            return false;
        for (int i = 0; i < array1.length; i++)
            if (array1[i] != array2[i])
                return false;
        return true;
    }

    /**
     * Compresses the given byte array using ZIP.
     */
    public static byte[] compress(final byte[] input) {
        final Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);

        compressor.setInput(input);
        compressor.finish();

        final ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        // Compress the data
        final byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            final int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            bos.close();
        } catch (IOException unlikely) {
            throw new RuntimeException("IOException caught closing...", unlikely);
        }

        return bos.toByteArray();
    }

    /** Decompresses the given ZIP-compressed byte array */
    public static byte[] decompress(final byte[] input) {
        final Inflater decompressor = new Inflater();
        decompressor.setInput(input);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        // Decompress the data
        final byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                final int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
            }
        }
        try {
            bos.close();
        } catch (IOException unlikely) {
            throw new RuntimeException("IOException caught closing...", unlikely);
        }

        return bos.toByteArray();
    }
}
