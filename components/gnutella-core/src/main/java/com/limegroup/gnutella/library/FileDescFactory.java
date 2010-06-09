package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import org.limewire.io.URNImpl;

import com.limegroup.gnutella.downloader.VerifyingFile;

public interface FileDescFactory {
    
    FileDesc createFileDesc(File file, Set<? extends URNImpl> urns, int index);
    
    IncompleteFileDesc createIncompleteFileDesc(File file,
            Set<? extends URNImpl> urns, int index, String completedName, long completedSize,
            VerifyingFile vf);

}
