package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown if the file already exists in the download directory
 */
public class FileExistsException extends IOException {
    private String filename;

    /**
     * @param filename the name of the file that already exists
     */
	public FileExistsException(String filename) { 
        this("", filename);        
    }

    /**
     * @param msg a generic error message
     * @param filename the name of the file that already exists     
     */
	public FileExistsException(String msg, String filename) {
        super(msg);
        this.filename=filename;
    }

    /** Returns the filename passed to this' constructor. */
    public String getFileName() {
        return filename;
    }
}
