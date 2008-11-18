package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class StatusBarSectionPainter<X> extends AbstractPainter<X>{

    @Resource private Color dividerForeground;
    
    public StatusBarSectionPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(false);
        this.setFilters(PainterUtils.createSoftenFilter(0.1f));
        this.setCacheable(true);
    }

    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        g.setPaint(this.dividerForeground);
        g.drawLine(width-3, 4, width-3, height-4);
    }    
}
