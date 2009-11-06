package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JPanel;

import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for CustomLineBorder.
 */
public class CustomLineBorderTest extends BaseTestCase {
    /** Instance of class being tested. */
    private CustomLineBorder customLineBorder;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public CustomLineBorderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        customLineBorder = new CustomLineBorder();
    }

    @Override
    protected void tearDown() throws Exception {
        customLineBorder = null;
        super.tearDown();
    }

    /** Tests no-arg constructor for initial state. */
    public void testCustomLineBorder() {
        // Create component and get border insets.
        JPanel panel = new JPanel();
        panel.setBorder(customLineBorder);
        Insets insets = customLineBorder.getBorderInsets(panel);
        
        // Verify insets.
        int defaultValue = 1;
        assertEquals("top inset", defaultValue, insets.top);
        assertEquals("left inset", defaultValue, insets.left);
        assertEquals("bottom inset", defaultValue, insets.bottom);
        assertEquals("right inset", defaultValue, insets.right);
    }

    /** Tests constructor using custom thicknesses. */
    public void testCustomLineBorderWithThickness() {
        // Create border with custom thicknesses.
        int top = 1;
        int left = 2;
        int bottom = 3;
        int right = 4;
        customLineBorder = new CustomLineBorder(Color.BLACK, top, Color.BLACK, left,
                Color.BLACK, bottom, Color.BLACK, right);
        
        // Create component and get border insets.
        JPanel panel = new JPanel();
        panel.setBorder(customLineBorder);
        Insets insets = customLineBorder.getBorderInsets(panel);
        
        // Verify insets.
        assertEquals("top inset", top, insets.top);
        assertEquals("left inset", left, insets.left);
        assertEquals("bottom inset", bottom, insets.bottom);
        assertEquals("right inset", right, insets.right);
    }
}
