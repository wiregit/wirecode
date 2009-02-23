package org.limewire.ui.swing.painter;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.Action;
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
    private final Paint disabledForeground;
    
    private final Font pressedFont;
    private final Font hoverFont;
    private final Font disabledFont;
    
    
    /**
     * Creates a button foreground painter that does not
     *  change it's font colour based on mouse state.
     *  
     *  NOTE: This should not be used as a shortcut to creating
     *          buttons since it will ignore the default app style
     *          -- instead use the factory
     */
    public ButtonForegroundPainter() {
        this(null, null, null);
    }
       
    /** 
     * Can be used to create a foreground painter with unique pressed and hover font colours
     *  and a right aligned icon.  
     *  
     *  NOTE: Will ignore default app style.  Use the factory if regular behaviour is desired.
     */
    public ButtonForegroundPainter(Paint hoverForeground, Paint pressedForeground, Paint disabledForeground) {
        this(hoverForeground, pressedForeground, disabledForeground, null, null, null);        
    }
    
    /** 
     * Can be used to create a foreground painter with unique pressed and hover font style and colours
     *  with a right aligned icon.  
     *  
     *  NOTE: Will ignore default app style.  Use the factory if regular behaviour is desired.
     */    
    public ButtonForegroundPainter(Paint hoverForeground, Paint pressedForeground, Paint disabledForeground,
            Font pressedFont, Font hoverFont, Font disabledFont) {
        
        this.pressedForeground = pressedForeground;
        this.hoverForeground = hoverForeground;
        this.disabledForeground = disabledForeground;
        
        this.pressedFont = pressedFont;
        this.hoverFont = hoverFont;
        this.disabledFont = disabledFont;
        
        setCacheable(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        
        int textBaseline = (object.getHeight()-3)/2 
                + g.getFontMetrics().getAscent()/2;
        
        Icon icon = null;
        Paint foreground = null;
        Font font = null;
         
        if (!object.isEnabled()) {
            foreground = disabledForeground;
            font = disabledFont;
        }
        else if (object.getModel().isPressed()) {
            icon = object.getPressedIcon();
            foreground = pressedForeground;
            font = pressedFont;
        }
        else if (object.getModel().isRollover() || object.hasFocus()) {
            icon = object.getRolloverIcon();
            foreground = hoverForeground;
            font = hoverFont;
        }
        else {
            icon = object.getIcon();
        }
            
        if (foreground == null) {
            foreground = object.getForeground();
        }
        
        if (font == null) {
            font = object.getFont();
        }
        
        g.setPaint(foreground);
        g.setFont(font);
        
        if (object.getText() != null) {
            g.drawString(object.getText(), object.getInsets().left, textBaseline);
            
            if (icon != null) {
                icon.paintIcon(object, g, 
                        object.getWidth() - icon.getIconWidth()/2 - 10, 
                        object.getHeight()/2 - icon.getIconHeight()/2);
            }
        } 
        // TODO: should use a more OO solution in future
        else if (object instanceof LimeComboBox) {            
            LimeComboBox box = (LimeComboBox) object;            
            Action action = box.getSelectedAction();
            if (action != null) {
                Icon actionIcon = (Icon)action.getValue(Action.SMALL_ICON);
                int leftGap = object.getInsets().left;
                if(actionIcon != null) {
                    actionIcon.paintIcon(box, g, leftGap, (box.getHeight()- actionIcon.getIconHeight())/2);
                    leftGap += actionIcon.getIconWidth() + 4;
                }
                g.drawString((String)action.getValue(Action.NAME), leftGap, textBaseline);
            }
            
            if (icon != null) {
                icon.paintIcon(box, g, box.getWidth() - object.getInsets().right + icon.getIconWidth(), 
                        box.getHeight()/2 - icon.getIconHeight()/2);
            }
        }

        
    }

}
