package org.limewire.core.api.library;

import java.io.File;

import org.limewire.core.api.URN;

public interface LibraryFileList extends LocalFileList {

    public boolean contains(File file);

    public boolean contains(URN urn);
}
