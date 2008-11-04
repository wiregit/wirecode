package org.limewire.ui.swing.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class PaintUtils {
   
    /**
     * Draws a string to a graphics with antialiasing
     */
    public static void drawSmoothString(Graphics g, String s, int x, int y) {
        
        Graphics2D g2 = (Graphics2D) g;
        
        // Get original antialiasing value for reset
        Object origAntiAliasHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // Turn on antailiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        
        // Draw the string
        g2.drawString(s, x, y);

        // Reset antialiasing propery
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint);
    }
    
    
    /**
     * Produces a new lighted colour value by a given intensity from a base colour 
     */
    public static Color lighten(Color orig, int intensity) {
        return new Color(orig.getRed() + intensity,
                orig.getGreen() + intensity,
                orig.getBlue() + intensity);
    }
}
