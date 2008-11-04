package org.limewire.ui.swing.painter;

import java.awt.Color;

import javax.swing.ButtonModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

/**
 * Background painter for a gradient button. 
 */

@Singleton
public class GreenButtonPainter extends AbstractButtonPainter {
        
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
    
    public GreenButtonPainter() {
        GuiUtils.assignResources(this);
    }
    
    // TODO: Proper shading
    @Override
    protected void setButtonColours(JXButton button, ButtonColours colours) {    
        colours.arcHeight = this.arcHeight;
        colours.arcWidth = this.arcWidth;
        
        ButtonModel model = button.getModel();

        //isSelected() for toggle buttons
        if(model.isPressed() || model.isSelected()) {
            colours.borderColour = this.borderColour;
            colours.backgroundGradientTop = this.backgroundGradientTop;
            colours.backgroundGradientBottom = this.backgroundGradientBottom;
            colours.i1 = 255;
            colours.i2 = 255;
            colours.i3 = 255;
        } 
        else {
            colours.borderColour = this.borderColour;
            colours.backgroundGradientTop = this.highlightGradientTop;
            colours.backgroundGradientBottom = this.highlightGradientBottom;
            colours.i1 = 255;
            colours.i2 = 255;
            colours.i3 = 255;
        }
    }
}