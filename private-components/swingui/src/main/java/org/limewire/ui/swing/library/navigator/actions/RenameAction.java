package org.limewire.ui.swing.library.navigator.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RenameAction extends AbstractAction {

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
        //TODO: figure out how to select the textField in the editor here
//        libraryNavigatorTable.get().requestFocus();
//        TableCellEditor editor = libraryNavigatorTable.get().getCellEditor(libraryNavigatorTable.get().getSelectedRow(), 0);
//        editor.shouldSelectCell(new ListSelectionEvent(this, libraryNavigatorTable.get().getSelectedRow(), libraryNavigatorTable.get().getSelectedRow(), true));
    }
}
