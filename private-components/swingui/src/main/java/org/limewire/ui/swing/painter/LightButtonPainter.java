package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Background painter for a gradient button. 
 */
public class LightButtonPainter extends ButtonPainter {
        
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
    
    public LightButtonPainter() {
        GuiUtils.assignResources(this);
                
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour,  PainterUtils.TRASPARENT,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.BUBBLE);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.borderColour,  PainterUtils.TRASPARENT,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.BUBBLE);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.borderColour,  PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.BUBBLE);
        
        this.setCacheable(false);
    }
}