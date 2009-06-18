package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.FocusManager;
import javax.swing.JDialog;

import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.CreateListPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;


class CreateListAction extends AbstractAction {
    
    private final Provider<CreateListPanel> createListPanel;
    private JDialog dialog;
    
    @Inject
    public CreateListAction(Provider<CreateListPanel> createListPanel) {
        this.createListPanel = createListPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(dialog == null) {
            dialog = FocusJOptionPane.createDialog(I18n.tr("Create List"), null, createListPanel.get());
        }
        if(!dialog.isVisible()) {
            dialog.setLocationRelativeTo(FocusManager.getCurrentManager().getFocusOwner());
            dialog.setVisible(true);
        }
    }
}
