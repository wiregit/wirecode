package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public abstract class ShowInListMenu extends JMenu {
    
    private final Provider<List<File>> selectedFiles;
    private LibraryPanel libraryPanel;
    private final ShowInListMenuIcons icons = new ShowInListMenuIcons();
    private Provider<Navigator> navigatorProvider;
    private Provider<LocalFileList> selectedLocalFileList;
    
    /**
     * Constructs a ShowInListMenu with all items enabled.  Initialize must be called after construction (except for injected subclasses).
     */
    public ShowInListMenu(Provider<List<File>> selectedFiles) {
        this(selectedFiles, null);
    }
 
    /**
     * Constructs a ShowInListMenu with all items but selectedLocalFileList enabled.  Initialize must be called after construction (except for injected subclasses).
     */
    public ShowInListMenu(Provider<List<File>> selectedFiles,
            final Provider<LocalFileList> selectedLocalFileList) {
        super(I18n.tr("Show in List"));        
        
        this.selectedFiles = selectedFiles;
        this.selectedLocalFileList = selectedLocalFileList;
    }
    
    @Inject
    public void initialize(final SharedFileListManager manager,
        LibraryPanel libraryPanel, Provider<Navigator> navigatorProvider){
        
        this.libraryPanel = libraryPanel;
        this.navigatorProvider = navigatorProvider;
        
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = ShowInListMenu.this;
                menu.removeAll();
                
                File selectedFile = null;
                if(!selectedFiles.get().isEmpty()) {
                    selectedFiles.get().get(0);
                    // once this is selected, show all the submenus
                    manager.getModel().getReadWriteLock().readLock().lock();
                    try { 
                        for(SharedFileList fileList : manager.getModel()) {
                            if(fileList.contains(selectedFile))
                                menu.add(new ShowAction(fileList.getCollectionName(), getListIcon(fileList), fileList, selectedFile)).setEnabled(selectedLocalFileList == null || fileList != selectedLocalFileList.get());
                        }
                    } finally {
                        manager.getModel().getReadWriteLock().readLock().unlock();
                    }
                }
                if(menu.getMenuComponentCount() < 1)
                    menu.add(new JMenuItem(I18n.tr("empty"))).setEnabled(false);
            }
            
        });
        // place holder to get the -> on the parent menu
        add(new JMenuItem(I18n.tr("empty")));
    }
    
    private Icon getListIcon(SharedFileList sharedFileList) {
        if(sharedFileList.isPublic())
            return icons.publicIcon;
        else if(sharedFileList.getFriendIds().size() == 0)
            return icons.unsharedIcon;
        else
            return icons.sharedIcon;
    }
    
    private class ShowAction extends AbstractAction {
        private final SharedFileList sharedFileList;
        private final File selectedFile;
        
        public ShowAction(String text, Icon icon, SharedFileList sharedFileList, File selectedFile) {
            super(text);
            putValue(SMALL_ICON, icon);
            this.sharedFileList = sharedFileList;
            this.selectedFile = selectedFile;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, LibraryMediator.NAME);
            item.select();
            libraryPanel.selectLocalFileList(sharedFileList);
            libraryPanel.selectAndScrollTo(selectedFile);
        }
    }
    
    private static class ShowInListMenuIcons {
        @Resource
        private Icon publicIcon;
        @Resource
        private Icon unsharedIcon;
        @Resource
        private Icon sharedIcon;
        
        public ShowInListMenuIcons() {
            GuiUtils.assignResources(this);
        }
    }
}
