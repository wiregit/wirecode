package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PaintUtils;

import com.google.inject.Singleton;

/**
 * Background painter for a gradient button. 
 */

@Singleton
public class LightButtonPainter extends AbstractPainter<JXButton> {
        
    @Resource
    private int arcHeight;
    
    @Resource
    private int arcWidth;
    
    @Resource 
    private Color borderColor;
    
    @Resource 
    private Color backgroundGradientTop;
    
    @Resource 
    private Color backgroundGradientBottom;
    
    @Resource
    private Color highlightGradientTop;
    
    @Resource
    private Color highlightGradientBottom;
    
    public LightButtonPainter() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        
        // get original antialiasing value for reset
        Object origAntiAliasHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        //turn off antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);    
        
        if (button.getModel().isPressed()) {
            g.setPaint(new GradientPaint(0,0, this.backgroundGradientTop, 0, height, this.backgroundGradientBottom, false));
        }
        else {
            g.setPaint(new GradientPaint(0,0, this.highlightGradientTop, 0, height, this.highlightGradientBottom, false));
        }
        
        g.fillRoundRect(0, 0, width-1, height-1, this.arcWidth, this.arcHeight);
        
        
        // Draw shading
        g.setColor(PaintUtils.lighten(this.borderColor,60));
        g.drawRoundRect(0, 1, width-1, height-3, this.arcWidth, this.arcHeight);
        g.drawLine(this.arcWidth/2, height-2, width-this.arcWidth/2, height-2);
        g.setColor(PaintUtils.lighten(this.borderColor,100));
        g.drawLine(this.arcWidth/2, 1, width-this.arcWidth/2, 1);
        g.setColor(PaintUtils.lighten(this.borderColor,80));
        g.drawLine(width-2, this.arcHeight/2, width-2, height-this.arcHeight/2);
        
        g.setColor(this.borderColor);
        g.drawRoundRect(0, 0, width-1, height-1, this.arcWidth, this.arcHeight);
        
        // reset antialiasing propery
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint); 
    }
}