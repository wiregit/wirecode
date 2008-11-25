package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.Icon;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.components.LimeComboBox;

/**
 * Painter to be used to extend general font and icon behaviour to all
 *  custom drawn buttons.  Should only be accessed through it's factory
 *  to avoid resource duplication.  
 *  
 *  NOTE: Will not respect icon alignment
 */
public class ButtonForegroundPainter extends AbstractPainter<JXButton> {

    private final Paint pressedForeground;
    private final Paint hoverForeground;
    
    ButtonForegroundPainter() {
        this(null, null);
    }
        
    ButtonForegroundPainter(Paint pressedForeground, Paint hoverForeground) {
        this.pressedForeground = pressedForeground;
        this.hoverForeground = hoverForeground;        
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        
        int textBaseline = object.getHeight() - object.getHeight()/2 
                + g.getFontMetrics().getAscent()/2 
                - object.getInsets().top;
        
        Icon icon = null;
        Paint foreground = null;
                
        if (object.getModel().isPressed()) {
            icon = object.getPressedIcon();
            foreground = pressedForeground; 
        }
        else if (object.getModel().isRollover()) {
            icon = object.getRolloverIcon();
            foreground = hoverForeground;
        }
        else {
            icon = object.getRolloverIcon();
        }
            
        if (foreground == null) {
            foreground = object.getForeground();
        }
        
        g.setFont(object.getFont());
        g.setPaint(foreground);
        
        if (object.getText() != null) {
            g.drawString(object.getText(), object.getInsets().left, textBaseline);
            
            if (icon != null) {
                icon.paintIcon(object, g, object.getWidth() - object.getInsets().right + 3, 
                        object.getHeight()/2 - icon.getIconHeight()/2);
            }
        } 
        // TODO: should use a more OO solution in future
        else if (object instanceof LimeComboBox) {
            
            LimeComboBox box = (LimeComboBox) object;
            
            if (box.getSelectedAction() != null) {
                g.drawString(box.getSelectedAction().getValue("Name").toString(), object.getInsets().left, 
                        textBaseline);
            }
            
            if (icon != null) {
                icon.paintIcon(box, g, box.getWidth() - object.getInsets().right + icon.getIconWidth(), 
                        box.getHeight()/2 - icon.getIconHeight()/2);
            }
        }

        
    }

}
