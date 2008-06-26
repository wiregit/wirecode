package org.jivesoftware.smackx.jingle.file;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jivesoftware.smackx.packet.StreamInitiation;

public interface FileLocator {
    public InputStream readFile(StreamInitiation.File file) throws FileNotFoundException;
    public OutputStream writeFile(StreamInitiation.File file) throws IOException;
}
