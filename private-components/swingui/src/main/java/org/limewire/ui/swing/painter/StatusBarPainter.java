package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Background painter for the status bar.
 */
public class StatusBarPainter extends AbstractPainter<JXPanel> {
  
    @Resource private Color topBorderColor;
    @Resource private Color belowTopBorderColor;
    
    @Resource private Color topGradient;
    @Resource private Color bottomGradient;
    
    public StatusBarPainter() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
        //first line
        g.setColor(topBorderColor);
        g.drawLine(0,0,width,0);
        
        //second line
        g.setColor(belowTopBorderColor);
        g.drawLine(0, 1, width, 1);
        
        //fill rest of the space with gradient
        g.setPaint(new GradientPaint(0, 0, topGradient, 0, height-2, bottomGradient));
        g.fillRect(0, 2, width, height-2);
    }
}
