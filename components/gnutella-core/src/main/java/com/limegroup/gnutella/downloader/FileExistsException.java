package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown if the file already exists in the download directory
 */
public class FileExistsException extends IOException {
	public FileExistsException() { super("File Already Exists"); }
	public FileExistsException(String msg) { super(msg); }
}
