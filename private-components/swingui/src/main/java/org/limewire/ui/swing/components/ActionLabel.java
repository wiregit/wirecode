package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.limewire.ui.swing.util.GuiUtils;


/**
 * A label that has a clickable text. The text is rendered as an HTML link and
 * the mouse cursor is changed when the mouse hovers over the label.
 */
public class ActionLabel extends JLabel  {
    
    private MouseListener urlListener;
    
    private PropertyChangeListener listener = null;

    private Action currentAction;
    
    private String url = "";
      
    private String text;
    
    private Color linkColor = UIManager.getColor("Label.foreground");
   

    /**
     * Constructs a new clickable label whose text is in the hex color described.
     * 
     * @param action
     * @param color
     */
    public ActionLabel(Action action) {
        setAction(action);
       
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        String htmlString = null;
        if(text != null) {
            htmlString = ("<html><a href=\"" + url + "\"" + 
                (linkColor != null ? "color=\"#" + GuiUtils.colorToHex(linkColor) + "\"" : "") +
                ">" + text + "</a></html>");
        }

        super.setText(htmlString);
    }
   
    
    public void setAction(Action action) {
        // remove old listener
        Action oldAction = getAction();
        if (oldAction != null) {
            oldAction.removePropertyChangeListener(getListener());
        }

        // add listener
        currentAction = action;
        currentAction.addPropertyChangeListener(getListener());
        installListener(GuiUtils.getActionHandListener(action));
        updateLabel();
    }
    
    
    public void setColor(Color fg) {
        linkColor = fg;
        setText(text);
    }
    
    public Action getAction(){
        return currentAction;
    }
       
    private PropertyChangeListener getListener() {
        if (listener == null) {
            listener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) { 
                    //update label properties
                    updateLabel();
                    
                }
            };
        }
        return listener;
    }
    
    /*
     * Update label text based on action event
     */
    public void updateLabel() {
        if (currentAction != null) {
            String display = (String) currentAction.getValue(Action.NAME);

            setIcon((Icon) currentAction.getValue(Action.SMALL_ICON));
            setToolTipText((String) currentAction.getValue(Action.SHORT_DESCRIPTION));

            // display
            setText(display);
        } else {
            setText(text);
            setToolTipText(url);
        }
    }
   
    private void installListener(MouseListener listener) {
        if (urlListener != null) {
            removeMouseListener(urlListener);
        }
        urlListener = listener;
        addMouseListener(urlListener);
    }
}