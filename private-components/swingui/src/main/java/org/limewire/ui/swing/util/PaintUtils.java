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
     * Draws a rounded border like box at a given location and size with 
     *  shadowing darkness defined by 3 intensity values
     */
    public static void drawRoundedBorder(Graphics g, int x, int y, int width, 
            int height, int arcWidth, int arcHeight, Color c, int i1, int i2, int i3) {

        // Draw shading
        g.setColor(PaintUtils.lighten(c,i1));
        g.drawRoundRect(x, y+1, width, height-2, arcWidth, arcHeight);
        g.drawLine(x+arcWidth/2, y+height-1, x+width-arcWidth/2, y+height-1);
        g.setColor(PaintUtils.lighten(c,i2));
        g.drawLine(x+arcWidth/2, y+1, x+width-arcWidth/2, y+1);
        g.setColor(PaintUtils.lighten(c,i3));
        g.drawLine(x+width-1, y+arcHeight/2, x+width-1, y+height-arcHeight/2);
        
        g.setColor(c);
        g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }
    
    
    /**
     * Produces a new lightened colour value by a given intensity from a base colour 
     */
    public static Color lighten(Color orig, int intensity) {
        try {
            return new Color(orig.getRed() + intensity,
                orig.getGreen() + intensity,
                orig.getBlue() + intensity);
        } catch (IllegalArgumentException e) {
            if (intensity > 0) {
                // Return an empty colour the colour is brightened by too much
                return new Color(0,0,0,0);
            } else {
                // Return an black if colour is darkened by too much
                return Color.BLACK;
            }
        }
    }
}
