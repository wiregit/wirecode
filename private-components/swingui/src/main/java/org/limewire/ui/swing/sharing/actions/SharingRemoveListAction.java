package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JList;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

/**
 *  An action for a button to unshare an item in a list
 */
public class SharingRemoveListAction extends AbstractAction {

    private FileList fileList;
    private JList list;
    
    public SharingRemoveListAction(FileList fileList, JList list) {
        super("");
        
        this.fileList = fileList;
        this.list = list;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        int index = list.getSelectedIndex();
        if(index > -1) {
            FileItem item = (FileItem) list.getModel().getElementAt(index);
            fileList.removeFile(item.getFile());
        }
    }
}
