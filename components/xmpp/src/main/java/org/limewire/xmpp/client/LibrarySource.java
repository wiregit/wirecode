package org.limewire.xmpp.client;

import java.util.Iterator;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Provides library interactions for users of the xmpp service.
 */
public interface LibrarySource {

    /**
     * @return an <code>Iterator</code> of all of the local shared files
     */
    public Iterator<FileMetaData> getFiles();

    /**
     * Used by the jingle engine to read a local file for writing to
     * a remote user
     * @param file meta data for the file to be sent
     * @return an <code>InputStream</code> to the file
     * @throws FileNotFoundException
     */
    public InputStream readFile(FileMetaData file) throws FileNotFoundException;

    /**
     * Used by the jingle engine to write an received remote file to the
     * local library
     * @param file meta data for the file to be received
     * @return an <code>OutputStream</code> to the file
     * @throws IOException
     */
    public OutputStream writeFile(FileMetaData file) throws IOException;
}
