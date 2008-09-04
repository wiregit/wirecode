package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.FileMetaData;

public class FileOfferedEvent extends AbstractEDTEvent {
    private final FileMetaData fileMetaData;
    private final String fromJID;
    public FileOfferedEvent(FileMetaData fileMetaData, String fromJID) {
        this.fileMetaData = fileMetaData;
        this.fromJID = fromJID;
    }
    public FileMetaData getFileMetaData() {
        return fileMetaData;
    }
    public String getFromJID() {
        return fromJID;
    }
}
