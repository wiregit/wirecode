package org.limewire.core.api.library;

import java.io.File;

import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.FileMetaData;

/**
 * A File that is displayed in a library
 */
public interface LocalFileItem extends FileItem {
    File getFile();

    FileMetaData offer(LimePresence limePresence);
}
