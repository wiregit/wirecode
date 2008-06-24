package org.jivesoftware.smackx.jingle.file;

import java.io.File;

import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;

public class InitiatorFileContentHandler extends FileContentHandler {
    public InitiatorFileContentHandler(java.io.File file, boolean sending, java.io.File saveDir) {
        super(file, sending, saveDir);
    }
    
    public InitiatorFileContentHandler(StreamInitiation.File file, boolean sending, java.io.File saveDir) {
        super(getFileContainer(file, sending), getAlwaysAcceptor(), saveDir);
    }

    protected boolean isInitiator() {
        return true;
    }
    
    private static FileDescription.FileContainer getFileContainer(StreamInitiation.File file, boolean sending) {
        return sending ? new FileDescription.Offer(new FileMediaNegotiator.JingleFile(file)) : new FileDescription.Request(new FileMediaNegotiator.JingleFile(file));
    }
}
