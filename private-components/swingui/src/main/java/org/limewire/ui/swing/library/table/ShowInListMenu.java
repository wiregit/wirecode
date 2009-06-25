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
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ShowInListMenu extends JMenu {

    @Resource
    private Icon publicIcon;
    @Resource
    private Icon unsharedIcon;
    @Resource
    private Icon sharedIcon;
    
    private final Provider<List<File>> selectedFiles;
    private final LibraryPanel libraryPanel;
    
    @Inject
    public ShowInListMenu(final SharedFileListManager manager,
            @LibrarySelected Provider<List<File>> selectedFiles,
            final @LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            LibraryPanel libraryPanel) {
        super("Show in List");
        
        GuiUtils.assignResources(this);
        
        this.selectedFiles = selectedFiles;
        this.libraryPanel = libraryPanel;
        
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = ShowInListMenu.this;
                menu.removeAll();
                
                File selectedFile = ShowInListMenu.this.selectedFiles.get().get(0);
                // once this is selected, show all the submenus
                manager.getModel().getReadWriteLock().readLock().lock();
                try { 
                    for(SharedFileList fileList : manager.getModel()) {
                        if(fileList.contains(selectedFile))
                            menu.add(new ShowAction(fileList.getCollectionName(), getListIcon(fileList), fileList, selectedFile)).setEnabled(fileList != selectedLocalFileList.get());
                    }
                } finally {
                    manager.getModel().getReadWriteLock().readLock().unlock();
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
            return publicIcon;
        else if(sharedFileList.getFriendIds().size() == 0)
            return unsharedIcon;
        else
            return sharedIcon;
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
            libraryPanel.selectLocalFileList(sharedFileList);
            libraryPanel.selectAndScrollTo(selectedFile);
        }
    }
}
