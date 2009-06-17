package org.limewire.ui.swing.dnd;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibrarySupport;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.table.LibraryTable;

import com.google.inject.Inject;

import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryTransferHandler extends LocalFileListTransferHandler {
    private final LibraryTable libraryTable;

    private final LibraryNavigatorPanel navigatorComponent;

    @Inject
    public LibraryTransferHandler(LibraryNavigatorPanel navigatorComponent,
            LibraryTable libraryTable, LibrarySupport librarySupport) {
        super(librarySupport);
        this.navigatorComponent = navigatorComponent;
        this.libraryTable = libraryTable;
    }

    @Override
    public LocalFileList getLocalFileList() {
        LibraryNavItem item = navigatorComponent.getSelectedNavItem();
        if (item == null) {
            return null;
        }
        return item.getLocalFileList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public EventSelectionModel<LocalFileItem> getSelectionModel() {
        return (EventSelectionModel<LocalFileItem>) libraryTable.getSelectionModel();
    }
}