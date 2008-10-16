package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Background painter for a gradient button. 
 */
public class PopupButtonPainter extends AbstractPainter<JXButton> {
    
    @Resource
    private int arcWidth;
    @Resource
    private Color backgroundPressed;
    @Resource
    private Color backgroundRollover;
    
    public PopupButtonPainter() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        // get original antialiasing value for reset
        Object origAntiAliasHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        
        //turn on antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
        
        if (button.getModel().isPressed()) {
            g.setColor(this.backgroundPressed);
            g.fillRoundRect(0, 0, width-1, height-1, arcWidth, height-1);
        }
        else if (button.getModel().isRollover()) {
            g.setColor(this.backgroundRollover);
            g.fillRoundRect(0, 0, width-1, height-1, arcWidth, height-1);
        }
        
        // reset antialiasing propery
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint); 
    }
}