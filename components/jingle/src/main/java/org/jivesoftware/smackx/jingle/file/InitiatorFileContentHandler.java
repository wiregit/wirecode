package org.jivesoftware.smackx.jingle.file;

import java.io.File;

public class InitiatorFileContentHandler extends FileContentHandler {
    public InitiatorFileContentHandler(File file, boolean sending, File saveDir) {
        super(file, sending, saveDir);
    }

    protected boolean isInitiator() {
        return true;
    }
}
