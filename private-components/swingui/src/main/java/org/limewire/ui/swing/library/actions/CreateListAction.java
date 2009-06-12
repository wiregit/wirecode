package org.limewire.ui.swing.library.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;

import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.CreateListPanel;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class CreateListAction extends AbstractAction {
    
    private final Provider<CreateListPanel> createListPanel;
    private final Provider<LibraryPanel> libraryPanel;
    private JDialog dialog;
    
    @Inject
    public CreateListAction(Provider<CreateListPanel> createListPanel, Provider<LibraryPanel> libraryPanel) {
        this.createListPanel = createListPanel;
        this.libraryPanel = libraryPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(dialog == null) {
            dialog = FocusJOptionPane.createDialog(I18n.tr("Create List"), null, createListPanel.get());
        }
        if(!dialog.isVisible()) {
            dialog.setLocationRelativeTo(libraryPanel.get());
            dialog.setVisible(true);
        }
    }
}