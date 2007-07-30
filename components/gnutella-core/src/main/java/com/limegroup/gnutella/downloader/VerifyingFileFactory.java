package com.limegroup.gnutella.downloader;

/** A factory for creating VerifyingFiles. */
public class VerifyingFileFactory {
    
    private final DiskController diskController;
    
    /** Constructs a VerifyingFileFactory that uses the given DiskController when constructing VerifyingFiles. */
    public VerifyingFileFactory(DiskController diskController) {
        this.diskController = diskController;
    }

    /** Constructs a verifying fiel with the given completed size. */
    public VerifyingFile createVerifyingFile(long completedSize) {
        return new VerifyingFile(completedSize, diskController);
    }

    /** Constructs a verifying file for testing. */
    public VerifyingFile createVerifyingFile() {
        return new VerifyingFile(-1, diskController);
    }
    
    /** Returns the diskController this factory is using. */
    public DiskController getDiskController() {
        return diskController;
    }
    
    //DPINJ: HACK! to easily use a single VerifyingFileFactory. -- REMOVE ME!
    private static final VerifyingFileFactory sharedFactory = new VerifyingFileFactory(new DiskController());
    public static VerifyingFileFactory getSharedFactory() {
        return sharedFactory;
    }
    
}
