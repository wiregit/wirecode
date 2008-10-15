package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;

abstract class AbstractButtonPainter extends AbstractPainter<JXButton> {
    
    private final ButtonColors colors = new ButtonColors();
        
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        setButtonColors(button, colors);
        
        // get original antialiasing value for reset
        Object origAntiAliasHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        
        //turn on antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
        
        // draw the border
        g.setPaint(new GradientPaint(0,0, colors.borderTop, 0, height, colors.borderBottom, false));
        g.drawRoundRect(0, 0, width-1, height-1, colors.arcWidth, colors.arcHeight);
        
        // draw the main gradient
        g.setPaint(new GradientPaint(0, 1, colors.backgroundTop, 0, height-1, colors.backgroundBottom, false));
        g.fillRoundRect(1, 1, width-3, height-3, colors.arcWidth, colors.arcHeight);
        
        // draw the highlight
        g.setPaint(new GradientPaint(0,1, colors.bevelTop, 0,height-3, colors.bevelBottom, false));
        g.drawRoundRect(1, 1, width-3, height-3, colors.arcWidth, colors.arcHeight);
        
        // reset antialiasing propery
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint); 
    }
    
    protected abstract void setButtonColors(JXButton button, ButtonColors colors);
    
    protected static class ButtonColors {
        protected Color borderTop;
        protected Color borderBottom;
        protected Color backgroundTop;
        protected Color backgroundBottom;
        protected Color bevelTop;
        protected Color bevelBottom;
        protected int arcWidth;
        protected int arcHeight;
    }
}
