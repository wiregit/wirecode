package org.limewire.ui.swing.painter;

import java.awt.Color;

import javax.swing.ButtonModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PaintUtils;

import com.google.inject.Singleton;

/**
 * Background painter for a gradient button. 
 */

@Singleton
public class LightButtonPainter extends AbstractButtonPainter {
        
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
    
    public LightButtonPainter() {
        GuiUtils.assignResources(this);

        props.arcHeight = this.arcHeight;
        props.arcWidth = this.arcWidth;

        props.border = this.borderColour;
        
        props.borderBevel1 = PaintUtils.lighten(this.borderColour, 60);
        props.borderBevel2 = PaintUtils.lighten(this.borderColour, 80);
        props.borderBevel3 = PaintUtils.lighten(this.borderColour, 100);
    }
    
    @Override
    protected void setButtonColours(JXButton button) {    
        
        ButtonModel model = button.getModel();

        //isSelected() for toggle buttons
        if(model.isPressed() || model.isSelected()) {
            props.backgroundGradientTop = this.backgroundGradientTop;
            props.backgroundGradientBottom = this.backgroundGradientBottom;
        } 
        else {
            props.backgroundGradientTop = this.highlightGradientTop;
            props.backgroundGradientBottom = this.highlightGradientBottom;
        }
    }
}