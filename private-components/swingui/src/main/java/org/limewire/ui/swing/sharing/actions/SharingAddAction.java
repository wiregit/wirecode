package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.sharing.table.CategoryFilter;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.FilterList;

/**
 * Given a userFileList and a category, copies all the files from the library of
 * type category into the user's filelist.
 */
public class SharingAddAction extends AbstractAction {

    private LocalFileList userList;
    private final FileList<LocalFileItem> libraryList;
    private final Category category;
    
    public SharingAddAction(FileList<LocalFileItem> libraryList, Category category) {
        this(null, libraryList, category);
    }
    
    public SharingAddAction(LocalFileList userList, FileList<LocalFileItem> libraryList, Category category) {
        this.userList = userList;
        this.libraryList = libraryList;
        this.category = category;
    }
        
    @Override
    public void actionPerformed(ActionEvent e) {
        if( userList == null || libraryList == null)
            return; 
        BackgroundExecutorService.schedule(new Runnable(){
            public void run() {
                FilterList<LocalFileItem> filter = new FilterList<LocalFileItem>(libraryList.getModel(), new CategoryFilter(category));
                try {
                    filter.getReadWriteLock().readLock().lock();
                    for(LocalFileItem item : filter) {
                        userList.addFile(item.getFile());
                    }
                } finally {
                    filter.getReadWriteLock().readLock().unlock();
                }
                filter.dispose();
            }
        });
    }
    
    public void setUserFileList(LocalFileList userList) {
        this.userList = userList;
    }
}
