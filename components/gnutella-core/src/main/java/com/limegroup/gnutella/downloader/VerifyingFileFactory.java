package com.limegroup.gnutella.downloader;

/** A factory for creating VerifyingFiles. */
public class VerifyingFileFactory {
    
    private final DiskController diskController;
    
    /** Constructs a VerifyingFileFactory that uses the given DiskController when constructing VerifyingFiles. */
    public VerifyingFileFactory(DiskController diskController) {
        this.diskController = diskController;
    }

    /** Constructs a verifying file with the given completed size. */
    public VerifyingFile createVerifyingFile(long completedSize) {
        return new VerifyingFile(completedSize, diskController);
    }

    /** Constructs a verifying file for testing. */
    public VerifyingFile createVerifyingFile() {
        return new VerifyingFile(-1, diskController);
    }
    
}
