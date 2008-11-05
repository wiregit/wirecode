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

    protected final ButtonProps props = new ButtonProps();
   
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        
        this.setButtonColours(button);
        
        // get original antialiasing value for reset
        Object origAntiAliasHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        //turn off antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);    
        
        g.setPaint(new GradientPaint(0,0, props.backgroundGradientTop, 0, height, props.backgroundGradientBottom, false));
        
        g.fillRoundRect(0, 0, width-1, height-1, props.arcWidth, props.arcHeight);
        
        
        PaintUtils.drawRoundedBorder(g, 0, 0, width-1, height-1, 
                props.arcWidth, props.arcHeight, props.border,
                props.borderBevel1, props.borderBevel2, props.borderBevel3);
        
        // reset antialiasing propery
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint); 
    }
    
    protected abstract void setButtonColours(JXButton button);
    
    protected static class ButtonProps {
        protected int arcWidth;
        protected int arcHeight;
        protected Color border;
        protected Color borderBevel1;
        protected Color borderBevel2;
        protected Color borderBevel3;
        protected Color backgroundGradientTop;
        protected Color backgroundGradientBottom;
    }
}