package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.filter.Filter;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ClearAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public ClearAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super(I18n.tr("Clear"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LibraryNavItem item = libraryNavigatorPanel.get().getSelectedNavItem();
        int confirmation = FocusJOptionPane.showConfirmDialog(null, getMessage(item), I18n.tr("Clear Files"), JOptionPane.OK_CANCEL_OPTION); 
        if (confirmation == JOptionPane.OK_OPTION) {
            item.getLocalFileList().removeFiles(new ClearFilter());
        }
    }
    
    private String getMessage(LibraryNavItem nav){
    
        switch(nav.getType()) {    
        case LIST:        
            if (isShared(nav)){            
                return I18n.tr("Remove all files from {0}?  This will unshare all of these files.", nav.getDisplayText());
            } else {
                return I18n.tr("Remove all files from {0}?", nav.getDisplayText());                
            }
            case LIBRARY:        
            return I18n.tr("Remove all files from your library?  This will remove all files you've downloaded and unshare every file you are sharing.");
        case PUBLIC_SHARED:  
            if (isShared(nav)){            
                return I18n.tr("Remove all files from {0}?  This will unshare all of these files with the world.", nav.getDisplayText());
            } else {          
                return I18n.tr("Remove all files from {0}?", nav.getDisplayText());
            }
        default:        
            throw new IllegalStateException("unknown type: " + nav.getType());    
        }
    }
    
    private boolean isShared(LibraryNavItem nav){        
        if (nav.getType() == NavType.PUBLIC_SHARED){
            return true;
        }
        
        if(nav.getLocalFileList() instanceof SharedFileList){
            return ((SharedFileList)(nav.getLocalFileList())).getFriendIds().size() > 0;
        }
        
        return false;
    }
    
    private static class ClearFilter implements Filter<LocalFileItem> {
        @Override
        public boolean allow(LocalFileItem t) {
            //always return true to clear all
            return true;
        }        
    }
}
