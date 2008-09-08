package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.sharing.table.CategoryFilter;

import ca.odell.glazedlists.FilterList;

public class SharingAddAllAction extends AbstractAction {

    private JCheckBox musicBox;
    private JCheckBox videoBox;
    private JCheckBox imageBox;
    
    private FileList userList;
    private FileList myLibraryList;
    
    public SharingAddAllAction(JCheckBox musicBox, JCheckBox videoBox, JCheckBox imageBox) {
        this.musicBox = musicBox;
        this.videoBox = videoBox;
        this.imageBox = imageBox;
    }
    
    public void setLibrary(FileList libraryList) {
        this.myLibraryList = libraryList;
    }
    
    public void setUserLibrary(FileList userList) {
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
        
        final FileList currentList = userList;
        final boolean selectMusic = musicBox.isSelected();
        final boolean selectVideo = videoBox.isSelected();
        final boolean selectImage = imageBox.isSelected();
        
        //TODO: convert this to an executor service
        Thread t = new Thread(new Runnable(){
            public void run() {
                if(selectMusic) {
                    FilterList<FileItem> audio = new FilterList<FileItem>( myLibraryList.getModel(), new CategoryFilter(FileItem.Category.AUDIO));
                    for(FileItem item : audio) {
                        currentList.addFile(item.getFile());
                    }
                }
                if(selectVideo) {
                    FilterList<FileItem> video = new FilterList<FileItem>( myLibraryList.getModel(), new CategoryFilter(FileItem.Category.VIDEO));
                    for(FileItem item : video) {
                        currentList.addFile(item.getFile());
                    }
                }
                if(selectImage) {
                    FilterList<FileItem> image = new FilterList<FileItem>( myLibraryList.getModel(), new CategoryFilter(FileItem.Category.IMAGE));
                    for(FileItem item : image) {
                        currentList.addFile(item.getFile());
                    }
                }
            }
        });
        t.start();

        reset();
    }
}
