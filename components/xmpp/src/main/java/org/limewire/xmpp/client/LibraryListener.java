package org.limewire.xmpp.client;

/**
 * Called when a <code>library</code> result IQ is recieved from a remote user,
 * containing a list of their shared files.
 */
public interface LibraryListener {
     public void fileAdded(FileMetaData f);
}
