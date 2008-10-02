package org.limewire.ui.swing.util;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

public class FontUtils {
    
    private FontUtils() {}
    
    public static void changeSize(JComponent component, float increment) {
        Font font = component.getFont();
        if (font == null) return;
        float newSize = font.getSize() + increment;
        component.setFont(font.deriveFont(newSize));
    }
    
    public static void changeStyle(JComponent component, int style) {
        component.setFont(component.getFont().deriveFont(style));
    }
    
    public static void bold(JComponent component) {
        changeStyle(component, Font.BOLD);
    }
    
    public static void plain(JComponent component) {
        changeStyle(component, Font.PLAIN);
    }

    public static void underline(JComponent component) {
        Font font = component.getFont();
        if (font == null) return;
        Map<TextAttribute, ?> map = font.getAttributes();
        Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
        newMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        component.setFont(font.deriveFont(newMap));
    }
    
    public static void removeUnderline(JComponent component) {
        Font font = component.getFont();
        if (font == null) return;
        Map<TextAttribute, ?> map = font.getAttributes();
        Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
        newMap.put(TextAttribute.UNDERLINE, Integer.valueOf(-1));
        component.setFont(font.deriveFont(newMap));
    }
    
    /**
     * Determines if a font can display up to a point in the string.
     *
     * Returns -1 if it can display the whole string.
     */
    public static boolean canDisplay(Font f, String s) {
        int upTo = f.canDisplayUpTo(s);
        if(upTo >= s.length() || upTo == -1)
            return true;
        else
            return false;
    }

}
