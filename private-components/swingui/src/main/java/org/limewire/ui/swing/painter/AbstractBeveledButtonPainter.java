package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;

abstract class AbstractBeveledButtonPainter extends AbstractPainter<JXButton> {
    
    private final ButtonColors colors = new ButtonColors();
        
    private final int ANTIALIAS_OFFSET = 1;
    
    public AbstractBeveledButtonPainter() {
        this.setAntialiasing(true);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        setButtonColors(button, colors);
          
        // draw the border
        g.setPaint(new GradientPaint(0,0, colors.borderTop, 0, height, colors.borderBottom, false));
        g.drawRoundRect(0, 0+ANTIALIAS_OFFSET, width-1, height-1, colors.arcWidth, colors.arcHeight);
        
        // draw the main gradient
        g.setPaint(new GradientPaint(0, 1, colors.backgroundTop, 0, height-1, colors.backgroundBottom, false));
        g.fillRoundRect(1, 1+ANTIALIAS_OFFSET, width-3, height-3, colors.arcWidth, colors.arcHeight);
        
        // draw the highlight
        g.setPaint(new GradientPaint(0,1, colors.bevelTop, 0,height-3, colors.bevelBottom, false));
        g.drawRoundRect(1, 1+ANTIALIAS_OFFSET, width-3, height-3, colors.arcWidth, colors.arcHeight);
         
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
