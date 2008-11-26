package org.limewire.ui.swing.util;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;

public class FontUtils {
    
    private FontUtils() {}
    
    public static void changeSize(JComponent component, float increment) {
        Font font = component.getFont();
        if (font == null) return;
        float newSize = font.getSize() + increment;
        component.setFont(font.deriveFont(newSize));
    }
    
    public static void setSize(JComponent component, int size) {
        Font font = component.getFont();
        if(font == null) return;
        component.setFont(font.deriveFont((float)size));
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
        if (font != null) {
            Map<TextAttribute, ?> map = font.getAttributes();
            Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
            newMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            component.setFont(font.deriveFont(newMap));
        }
    }
    
    public static boolean isUnderlined(JComponent component) {
        Font font = component.getFont();
        Map<TextAttribute, ?> map = font.getAttributes();
        return map.get(TextAttribute.UNDERLINE) == TextAttribute.UNDERLINE_ON;
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

    
    private static String unpackText(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Action)
            return (String) ((Action) object).getValue(Action.NAME);
        else
            return object.toString();
    }

    public static Rectangle2D getLongestTextArea(Font font, Object... objects) {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        Rectangle2D largestRect = font.getStringBounds(unpackText(objects[0]), frc);
        for (int i = 1; i < objects.length; i++) {
            Rectangle2D currentRect = font.getStringBounds(unpackText(objects[i]), frc);
            if (currentRect.getWidth() > largestRect.getWidth()) {
                largestRect = currentRect;
            }
        }
        return largestRect;
    }
}
