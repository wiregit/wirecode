package org.jivesoftware.smackx.jingle.file;

import java.io.File;

import org.jivesoftware.smackx.packet.StreamInitiation;

public interface FileLocator {
    public File getFile(StreamInitiation.File file);
}
