package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ImportListAction extends AbstractAction {

//    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public ImportListAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super(I18n.tr("Import List..."));
        
//        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: get currently selected NavItem, open dialog??, load M3U list
        throw new NotImplementedException("not implemented");
    }
}
