package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Insets;
import java.awt.geom.Line2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.image.GaussianBlurFilter;
import org.jdesktop.swingx.painter.ShapePainter;
import org.limewire.ui.swing.util.GuiUtils;

public class StatusBarSectionPainter<X> extends ShapePainter<X>{

    @Resource private Color dividerForeground;
    
    public StatusBarSectionPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(true);
        this.setFilters(new GaussianBlurFilter(2));
        
        this.setBorderPaint(this.dividerForeground);
        this.setShape(new Line2D.Float(0,0,0,10));
        this.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        this.setFillVertical(true);
        this.setFillPaint(this.dividerForeground);
        this.setInsets(new Insets(3,0,2,0));
        
        this.setCacheable(true);
    }    

    
}
