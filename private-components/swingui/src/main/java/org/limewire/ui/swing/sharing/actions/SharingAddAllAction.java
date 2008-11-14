package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.FilterList;

public class SharingAddAllAction extends AbstractAction {

    private JCheckBox musicBox;
    private JCheckBox videoBox;
    private JCheckBox imageBox;
    
    private FriendFileList userList;
    private LocalFileList myLibraryList;
    
    public SharingAddAllAction(JCheckBox musicBox, JCheckBox videoBox, JCheckBox imageBox) {
        this.musicBox = musicBox;
        this.videoBox = videoBox;
        this.imageBox = imageBox;
    }
    
    public void setLibrary(LocalFileList libraryList) {
        this.myLibraryList = libraryList;
    }
    
    public void setUserLibrary(FriendFileList userList) {
        this.userList = userList;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(myLibraryList == null || userList == null)
            return;
        
        userList.setAddNewAudioAlways(musicBox.isSelected());
        userList.setAddNewImageAlways(imageBox.isSelected());
        userList.setAddNewVideoAlways(videoBox.isSelected());
        
        FriendFileList currentUserList = userList;
         
        List<Category> categories = new ArrayList<Category>();
        
        if(musicBox.isSelected())
            categories.add(Category.AUDIO);
        if(videoBox.isSelected())
            categories.add(Category.VIDEO);
        if(imageBox.isSelected())
            categories.add(Category.IMAGE);
        
        if(categories.size() > 0)
            loadFiles(currentUserList, categories);
    }
    
    private void loadFiles(final FriendFileList currentUserList, final List<Category> categories) {
        BackgroundExecutorService.execute(new Runnable(){
            public void run() {
                for(Category category : categories) {
                    loadFilteredList(category, currentUserList);
                }
            }
        });
    }
    
    private void loadFilteredList(Category category, FriendFileList currentUserList) {
        FilterList<LocalFileItem> filter = GlazedListsFactory.filterList(myLibraryList.getModel(), new CategoryFilter(category));
        try {
            filter.getReadWriteLock().readLock().lock();
            for(LocalFileItem item : filter) {
                currentUserList.addFile(item.getFile());
            }
        } finally {
            filter.getReadWriteLock().readLock().unlock();
        }
        filter.dispose();
    }

}
