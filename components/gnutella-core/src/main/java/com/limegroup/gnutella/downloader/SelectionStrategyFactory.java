package com.limegroup.gnutella.downloader;

/** A class to determine which SelectionStrategy should be used for a given file. */
public class SelectionStrategyFactory {
    /** @param extension a String representation of a file extension, 
     *      without the leading period. 
     *  @param fileSize the size (in bytes) of the file to be downloaded.
     *  @return the proper SelectionStrategy to use, based on the input params.
     */
    public static SelectionStrategy getStrategyFor(String extension, long fileSize) {
        return new BiasedRandomDownloadStrategy(fileSize);
    }
}
