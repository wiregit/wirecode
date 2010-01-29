package org.limewire.ui.swing.activation;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.table.TableCellEditor;

import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.IconButton;

public class ActivationInfoAction extends AbstractAction {

    private final TableCellEditor editor;
    private final JComponent parent;

    public ActivationInfoAction(TableCellEditor editor, JComponent parent) {
        this.editor = editor;
        this.parent = parent;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() instanceof IconButton) {
            ((IconButton) e.getSource()).resetDefaultCursor();
        }
        
        Object item = editor.getCellEditorValue();
        if (item != null && item instanceof ActivationItem) {
            ActivationItem activationItem = (ActivationItem) item;
            String message = ActivationUtilities.getStatusMessage(activationItem);
            FocusJOptionPane.showMessageDialog(parent.getRootPane().getParent(), message, activationItem.getLicenseName(), JOptionPane.OK_OPTION);
            editor.cancelCellEditing();
        }
    }

}
