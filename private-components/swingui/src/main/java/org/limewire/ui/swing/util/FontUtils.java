package org.limewire.ui.swing.util;

import javax.swing.JComponent;

public class FontUtils {
    
    private FontUtils() {}
    
    public static void changeFontSize(JComponent component, float increment) {
        component.setFont(component.getFont().deriveFont(component.getFont().getSize() + increment));
    }
    
    public static void changeStyle(JComponent component, int style) {
        component.setFont(component.getFont().deriveFont(style));
    }

}
