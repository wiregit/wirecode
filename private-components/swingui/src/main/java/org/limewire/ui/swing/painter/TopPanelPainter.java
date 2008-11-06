package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Background painter for the top panel.
 */
public class TopPanelPainter extends AbstractPainter<JXPanel> {
  
    @Resource private Color borderColor;
    
    @Resource private Color topGradient;
    @Resource private Color bottomGradient;
    
    public TopPanelPainter() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
        g.setPaint(new GradientPaint(0, 0, topGradient, 0, height-1, bottomGradient));
        g.fillRect(0, 0, width, height-1);
        
        g.setColor(borderColor);
        g.drawLine(0, height-1, width, height-1);
    }
}
