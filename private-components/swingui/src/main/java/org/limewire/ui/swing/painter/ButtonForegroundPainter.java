package org.limewire.ui.swing.painter;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.Action;
import javax.swing.Icon;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.util.FontUtils;

/**
 * Painter to be used to extend general font and icon behaviour to all
 *  custom drawn buttons.  Should only be accessed through it's factory
 *  to avoid resource duplication.  
 *  
 *  NOTE: Will not respect icon alignment
 *  
 *  TODO: COmment again
 */
public class ButtonForegroundPainter extends AbstractPainter<JXButton> {

    private final Paint pressedForeground;
    private final Paint hoverForeground;
    private final Paint disabledForeground;
    
    private final FontTransform pressedTransform;
    private final FontTransform hoverTransform;
    private final FontTransform disabledTransform;
    
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
     * Can be used to create a foreground painter with unique overlaid pressed and hover font colours
     *  and a right aligned icon.  
     *  
     *  NOTE: Will ignore default app style.  Use the factory if regular behaviour is desired.
     */
    public ButtonForegroundPainter(Paint hoverForeground, Paint pressedForeground, Paint disabledForeground) {
        this(hoverForeground, pressedForeground, disabledForeground, 
                FontTransform.NO_CHANGE, FontTransform.NO_CHANGE, FontTransform.NO_CHANGE);        
    }
    
    /** 
     * Can be used to create a foreground painter with unique overlaid pressed and hover font style and colours
     *  with a right aligned icon.  
     *  
     *  NOTE: Will ignore default app style.  Use the factory if regular behaviour is desired.
     */    
    public ButtonForegroundPainter(Paint hoverForeground, Paint pressedForeground, Paint disabledForeground,
            FontTransform hoverTransform, FontTransform pressedTransform, FontTransform disabledTransform) {
        
        this.pressedForeground = pressedForeground;
        this.hoverForeground = hoverForeground;
        this.disabledForeground = disabledForeground;
        
        this.pressedTransform = pressedTransform;
        this.hoverTransform = hoverTransform;
        this.disabledTransform = disabledTransform;
        
        setCacheable(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        
        int textBaseline = (object.getHeight()-3)/2 
                + g.getFontMetrics().getAscent()/2;
        
        Icon icon = null;
        Paint foreground = null;
        FontTransform fontTransform = FontTransform.NO_CHANGE;
         
        if (!object.isEnabled()) {
            foreground = disabledForeground;
            fontTransform = disabledTransform;
        }
        else if (object.getModel().isPressed() || object.getModel().isSelected()) {
            icon = object.getPressedIcon();
            foreground = pressedForeground;
            fontTransform = pressedTransform;
        }
        else if (object.getModel().isRollover() || object.hasFocus()) {
            icon = object.getRolloverIcon();
            foreground = hoverForeground;
            fontTransform = hoverTransform;
        }
        else {
            icon = object.getIcon();
        }
            
        if (foreground == null) {
            foreground = object.getForeground();
        }
        
        Font font = object.getFont();
        
        // TODO: Should be cached.
        switch (fontTransform) {
            case NO_CHANGE :
                break;
            case ADD_UNDERLINE :
                font = FontUtils.deriveUnderline(font, true);
                break;
            case REMOVE_UNDERLINE :
                font = FontUtils.deriveUnderline(font, false);
                break;
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
    
    public enum FontTransform {
        NO_CHANGE, ADD_UNDERLINE, REMOVE_UNDERLINE;
    }

}
