package org.limewire.xmpp.client;

import java.util.Iterator;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface LibrarySource {
    public Iterator<FileMetaData> getFiles();
    public InputStream readFile(FileMetaData file) throws FileNotFoundException;
    public OutputStream writeFile(FileMetaData file) throws IOException;
}
