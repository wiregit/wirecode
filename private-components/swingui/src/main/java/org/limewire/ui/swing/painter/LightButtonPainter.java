package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

/**
 * Background painter for a gradient button. 
 */

@Singleton
public class LightButtonPainter extends AbstractPainter<JXButton> {
    
    private final int ANTIALIAS_OFFSET = 1;
    
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
        
        //turn on antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
        
        if (button.getModel().isPressed()) {
            g.setPaint(new GradientPaint(0,0, this.backgroundGradientTop, 0, height, this.backgroundGradientBottom, false));
        }
        else {
            g.setPaint(new GradientPaint(0,0, this.highlightGradientTop, 0, height, this.highlightGradientBottom, false));
        }
        
        g.drawRoundRect(0, 0+ANTIALIAS_OFFSET, width-1, height-1, this.arcWidth, height-1);
        
        g.setColor(this.borderColor);
        g.drawRoundRect(0, 0+ANTIALIAS_OFFSET, width-1, height-1, arcWidth, height-1);
        
        // reset antialiasing propery
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint); 
    }
}