package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.ButtonModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PaintUtils;

/**
 * Background painter for a gradient button. 
 */
public class LightButtonPainter extends AbstractPainter<JXButton> {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop;
    @Resource private Color backgroundGradientBottom;
    @Resource private Color highlightGradientTop;
    @Resource private Color highlightGradientBottom;
    @Resource private Color clickGradientTop;
    @Resource private Color clickGradientBottom;
    @Resource private Color borderColour;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRightGradientTop;
    @Resource private Color bevelRightGradientBottom;
    @Resource private Color bevelBottom;
    
    private final Painter<JXButton> normalPainter;
    private final Painter<JXButton> clickedPainter;
    private final Painter<JXButton> hoveredPainter;
    
    
    private Painter<JXButton> createPainter(Color gradientTop, Color gradientBottom, 
            Paint border, Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom) {
        
        CompoundPainter<JXButton> compoundPainter = new CompoundPainter<JXButton>();
        
        RectanglePainter<JXButton> painter = new RectanglePainter<JXButton>();
        
        painter.setRounded(true);
        painter.setFillPaint(new GradientPaint(0,0, gradientTop, 0, 1, gradientBottom, false));
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setInsets(new Insets(1,2,2,2));
        painter.setPaintStretched(true);
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        compoundPainter.setPainters(painter, new BorderPainter(this.arcWidth, this.arcHeight,
                this.borderColour,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom));
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
    
    public LightButtonPainter() {
        GuiUtils.assignResources(this);
                
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour,  PaintUtils.TRASPARENT,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.borderColour,  PaintUtils.TRASPARENT,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.borderColour,  PaintUtils.TRASPARENT, PaintUtils.TRASPARENT, PaintUtils.TRASPARENT, 
                gradientRight, this.bevelBottom);
    }
    
    
    @Override
    public void doPaint(Graphics2D g, JXButton object, int width, int height) {
        ButtonModel model = object.getModel();
        
        if(model.isPressed() || model.isSelected()) {
            this.clickedPainter.paint(g, object, width, height);
        } 
        else if (model.isRollover()) {
            this.hoveredPainter.paint(g, object, width, height);
        } 
        else {
            this.normalPainter.paint(g, object, width, height);
        }        
    }
}