package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 *  Paints a Green Message component.
 */
public class GreenMessagePainter<X> extends CompoundPainter<X> {

    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color backgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color borderColour = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop1 = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop2 = PainterUtils.TRASPARENT;
    @Resource private Color bevelLeft = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color bevelBottom = PainterUtils.TRASPARENT;
        
    public GreenMessagePainter() {
        GuiUtils.assignResources(this);
        
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
    }
    
    private void createPainter(Color gradientTop, Color gradientBottom, 
            Paint border, Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight, AccentType accentType) {
                
        RectanglePainter<X> painter = new RectanglePainter<X>();
        
        painter.setRounded(true);
        painter.setFillPaint(new GradientPaint(0,0, gradientTop, 0, 1, gradientBottom, false));
        painter.setRoundWidth(arcWidth);
        painter.setRoundHeight(arcHeight);
        painter.setInsets(new Insets(1,2,2,2));
        painter.setPaintStretched(true);
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
                
        BorderPainter borderPainter = new BorderPainter<X>(arcWidth, arcHeight,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, accentType);
        
        setPainters(painter, borderPainter);
        setCacheable(true);
    }
}