package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileList;

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
        
        new SharingAddAction(userList, myLibraryList, Category.AUDIO).update(musicBox.isSelected());
        new SharingAddAction(userList, myLibraryList, Category.VIDEO).update(videoBox.isSelected());
        new SharingAddAction(userList, myLibraryList, Category.IMAGE).update(imageBox.isSelected());
    }
}
