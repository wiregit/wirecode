package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Background painter for a gradient button. 
 */
public class DarkButtonPainter extends ButtonPainter {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    
    @Resource private Color border = PainterUtils.TRASPARENT;
    
    @Resource private Color normalGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color normalGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color normalBevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color normalBevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color normalBevelBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color highlightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color highlightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color highlightBevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color highlightBevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color highlightBevelBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color clickGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color clickGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color clickBevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color clickBevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color clickBevelBottom = PainterUtils.TRASPARENT;
    
    
    
    public DarkButtonPainter() {
        GuiUtils.assignResources(this);
        
        GradientPaint normalGradientRight = new GradientPaint(0,0, this.normalBevelRightGradientTop, 
                0, 1, this.normalBevelRightGradientBottom, false);
        
        GradientPaint hoveredGradientRight = new GradientPaint(0,0, this.highlightBevelRightGradientTop, 
                0, 1, this.highlightBevelRightGradientBottom, false);
        
        GradientPaint clickedGradientRight = new GradientPaint(0,0, this.clickBevelRightGradientTop, 
                0, 1, this.clickBevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.normalGradientTop, this.normalGradientBottom,
                this.border, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT,
                normalGradientRight, this.normalBevelBottom, this.arcWidth, this.arcHeight, false);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.border,  PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                hoveredGradientRight, this.highlightBevelBottom, this.arcWidth, this.arcHeight, false);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.border,  PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                clickedGradientRight, this.clickBevelBottom, this.arcWidth, this.arcHeight, false);
        
        this.setCacheable(false);
    }
}