package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
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
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public abstract class AddToListMenu extends JMenu {

    private final Provider<List<File>> selectedFiles;
    private final Provider<LocalFileList> selectedLocalFileList; 
    private final AddToListMenuIcons icons = new AddToListMenuIcons();
    
    /**
     * Constructs an AddToListMenu with all lists enable..
     */
    public AddToListMenu(Provider<List<File>> selectedFiles) {
        this(null, selectedFiles);
    }
    
    /**
     * Constructs an AddToListMenu with all but selectedLocalFileList enabled.
     */
    public AddToListMenu(final Provider<LocalFileList> selectedLocalFileList,
            Provider<List<File>> selectedFiles) {
        super("Add to List");  
        
        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedFiles = selectedFiles;
    }
        
        
    @Inject
    public void initialize(final SharedFileListManager manager){        
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = AddToListMenu.this;
                menu.removeAll();
                // once this is selected, show all the submenus
                manager.getModel().getReadWriteLock().readLock().lock();
                try { 
                    for(SharedFileList fileList : manager.getModel()) {
                        menu.add(new AddListAction(fileList.getCollectionName(), getListIcon(fileList), fileList)).setEnabled(selectedLocalFileList == null || fileList != selectedLocalFileList.get());
                    }
                } finally {
                    manager.getModel().getReadWriteLock().readLock().unlock();
                }
            }
            
        });
        // place holder to get the -> on the parent menu
        add(new JMenuItem(I18n.tr("Public Shared")));
    }
    
    private Icon getListIcon(SharedFileList sharedFileList) {
        if(sharedFileList.isPublic())
            return icons.publicIcon;
        else if(sharedFileList.getFriendIds().size() == 0)
            return icons.unsharedIcon;
        else
            return icons.sharedIcon;
    }
    
    private class AddListAction extends AbstractAction {
        private final LocalFileList localFileList;
        
        public AddListAction(String text, Icon icon, LocalFileList localFileList) {
            super(text);
            putValue(SMALL_ICON, icon);
            this.localFileList = localFileList;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    List<File> selected = new ArrayList<File>(selectedFiles.get());
                    for(File file : selected) {
                        localFileList.addFile(file);
                    }                    
                }
            });
        }
    }
    
    public class AddToListMenuIcons {  
        @Resource
        private Icon publicIcon;
        @Resource
        private Icon unsharedIcon;
        @Resource
        private Icon sharedIcon;
        
        public AddToListMenuIcons() {
            GuiUtils.assignResources(this);
        }
    }
}
