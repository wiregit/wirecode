package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class RenameAction extends AbstractAction {

    private final Provider<LibraryNavigatorTable> libraryNavigatorTable;
    
    @Inject
    public RenameAction(Provider<LibraryNavigatorTable> libraryNavigatorTable) {
        super(I18n.tr("Rename"));

        this.libraryNavigatorTable = libraryNavigatorTable;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        libraryNavigatorTable.get().setEditable(true);
        libraryNavigatorTable.get().editCellAt(libraryNavigatorTable.get().getSelectedRow(), 0);
    }
}
