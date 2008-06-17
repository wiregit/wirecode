package org.jivesoftware.smackx.jingle.file;

import org.jivesoftware.smackx.packet.file.FileDescription;

public interface UserAcceptor {
    public boolean userAccepts(FileDescription.FileContainer file);
}
