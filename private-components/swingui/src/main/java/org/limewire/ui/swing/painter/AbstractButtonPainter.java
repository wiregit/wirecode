package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.PaintUtils;

/**
 * Background painter for a gradient button. 
 */

abstract class AbstractButtonPainter extends AbstractPainter<JXButton> {

    private final ButtonColours colours = new ButtonColours();
   
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        
        this.setButtonColours(button, colours);
        
        // get original antialiasing value for reset
        Object origAntiAliasHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        //turn off antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);    
        
        g.setPaint(new GradientPaint(0,0, colours.backgroundGradientTop, 0, height, colours.backgroundGradientBottom, false));
        
        g.fillRoundRect(0, 0, width-1, height-1, colours.arcWidth, colours.arcHeight);
        
        
        PaintUtils.drawRoundedBorder(g, 0, 0, width-1, height-1, 
                colours.arcWidth, colours.arcHeight, colours.borderColour,
                colours.i1, colours.i2, colours.i3);
        
        // reset antialiasing propery
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint); 
    }
    
    protected abstract void setButtonColours(JXButton button, ButtonColours colors);
    
    protected static class ButtonColours {
        protected int arcWidth;
        protected int arcHeight;
        protected Color borderColour;
        protected Color backgroundGradientTop;
        protected Color backgroundGradientBottom;
        protected int i1, i2, i3;
    }
}