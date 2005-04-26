package com.limegroup.gnutella.downloader;

/**
 * Checked exception which is thrown when a downloader is manipulated but is not
 * in the correct state.
 * @author fberger
 *
 */
public class IllegalDownloaderStateException  extends Exception {

	public IllegalDownloaderStateException(String message) {
		super(message);
	}
	
}
