package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AddToListMenu extends JMenu {
    
    private final Provider<List<File>> selectedFiles;
    
    @Inject
    public AddToListMenu(final SharedFileListManager manager,
            @LibrarySelected Provider<List<File>> selectedFiles) {
        super("Add to List");
        
        this.selectedFiles = selectedFiles;
        
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = AddToListMenu.this;
                menu.removeAll();
                // once this is selected, show all the submenus
                manager.getModel().getReadWriteLock().readLock().lock();
                try { 
                    for(SharedFileList fileList : manager.getModel()) {
                        menu.add(new AddListAction(fileList.getCollectionName(), fileList));
                    }
                } finally {
                    manager.getModel().getReadWriteLock().readLock().unlock();
                }
            }
            
        });
        // place holder to get the -> on the parent menu
        add(new JMenuItem(I18n.tr("Public Shared")));
    }
    
    private class AddListAction extends AbstractAction {
        private final LocalFileList localFileList;
        
        public AddListAction(String text, LocalFileList localFileList) {
            super(text);
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
}
