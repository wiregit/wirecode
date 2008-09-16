package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

/**
 * Removes all the files in eventList from the given FileList.
 */
public class SharingRemoveAllAction extends AbstractAction {

    private LocalFileList fileList;
    private EventList<LocalFileItem> eventList;
    
    public SharingRemoveAllAction(LocalFileList fileList, EventList<LocalFileItem> eventList) {
        super(I18n.tr("Yes"));
        this.fileList = fileList;
        this.eventList = eventList;
    }
    
    public void setFileList(LocalFileList newFileList) {
        this.fileList = newFileList;
    }
    
    public void setEventList(EventList<LocalFileItem> eventList) {
        this.eventList = eventList;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final LocalFileItem[] items = eventList.toArray(new LocalFileItem[eventList.size()]);
        BackgroundExecutorService.schedule(new Runnable(){
            public void run() {
                for(LocalFileItem item : items) {
                    fileList.removeFile(item.getFile());
                }             
            }
        });    
    }
}
