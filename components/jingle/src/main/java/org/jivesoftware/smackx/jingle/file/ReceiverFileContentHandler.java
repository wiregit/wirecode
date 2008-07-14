package org.jivesoftware.smackx.jingle.file;

import org.jivesoftware.smackx.packet.file.FileDescription;

public class ReceiverFileContentHandler extends FileContentHandler {
    
    public ReceiverFileContentHandler(FileDescription.FileContainer file, UserAcceptor userAcceptor, FileLocator fileLocator) {
        super(file, userAcceptor, fileLocator);
    }

    protected boolean isInitiator() {
        return false;
    }
}
