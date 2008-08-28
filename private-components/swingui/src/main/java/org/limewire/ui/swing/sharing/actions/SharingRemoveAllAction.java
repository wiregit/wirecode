package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

import ca.odell.glazedlists.EventList;

/**
 * Removes all the files in eventList from the given FileList.
 */
public class SharingRemoveAllAction extends AbstractAction {

    private FileList fileList;
    private EventList<FileItem> eventList;
    
    public SharingRemoveAllAction(FileList fileList, EventList<FileItem> eventList) {
        super("Yes");
        this.fileList = fileList;
        this.eventList = eventList;
    }
    
    public void setFileList(FileList newFileList) {
        this.fileList = newFileList;
    }
    
    public void setEventList(EventList<FileItem> eventList) {
        this.eventList = eventList;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: this needs to be moved off the swing thread
        FileItem[] items = eventList.toArray(new FileItem[eventList.size()]);
        for(FileItem item : items) {
            fileList.removeFile(item.getFile());
        }            
    }
}
