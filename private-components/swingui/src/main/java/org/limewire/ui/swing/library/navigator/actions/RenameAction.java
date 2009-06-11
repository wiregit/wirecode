package org.limewire.ui.swing.library.navigator.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RenameAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public RenameAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super(I18n.tr("Rename"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: get currently selected NavItem, open dialog??, add files to that list
        throw new NotImplementedException("not implemented");
    }
}
