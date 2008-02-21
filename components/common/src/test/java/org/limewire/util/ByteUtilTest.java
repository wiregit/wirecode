package org.limewire.util;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtil;

public class ByteUtilTest extends BaseTestCase {
    public ByteUtilTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ByteUtilTest.class);
    }

    public void testConvertToBytes() {
        byte[] bytes = ByteUtil.convertToBytes(1L, 4);
        assertEquals(new byte[] { 0, 0, 0, 1 }, bytes);

        bytes = ByteUtil.convertToBytes(256L, 4);
        assertEquals(new byte[] { 0, 0, 1, 0 }, bytes);

        bytes = ByteUtil.convertToBytes(65536L, 4);
        assertEquals(new byte[] { 0, 1, 0, 0 }, bytes);

        bytes = ByteUtil.convertToBytes(65537L, 4);
        assertEquals(new byte[] { 0, 1, 0, 1 }, bytes);

        bytes = ByteUtil.convertToBytes(16777216L, 4);
        assertEquals(new byte[] { 1, 0, 0, 0 }, bytes);
    }

    public void testConvertToBytes2() {
        byte[] bytes = ByteUtil.convertToBytes(1L, 2);
        assertEquals(new byte[] { 0, 1 }, bytes);
    }

    public void testConvertToBytesMinus1() {
        byte[] bytes = ByteUtil.convertToBytes(-1L, 2);
        assertEquals(new byte[] { -1, -1 }, bytes);

        bytes = ByteUtil.convertToBytes(-1L, 8);
        assertEquals(new byte[] { -1, -1, -1, -1, -1, -1, -1, -1 }, bytes);

    }

    public void testConvertToBytesExtents() {
        try {
            ByteUtil.convertToBytes(1, 9);
            fail("Expected an exception for byteCount > 8");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }
        try {
            ByteUtil.convertToBytes(1, -1);
            fail("Expected an exception for byteCount < 0");
        } catch (NegativeArraySizeException expected) {
        }

    }

    public void testToLongFromBytes() {
        // Depends on testConvertToBytes to be correct.
        for (long i = 0; i < 17000000; i++) {
            byte[] bytes = ByteUtil.convertToBytes(i, 4);
            assertEquals(i, ByteUtil.toLongFromBytes(bytes));
        }
        for (long i = 0; i < 1000; i++) {
            byte[] bytes = ByteUtil.convertToBytes(i, 8);
            assertEquals(i, ByteUtil.toLongFromBytes(bytes));
        }
        byte[] bytes = ByteUtil.convertToBytes(2147483647L, 8);
        assertEquals(2147483647L, ByteUtil.toLongFromBytes(bytes));

        bytes = ByteUtil.convertToBytes(Long.MAX_VALUE, 8);
        assertEquals(Long.MAX_VALUE, ByteUtil.toLongFromBytes(bytes));
        bytes = ByteUtil.convertToBytes(Long.MIN_VALUE, 8);
        assertEquals(Long.MIN_VALUE, ByteUtil.toLongFromBytes(bytes));
    }

}
