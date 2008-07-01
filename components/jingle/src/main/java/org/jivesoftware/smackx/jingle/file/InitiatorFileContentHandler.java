package org.jivesoftware.smackx.jingle.file;

import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;

public class InitiatorFileContentHandler extends FileContentHandler {
    
    public InitiatorFileContentHandler(StreamInitiation.File file, boolean sending, FileLocator fileLocator) {
        super(getFileContainer(file, sending), getAlwaysAcceptor(), fileLocator);
    }

    protected boolean isInitiator() {
        return true;
    }
    
    private static FileDescription.FileContainer getFileContainer(StreamInitiation.File file, boolean sending) {
        return sending ? new FileDescription.Offer(file) : new FileDescription.Request(file);
    }
}
