package org.limewire.xmpp.client;

import java.util.Iterator;

public interface LibrarySource {
    public Iterator<File> getFiles();
    public java.io.File getSaveDirectory(String fileName);
}
