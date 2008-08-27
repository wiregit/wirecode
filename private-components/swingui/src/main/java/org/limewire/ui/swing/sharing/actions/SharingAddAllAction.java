package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

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
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(myLibraryList == null || userList == null)
            return;
        
        for(FileItem item : myLibraryList.getModel()) { System.out.println("adding " + item.getName());
            userList.addFile(item.getFile());
        }
    }

}
