package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Background painter for a gradient button. 
 */
 //TODO: this needs to get abstracted/cleaned up still. Mostly here so anthony can start testing
public class ButtonPainter extends AbstractPainter<JXButton> {
    
    @Resource
    private int arc;
    @Resource
    private Color borderGradientTop;
    @Resource
    private Color borderGradientBottom;
    @Resource
    private Color backgroundGradientTop;
    @Resource
    private Color backgroundGradientBottom;
    @Resource
    private Color highlightBevel;
    
    public ButtonPainter() {
        GuiUtils.assignResources(this);
    }

    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        //turn on antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // draw the border
        g.setPaint(new GradientPaint(0,0, borderGradientTop, 0, height, borderGradientBottom, false));
        g.drawRoundRect(0, 0, width-1, height-1, arc, arc);
        
        //if the button is pressed
        if(button.getModel().isPressed()) {
            // draw the main gradient
            g.setPaint(new GradientPaint(0, 1, backgroundGradientBottom, 0, height-1, backgroundGradientTop, false));
            g.fillRoundRect(1, 1, width-3, height-3, arc, arc);
            
            // draw the highlight
            g.setPaint(new GradientPaint(0,1, highlightBevel, 0,height-3, backgroundGradientTop, false));
            g.drawRoundRect(1, 1, width-3, height-3, arc, arc);
        } else {           
            // draw the main gradient
            g.setPaint(new GradientPaint(0, 1, backgroundGradientTop, 0, height-1, backgroundGradientBottom, false));
            g.fillRoundRect(1, 1, width-3, height-3, arc, arc);
            
            // draw the highlight
            g.setPaint(new GradientPaint(0,1, highlightBevel, 0,height-3, backgroundGradientBottom, false));
            g.drawRoundRect(1, 1, width-3, height-3, arc, arc);
        }
    }
}
