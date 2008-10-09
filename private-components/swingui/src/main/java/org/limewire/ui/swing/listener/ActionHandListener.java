package org.limewire.ui.swing.listener;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public class ActionHandListener extends MouseAdapter {
    private final ActionListener actionListener;

    public ActionHandListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }
    
    public ActionHandListener() {
        this(null);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        if (!Boolean.TRUE.equals(comp.getClientProperty("limewire.actionHand.disabled"))) {
            comp.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        comp.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(e.getComponent(),
                    ActionEvent.ACTION_PERFORMED, null));
        }
    }
    
    /** Disables or re-enables the action hand from drawing. */
    public static void setActionHandDrawingDisabled(JComponent component, boolean disabled) {
        component.putClientProperty("limewire.actionHand.disabled", disabled);
    }
    
}
