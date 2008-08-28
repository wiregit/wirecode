package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;

/**
 *  An action for a button to unshare an item in a list
 */
public class SharingRemoveListAction extends AbstractAction {

    private ImageList list;
    
    public SharingRemoveListAction(ImageList list) {
        super("");
        
        this.list = list;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        int index = list.getSelectedIndex();
        if(index > -1) {
            ImageListModel model = (ImageListModel) list.getModel();
            model.removeFile(index);
        }
    }
}
