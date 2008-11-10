package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.ButtonModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Background painter for a gradient button. 
 */

public class GreenButtonPainter implements Painter<JXButton> {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop;
    @Resource private Color backgroundGradientBottom;
    @Resource private Color highlightGradientTop;
    @Resource private Color highlightGradientBottom;
    @Resource private Color borderColour;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
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
        painter.setInsets(new Insets(2,2,2,2));
        painter.setBorderPaint(null);
        painter.setPaintStretched(true);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        
        compoundPainter.setPainters(painter, new BorderPainter(this.arcWidth, this.arcHeight,
                borderColour,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom));
        
        return compoundPainter;
    }
    
    public GreenButtonPainter() {
        GuiUtils.assignResources(this);
                
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour,  this.bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                this.bevelRight, this.bevelBottom);
        
        this.hoveredPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour,  this.bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                this.bevelRight, this.bevelBottom);
        
        this.clickedPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.borderColour,  this.bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                this.bevelRight, this.bevelBottom);
    }
    
    @Override
    public void paint(Graphics2D g, JXButton object, int width, int height) {
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