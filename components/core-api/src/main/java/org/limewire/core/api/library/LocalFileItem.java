package org.limewire.core.api.library;

import java.io.File;

import com.limegroup.gnutella.FileDetails;

/**
 * A File that is displayed in a library
 */
public interface LocalFileItem extends FileItem {
    File getFile();
    
    FileDetails getFileDetails();
}
