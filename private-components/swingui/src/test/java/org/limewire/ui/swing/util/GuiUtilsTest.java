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
}
