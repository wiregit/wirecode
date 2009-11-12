package org.limewire.ui.swing.util;

import java.awt.event.KeyEvent;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class GuiUtilsTest extends BaseTestCase {

    public GuiUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GuiUtilsTest.class);
    }

    public void testStripAmpersand() {
        assertEquals("hello world", GuiUtils.stripAmpersand("hello &world"));
        assertEquals("hello world", GuiUtils.stripAmpersand("he&llo world"));
        assertEquals("hello & world", GuiUtils.stripAmpersand("&hello & world"));
        assertEquals("hello & world", GuiUtils.stripAmpersand("hello & &world"));

        assertEquals("hello world&", GuiUtils.stripAmpersand("hello world&"));
        assertEquals("hello world", GuiUtils.stripAmpersand("hello world"));
    }

    public void testGetMnemonicKeyCode() {
        assertEquals(KeyEvent.VK_8, GuiUtils.getMnemonicKeyCode("hello &8"));
        assertEquals(KeyEvent.VK_H, GuiUtils.getMnemonicKeyCode("&hello &8"));
        assertEquals(KeyEvent.VK_W, GuiUtils.getMnemonicKeyCode("hello & &world"));
        assertEquals(KeyEvent.VK_W, GuiUtils.getMnemonicKeyCode("hello & &World"));
    }

    public void testUnitBytesBoundaries() {
        assertEquals("0 bytes", GuiUtils.formatUnitFromBytes(0));
        assertEquals("1 bytes", GuiUtils.formatUnitFromBytes(1));
        assertEquals("10 bytes", GuiUtils.formatUnitFromBytes(10));
        assertEquals("51 bytes", GuiUtils.formatUnitFromBytes(51));
        assertEquals("52 bytes", GuiUtils.formatUnitFromBytes(52));
        assertEquals("53 bytes", GuiUtils.formatUnitFromBytes(53));
        assertEquals("99 bytes", GuiUtils.formatUnitFromBytes(99));
        assertEquals("100 bytes", GuiUtils.formatUnitFromBytes(100));
        assertEquals("232 bytes", GuiUtils.formatUnitFromBytes(232));
        assertEquals("1 KB", GuiUtils.formatUnitFromBytes(1024));
        assertEquals("1.00 MB", GuiUtils.formatUnitFromBytes(1024 * 1024));
        assertEquals("1.50 MB", GuiUtils.formatUnitFromBytes(1024 * 1024 + 512 * 1024));
        assertEquals("9.50 MB", GuiUtils.formatUnitFromBytes(1024 * 1024 * 9 + 512 * 1024));
        assertEquals("9.88 MB", GuiUtils.formatUnitFromBytes(1024 * 1024 * 9 + 900 * 1024));

        int TEN_MBS = 10 * 1024 * 1024;
        int TENTH_OF_A_MB = 1024 * 1024 / 10;
        assertEquals("10.00 MB", GuiUtils.formatUnitFromBytes(TEN_MBS - 1));
        assertEquals("10.00 MB", GuiUtils.formatUnitFromBytes(TEN_MBS - 1024));
        assertEquals("10.00 MB", GuiUtils.formatUnitFromBytes(TEN_MBS));
        assertEquals("10.10 MB", GuiUtils.formatUnitFromBytes(TEN_MBS + TENTH_OF_A_MB));
        assertEquals("999.00 MB", GuiUtils.formatUnitFromBytes(1024 * 1024 * 999));
        assertEquals("999.88 MB", GuiUtils.formatUnitFromBytes(1024 * 1024 * 999 + 900 * 1024));

        long TEN_GBS = TEN_MBS * 1024L;
        long TENTH_OF_A_GB = TENTH_OF_A_MB * 1024L;
        assertEquals("1.00 GB", GuiUtils.formatUnitFromBytes(TEN_GBS / 10));
        assertEquals("10.00 GB", GuiUtils.formatUnitFromBytes(TEN_GBS - 1L));
        assertEquals("10.00 GB", GuiUtils.formatUnitFromBytes(TEN_GBS - 1024L * 1024L));
        assertEquals("10.00 GB", GuiUtils.formatUnitFromBytes(TEN_GBS));
        assertEquals("10.10 GB", GuiUtils.formatUnitFromBytes(TEN_GBS + TENTH_OF_A_GB));
        assertEquals("1,023.00 GB", GuiUtils.formatUnitFromBytes((1024L * 1024L * 1024L * 1023L)));

        long TEN_TBS = TEN_GBS * 1024L;
        long TENTH_OF_A_TB = TENTH_OF_A_GB * 1024L;
        assertEquals("1.00 TB", GuiUtils.formatUnitFromBytes((1024L * 1024L * 1024L * 1024L)));
        assertEquals("1.50 TB", GuiUtils.formatUnitFromBytes((1024L * 1024L * 1024L * 1024L)
                + (1024L * 1024L * 1024L * 512L)));
        assertEquals("10.00 TB", GuiUtils.formatUnitFromBytes(TEN_TBS - 1L));
        assertEquals("10.00 TB", GuiUtils.formatUnitFromBytes(TEN_TBS - 1L));
        assertEquals("10.00 TB", GuiUtils.formatUnitFromBytes(TEN_TBS - 1024L * 1024L * 1024L));
        assertEquals("10.00 TB", GuiUtils.formatUnitFromBytes(TEN_TBS));
        assertEquals("10.10 TB", GuiUtils.formatUnitFromBytes(TEN_TBS + TENTH_OF_A_TB));

        long TEN_PBS = TEN_TBS * 1024L;
        assertEquals("10,240.00 TB", GuiUtils.formatUnitFromBytes(TEN_PBS));
        assertEquals("10,241.00 TB", GuiUtils.formatUnitFromBytes(TEN_PBS + 1024L * 1024L * 1024L * 1024L));
    }

    public void testUnitBytesPerSecBoundaries() {
        assertEquals("0 B/s", GuiUtils.formatUnitFromBytesPerSec(0));
        assertEquals("1 B/s", GuiUtils.formatUnitFromBytesPerSec(1));
        assertEquals("10 B/s", GuiUtils.formatUnitFromBytesPerSec(10));
        assertEquals("51 B/s", GuiUtils.formatUnitFromBytesPerSec(51));
        assertEquals("52 B/s", GuiUtils.formatUnitFromBytesPerSec(52));
        assertEquals("53 B/s", GuiUtils.formatUnitFromBytesPerSec(53));
        assertEquals("99 B/s", GuiUtils.formatUnitFromBytesPerSec(99));
        assertEquals("100 B/s", GuiUtils.formatUnitFromBytesPerSec(100));
        assertEquals("232 B/s", GuiUtils.formatUnitFromBytesPerSec(232));
        assertEquals("1 KB/s", GuiUtils.formatUnitFromBytesPerSec(1024));
        assertEquals("1.00 MB/s", GuiUtils.formatUnitFromBytesPerSec(1024 * 1024));
        assertEquals("1.50 MB/s", GuiUtils.formatUnitFromBytesPerSec(1024 * 1024 + 512 * 1024));
        assertEquals("9.50 MB/s", GuiUtils.formatUnitFromBytesPerSec(1024 * 1024 * 9 + 512 * 1024));
        assertEquals("9.88 MB/s", GuiUtils.formatUnitFromBytesPerSec(1024 * 1024 * 9 + 900 * 1024));

        int TEN_MBS = 10 * 1024 * 1024;
        int TENTH_OF_A_MB = 1024 * 1024 / 10;
        assertEquals("10.00 MB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_MBS - 1));
        assertEquals("10.00 MB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_MBS - 1024));
        assertEquals("10.00 MB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_MBS));
        assertEquals("10.10 MB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_MBS + TENTH_OF_A_MB));
        assertEquals("999.00 MB/s", GuiUtils.formatUnitFromBytesPerSec(1024 * 1024 * 999));
        assertEquals("999.88 MB/s", GuiUtils.formatUnitFromBytesPerSec(1024 * 1024 * 999 + 900 * 1024));

        long TEN_GBS = TEN_MBS * 1024L;
        long TENTH_OF_A_GB = TENTH_OF_A_MB * 1024L;
        assertEquals("1.00 GB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_GBS / 10));
        assertEquals("10.00 GB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_GBS - 1L));
        assertEquals("10.00 GB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_GBS - 1024L * 1024L));
        assertEquals("10.00 GB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_GBS));
        assertEquals("10.10 GB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_GBS + TENTH_OF_A_GB));
        assertEquals("1,023.00 GB/s", GuiUtils.formatUnitFromBytesPerSec((1024L * 1024L * 1024L * 1023L)));

        long TEN_TBS = TEN_GBS * 1024L;
        long TENTH_OF_A_TB = TENTH_OF_A_GB * 1024L;
        assertEquals("1.00 TB/s", GuiUtils.formatUnitFromBytesPerSec((1024L * 1024L * 1024L * 1024L)));
        assertEquals("1.50 TB/s", GuiUtils.formatUnitFromBytesPerSec((1024L * 1024L * 1024L * 1024L)
                + (1024L * 1024L * 1024L * 512L)));
        assertEquals("10.00 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_TBS - 1L));
        assertEquals("10.00 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_TBS - 1L));
        assertEquals("10.00 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_TBS - 1024L * 1024L * 1024L));
        assertEquals("10.00 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_TBS));
        assertEquals("10.10 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_TBS + TENTH_OF_A_TB));

        long TEN_PBS = TEN_TBS * 1024L;
        assertEquals("10,240.00 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_PBS));
        assertEquals("10,241.00 TB/s", GuiUtils.formatUnitFromBytesPerSec(TEN_PBS + 1024L * 1024L * 1024L * 1024L));
    }
    
}
