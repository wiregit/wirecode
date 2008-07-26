package org.limewire.ui.swing.util;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

public class FontUtils {
    
    private FontUtils() {}
    
    public static void changeSize(JComponent component, float increment) {
        component.setFont(component.getFont().deriveFont(component.getFont().getSize() + increment));
    }
    
    public static void changeStyle(JComponent component, int style) {
        component.setFont(component.getFont().deriveFont(style));
    }
    
    public static void bold(JComponent component) {
        changeStyle(component, Font.BOLD);
    }

    public static void underline(JComponent component) {
        Font font = component.getFont();
        Map<TextAttribute, ?> map = font.getAttributes();
        Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
        newMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        component.setFont(font.deriveFont(newMap));
    }
    
    public static void removeUnderline(JComponent component) {
        Font font = component.getFont();
        Map<TextAttribute, ?> map = font.getAttributes();
        Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
        newMap.put(TextAttribute.UNDERLINE, Integer.valueOf(-1));
        component.setFont(font.deriveFont(newMap));
    }

}
