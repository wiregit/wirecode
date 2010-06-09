package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import org.limewire.io.URNImpl;

import com.limegroup.gnutella.downloader.VerifyingFile;

public class IncompleteFileCollectionStub extends AbstractFileCollectionStub implements IncompleteFileCollection {

    @Override
    public void addIncompleteFile(File incompleteFile, Set<? extends URNImpl> urns, String name,
            long size, VerifyingFile vf) {
        // No-op.
    }

    @Override
    public boolean isFileAllowed(File file) {
        return true;
    }

}
