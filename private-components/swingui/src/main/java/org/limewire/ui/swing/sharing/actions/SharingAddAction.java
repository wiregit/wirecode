package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.FilterList;

/**
 * Given a userFileList and a category, copies all the files from the library of
 * type category into the user's filelist.
 */
//TODO: clean this up. 
public class SharingAddAction extends AbstractAction {

    private FriendFileList userList;
    private final FileList<LocalFileItem> libraryList;
    private final Category category;
    
    public SharingAddAction(FileList<LocalFileItem> libraryList, Category category) {
        this(null, libraryList, category);
    }
    
    public SharingAddAction(FriendFileList userList, FileList<LocalFileItem> libraryList, Category category) {
        this.userList = userList;
        this.libraryList = libraryList;
        this.category = category;
    }
        
    @Override
    public void actionPerformed(ActionEvent e) {
        if( userList == null || libraryList == null)
            return; 

        if(e == null)
            return;
        JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem)e.getSource();
        saveSetting(userList, category, checkBox.isSelected());
        
        if(checkBox.isSelected()) {
            BackgroundExecutorService.schedule(new Runnable(){
                public void run() {
                    FilterList<LocalFileItem> filter = GlazedListsFactory.filterList(libraryList.getModel(), new CategoryFilter(category));
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
    }
    
    public void update(boolean isSelected) {
        if( userList == null || libraryList == null)
            return; 

        saveSetting(userList, category, isSelected);
        
        if(isSelected) {
            BackgroundExecutorService.schedule(new Runnable(){
                public void run() {
                    FilterList<LocalFileItem> filter = GlazedListsFactory.filterList(libraryList.getModel(), new CategoryFilter(category));
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
    }
    
    public void setUserFileList(FriendFileList userList) {
        this.userList = userList;
    }
    
    public FriendFileList getUserFileList() {
        return userList;
    }
    
    private void saveSetting(FriendFileList friendList, Category category, boolean value) {
        if(category.equals(Category.AUDIO)) {
            userList.setAddNewAudioAlways(value);
        } else if(category.equals(Category.IMAGE)) {
            userList.setAddNewImageAlways(value);
        } else if(category.equals(Category.VIDEO)) {
            userList.setAddNewVideoAlways(value);
        }
    }
}
