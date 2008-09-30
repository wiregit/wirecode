package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.util.BackgroundExecutorService;

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
                    new SharingAddAction(currentList, myLibraryList, Category.AUDIO).actionPerformed(null);
                }
                if(selectVideo) {
                    new SharingAddAction(currentList, myLibraryList, Category.VIDEO).actionPerformed(null);
                }
                if(selectImage) {
                    new SharingAddAction(currentList, myLibraryList, Category.IMAGE).actionPerformed(null);
                }
            }
        });
        reset();
    }
}
