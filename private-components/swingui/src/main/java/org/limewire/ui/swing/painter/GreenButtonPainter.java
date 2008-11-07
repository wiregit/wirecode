package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import javax.swing.ButtonModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PaintUtils;

import com.google.inject.Singleton;

/**
 * Background painter for a gradient button. 
 */

@Singleton
public class GreenButtonPainter implements Painter<JXButton> {
        
    @Resource
    private int arcWidth;
    
    @Resource
    private int arcHeight;
    
    @Resource 
    private Color borderColour;
    
    @Resource 
    private Color backgroundGradientTop;
    
    @Resource 
    private Color backgroundGradientBottom;
    
    @Resource
    private Color highlightGradientTop;
    
    @Resource
    private Color highlightGradientBottom;
    
    private final Painter<JXButton> normalPainter;
    private final Painter<JXButton> clickedPainter;
    private final Painter<JXButton> hoveredPainter;
    
    
    private Painter<JXButton> createPainter(Color gradientTop, Color gradientBottom, Color border, 
            Color bevel1, Color bevel2, Color bevel3) {
        
        CompoundPainter<JXButton> compoundPainter = new CompoundPainter<JXButton>();
        
        RectanglePainter<JXButton> painter = new RectanglePainter<JXButton>();
        
        painter.setRounded(true);
        painter.setFillPaint(new GradientPaint(0,0, gradientTop, 0, 1, gradientBottom, false));
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setPaintStretched(true);
        painter.setAntialiasing(true);
        
        compoundPainter.setPainters(painter, new BorderPainter(this.arcWidth, this.arcHeight,
                border, bevel1, bevel2, bevel3));
        
        return compoundPainter;
    }
    
    public GreenButtonPainter() {
        GuiUtils.assignResources(this);
        
        Color bevel1 = PaintUtils.lighten(this.backgroundGradientTop, -10);
        Color bevel2 = PaintUtils.lighten(this.backgroundGradientTop, -20);
        Color bevel3 = PaintUtils.lighten(this.backgroundGradientTop, -30);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour, bevel1, bevel2, bevel3);
        
        this.hoveredPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour, bevel1, bevel2, bevel3);
        
        this.clickedPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.borderColour, bevel1, bevel2, bevel3);
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