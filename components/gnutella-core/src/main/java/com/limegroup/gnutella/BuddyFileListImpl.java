package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

/**
 * A collection of FileDescs containing files shared with an individual buddy.
 */
public class BuddyFileListImpl extends FileListImpl {

    public BuddyFileListImpl(String name, FileManager fileManager, Set<File> individualFiles) {
        super(name, fileManager, individualFiles);
    }

    /**
     * Buddy lists are based completely on individual files since there's no 
     * directory for files to defaultly reside in. Always add buddy files as 
     * individuals
     */
    @Override
    protected void addAsIndividualFile(FileDesc fileDesc) {
        individualFiles.add(fileDesc.getFile());
    }
    
    /**
     * As long as its not a store file it can be added
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if( fileDesc.getLimeXMLDocuments().size() != 0 && 
                isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } else {
            return true;
        }
    }
}
