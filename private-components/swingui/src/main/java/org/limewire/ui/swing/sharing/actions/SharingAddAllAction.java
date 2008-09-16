package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.sharing.table.CategoryFilter;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.FilterList;

public class SharingAddAllAction extends AbstractAction {

    private JCheckBox musicBox;
    private JCheckBox videoBox;
    private JCheckBox imageBox;
    
    private LocalFileList userList;
    private LocalFileList myLibraryList;
    
    public SharingAddAllAction(JCheckBox musicBox, JCheckBox videoBox, JCheckBox imageBox) {
        this.musicBox = musicBox;
        this.videoBox = videoBox;
        this.imageBox = imageBox;
    }
    
    public void setLibrary(LocalFileList libraryList) {
        this.myLibraryList = libraryList;
    }
    
    public void setUserLibrary(LocalFileList userList) {
        this.userList = userList;
    }

    private void reset() {
        musicBox.setSelected(false);
        videoBox.setSelected(false);
        imageBox.setSelected(false);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(myLibraryList == null || userList == null)
            return;
        
        final LocalFileList currentList = userList;
        final boolean selectMusic = musicBox.isSelected();
        final boolean selectVideo = videoBox.isSelected();
        final boolean selectImage = imageBox.isSelected();
        
        BackgroundExecutorService.schedule(new Runnable(){
            public void run() {
                if(selectMusic) {
                    FilterList<LocalFileItem> audio = new FilterList<LocalFileItem>( myLibraryList.getModel(), new CategoryFilter(FileItem.Category.AUDIO));
                    try {
                        audio.getReadWriteLock().readLock().lock();
                        for(LocalFileItem item : audio) {
                            currentList.addFile(item.getFile());
                        }
                    } finally {
                        audio.getReadWriteLock().readLock().unlock();
                    }
                    audio.dispose();
                }
                if(selectVideo) {
                    FilterList<LocalFileItem> video = new FilterList<LocalFileItem>( myLibraryList.getModel(), new CategoryFilter(FileItem.Category.VIDEO));
                    try {
                        video.getReadWriteLock().readLock().lock();
                        for(LocalFileItem item : video) {
                            currentList.addFile(item.getFile());
                        }
                    } finally {
                        video.getReadWriteLock().readLock().unlock();
                    }
                    video.dispose();
                }
                if(selectImage) {
                    FilterList<LocalFileItem> image = new FilterList<LocalFileItem>( myLibraryList.getModel(), new CategoryFilter(FileItem.Category.IMAGE));
                    try {
                        image.getReadWriteLock().readLock().lock();
                        for(LocalFileItem item : image) {
                            currentList.addFile(item.getFile());
                        }
                    } finally {
                        image.getReadWriteLock().readLock().unlock();
                    }
                    image.dispose();
                }
            }
        });
        reset();
    }
}
