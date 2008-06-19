package org.jivesoftware.smackx.jingle.file;

import java.io.File;

import org.jivesoftware.smackx.packet.file.FileDescription;

public class ReceiverFileContentHandler extends FileContentHandler {
    
    public ReceiverFileContentHandler(FileDescription.FileContainer file, UserAcceptor userAcceptor, File saveDir) {
        super(file, userAcceptor, saveDir);
    }

    protected boolean isInitiator() {
        return false;
    }
}
