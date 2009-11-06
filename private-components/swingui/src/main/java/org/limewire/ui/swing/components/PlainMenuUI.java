package org.limewire.ui.swing.components;

import java.awt.Color;

import javax.swing.plaf.basic.BasicMenuUI;

public class PlainMenuUI extends BasicMenuUI {
    private static Color originalSelectionBackground;
    private static Color originalSelectionForeground;

    public static void overrideDefaults(Color selectionForeground, Color selectionBackground) {
        originalSelectionForeground = selectionForeground;
        originalSelectionBackground = selectionBackground;
    }
    
    public PlainMenuUI() {
    }

    @Override
    public void installDefaults() {
        super.installDefaults();
        selectionForeground = originalSelectionForeground;
        selectionBackground = originalSelectionBackground;
    }
}
