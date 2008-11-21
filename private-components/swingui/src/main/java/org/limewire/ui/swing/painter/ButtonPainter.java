package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.ButtonModel;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;

/**
 * Background painter for a gradient button. 
 */
public abstract class ButtonPainter extends AbstractPainter<JXButton> {
    
    protected Painter<JXButton> normalPainter;
    protected Painter<JXButton> clickedPainter;
    protected Painter<JXButton> hoveredPainter;
    
    protected Painter<JXButton> createPainter(Color gradientTop, Color gradientBottom, 
            Paint border, Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight, boolean hasBubble) {
        
        CompoundPainter<JXButton> compoundPainter = new CompoundPainter<JXButton>();
        
        RectanglePainter<JXButton> painter = new RectanglePainter<JXButton>();
        
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
        
        compoundPainter.setPainters(painter, new BorderPainter(arcWidth, arcHeight,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, hasBubble));
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
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