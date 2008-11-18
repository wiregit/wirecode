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
        assertEquals("0 B", GuiUtils.toUnitbytes(0));
        assertEquals("1 B", GuiUtils.toUnitbytes(1));
        assertEquals("10 B", GuiUtils.toUnitbytes(10));
        assertEquals("51 B", GuiUtils.toUnitbytes(51));
        assertEquals("52 B", GuiUtils.toUnitbytes(52));
        assertEquals("53 B", GuiUtils.toUnitbytes(53));
        assertEquals("99 B", GuiUtils.toUnitbytes(99));
        assertEquals("0.1 KB", GuiUtils.toUnitbytes(100));
        assertEquals("0.2 KB", GuiUtils.toUnitbytes(232));
        assertEquals("0.2 KB", GuiUtils.toUnitbytes(232));
        assertEquals("1.0 KB", GuiUtils.toUnitbytes(1024));
        assertEquals("1.0 MB", GuiUtils.toUnitbytes(1024 * 1024));
        assertEquals("1.5 MB", GuiUtils.toUnitbytes(1024 * 1024 + 512 * 1024));
        assertEquals("9.5 MB", GuiUtils.toUnitbytes(1024 * 1024 * 9 + 512 * 1024));
        assertEquals("9.9 MB", GuiUtils.toUnitbytes(1024 * 1024 * 9 + 900 * 1024));

        int TEN_MBS = 10 * 1024 * 1024;
        int TENTH_OF_A_MB = 1024 * 1024 / 10;
        assertEquals("10.0 MB", GuiUtils.toUnitbytes(TEN_MBS - 1));
        assertEquals("10.0 MB", GuiUtils.toUnitbytes(TEN_MBS - 1024));
        assertEquals("10.0 MB", GuiUtils.toUnitbytes(TEN_MBS));
        assertEquals("10.1 MB", GuiUtils.toUnitbytes(TEN_MBS + TENTH_OF_A_MB));
        assertEquals("999.0 MB", GuiUtils.toUnitbytes(1024 * 1024 * 999));
        assertEquals("999.9 MB", GuiUtils.toUnitbytes(1024 * 1024 * 999 + 900 * 1024));

        long TEN_GBS = TEN_MBS * 1024L;
        long TENTH_OF_A_GB = TENTH_OF_A_MB * 1024L;
        assertEquals("1.0 GB", GuiUtils.toUnitbytes(TEN_GBS / 10));
        assertEquals("10.0 GB", GuiUtils.toUnitbytes(TEN_GBS - 1L));
        assertEquals("10.0 GB", GuiUtils.toUnitbytes(TEN_GBS - 1024L * 1024L));
        assertEquals("10.0 GB", GuiUtils.toUnitbytes(TEN_GBS));
        assertEquals("10.1 GB", GuiUtils.toUnitbytes(TEN_GBS + TENTH_OF_A_GB));
        assertEquals("1,023 GB", GuiUtils.toUnitbytes((1024L * 1024L * 1024L * 1023L)));

        long TEN_TBS = TEN_GBS * 1024L;
        long TENTH_OF_A_TB = TENTH_OF_A_GB * 1024L;
        assertEquals("1.0 TB", GuiUtils.toUnitbytes((1024L * 1024L * 1024L * 1024L)));
        assertEquals("1.5 TB", GuiUtils.toUnitbytes((1024L * 1024L * 1024L * 1024L)
                + (1024L * 1024L * 1024L * 512L)));
        assertEquals("10.0 TB", GuiUtils.toUnitbytes(TEN_TBS - 1L));
        assertEquals("10.0 TB", GuiUtils.toUnitbytes(TEN_TBS - 1L));
        assertEquals("10.0 TB", GuiUtils.toUnitbytes(TEN_TBS - 1024L * 1024L * 1024L));
        assertEquals("10.0 TB", GuiUtils.toUnitbytes(TEN_TBS));
        assertEquals("10.1 TB", GuiUtils.toUnitbytes(TEN_TBS + TENTH_OF_A_TB));

        long TEN_PBS = TEN_TBS * 1024L;
        assertEquals("10,240 TB", GuiUtils.toUnitbytes(TEN_PBS));
        assertEquals("10,241 TB", GuiUtils.toUnitbytes(TEN_PBS + 1024L * 1024L * 1024L * 1024L));
    }

}
