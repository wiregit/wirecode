package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXLabel;
import org.limewire.ui.swing.util.GuiUtils;


/**
 * A label that has a clickable text. The text is rendered as an HTML link and
 * the mouse cursor is changed when the mouse hovers over the label.
 */
public class ActionLabel extends JXLabel  {
    
    private MouseListener urlListener;
    
    private PropertyChangeListener listener = null;

    private Action currentAction;
          
    private String text;
    
    private final boolean renderAsLink;
    
    private Color linkColor = UIManager.getColor("Label.foreground");
    
    private final List<ActionListener> actionListeners = new CopyOnWriteArrayList<ActionListener>();
   

    /**
     * Constructs a new clickable label whose text is in the hex color described.
     * 
     * @param action
     * @param color
     */
    public ActionLabel(Action action, boolean renderAsLink) {
        this.renderAsLink = renderAsLink;
        setAction(action);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setHorizontalAlignment(SwingConstants.LEFT);
    }
    
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }
    
    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        if(renderAsLink) {
            String htmlString = null;
            if(text != null) {
                htmlString = ("<html><a href=\"\"" + 
                    (linkColor != null ? "color=\"#" + GuiUtils.colorToHex(linkColor) + "\"" : "") +
                    ">" + text + "</a></html>");
            }
            super.setText(htmlString);
        } else {
            super.setText(text);
        }
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
        installListener(GuiUtils.getActionHandListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                currentAction.actionPerformed(e);
                for(ActionListener listener : actionListeners) {
                    listener.actionPerformed(e);
                }
            }
        }));
        updateLabel(currentAction, null);
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
                    updateLabel(null, evt);                    
                }
            };
        }
        return listener;
    }
    
    /*
     * Update label text based on action event
     */
    public void updateLabel(Action action, PropertyChangeEvent evt) {
        if(evt == null) {
            assert action != null;
            String display = (String) currentAction.getValue(Action.NAME);
            setIcon((Icon) currentAction.getValue(Action.SMALL_ICON));
            setToolTipText((String) currentAction.getValue(Action.SHORT_DESCRIPTION));
            if(display != null) {
                setText(display);
            }
        } else {
            assert action == null;
            String id = evt.getPropertyName();
            if(id.equals(Action.NAME)) {
                setText((String)evt.getNewValue());
            } else if(id.equals(Action.SMALL_ICON)) {
                setIcon((Icon)evt.getNewValue());
            } else if(id.equals(Action.SHORT_DESCRIPTION)) {
                setToolTipText((String)evt.getNewValue());
            }
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