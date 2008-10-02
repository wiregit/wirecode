package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Paints the background color for subpanel headers. This is the panel containing
 * the filter box below the search bar
 */
//TODO: make this a singleton and inject it
public class SubpanelPainter extends AbstractPainter<JXPanel> {

    @Resource
    private Color borderColorTop;
    @Resource
    private Color backgroundGradientTop;
    @Resource
    private Color backgroundGradientBottom;
    @Resource
    private Color borderColorBottom;
    
    public SubpanelPainter() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
        // paint the top border
        g.setColor(borderColorTop);
        g.drawLine(0, 0, width, 0);
        
        //paint the gradient
        g.setPaint(new GradientPaint(0,1, backgroundGradientTop, 0, height-2, backgroundGradientBottom));
        g.fillRect(0, 1, width, height-2);
        
        //paint the bottom border
        g.setColor(borderColorBottom);
        g.drawLine(0,height-1, width, height-1);
    }
}
