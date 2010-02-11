package org.limewire.ui.swing.activation;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.table.TableCellEditor;

import org.limewire.activation.api.ActivationItem;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.IconButton;

public class ActivationInfoAction extends AbstractAction {

    private final TableCellEditor editor;
    private final JComponent parent;
    private final Application application;

    public ActivationInfoAction(TableCellEditor editor, JComponent parent, Application application) {
        this.editor = editor;
        this.parent = parent;
        this.application = application;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() instanceof IconButton) {
            ((IconButton) e.getSource()).resetDefaultCursor();
        }
        
        Object item = editor.getCellEditorValue();
        if (item instanceof ActivationItem) {
            ActivationItem activationItem = (ActivationItem) item;
            JComponent message = ActivationUtilities.getStatusMessage(activationItem, application);
            FocusJOptionPane.showMessageDialog(parent.getRootPane().getParent(), message, activationItem.getLicenseName(), JOptionPane.OK_OPTION);
            editor.cancelCellEditing();
        }
    }

}
