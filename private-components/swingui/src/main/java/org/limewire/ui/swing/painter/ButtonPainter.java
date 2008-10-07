package org.limewire.ui.swing.painter;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Background painter for a gradient button. 
 */
public class ButtonPainter extends AbstractButtonPainter {
    
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
    
    public ButtonPainter() {
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
