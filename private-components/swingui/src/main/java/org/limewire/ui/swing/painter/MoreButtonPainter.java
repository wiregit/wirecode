package org.limewire.ui.swing.painter;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.GuiUtils;

/** Painter for the 'more' button. */
public class MoreButtonPainter extends AbstractButtonPainter {
    
    @Resource
    private int arcWidth;
    @Resource
    private int arcHeight;
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
    @Resource
    private Color highlightBevelBottom;
    
    //Pressed state
    @Resource
    private Color borderGradientTopPressed;
    @Resource
    private Color borderGradientBottomPressed;
    @Resource
    private Color backgroundGradientTopPressed;
    @Resource
    private Color backgroundGradientBottomPressed;
    @Resource
    private Color highlightBevelPressed;
    @Resource
    private Color highlightBevelBottomPressed;
    
    // Hover state
    @Resource
    private Color borderGradientTopHover;
    @Resource
    private Color borderGradientBottomHover;
    @Resource
    private Color backgroundGradientTopHover;
    @Resource
    private Color backgroundGradientBottomHover;
    @Resource
    private Color highlightBevelHover;
    @Resource
    private Color highlightBevelBottomHover;
    
    public MoreButtonPainter() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected void setButtonColors(JXButton button, ButtonColors colors) {    
        colors.arcHeight = arcHeight;
        colors.arcWidth = arcWidth;
        
        if(button.getModel().isPressed()) {
            colors.backgroundTop = backgroundGradientTopPressed;
            colors.backgroundBottom = backgroundGradientBottomPressed;
            colors.bevelTop = highlightBevelPressed;
            colors.bevelBottom = highlightBevelBottomPressed;
            colors.borderTop = borderGradientTopPressed;
            colors.borderBottom = borderGradientBottomPressed;
        } else if(button.getModel().isRollover()) {
            colors.backgroundTop = backgroundGradientTopHover;
            colors.backgroundBottom = backgroundGradientBottomHover;
            colors.bevelTop = highlightBevelHover;
            colors.bevelBottom = highlightBevelBottomHover;
            colors.borderTop = borderGradientTopHover;
            colors.borderBottom = borderGradientBottomHover;            
        } else {
            colors.backgroundTop = backgroundGradientTop;
            colors.backgroundBottom = backgroundGradientBottom;
            colors.bevelTop = highlightBevel;
            colors.bevelBottom = highlightBevelBottom;
            colors.borderTop = borderGradientTop;
            colors.borderBottom = borderGradientBottom;     
        }
    }
}
