package com.limegroup.gnutella.downloader;

/**
 * Thrown when a file can't be downloaded because some other downloader is
 * already downloading it (or queued to do so.) 
 */
public class AlreadyDownloadingException extends Exception {
    private String filename;

    public AlreadyDownloadingException(String filename) {
        this.filename=filename;
    }

    /** Returns the name of the conflicting file, without path info. */
    public String getFilename() {
        return filename;
    }
}
