package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;

/**
 * IOException which can be thrown from {@link Downloader#setSaveLocation(File)}.
 * @author fberger
 *
 */
public class SaveLocationException extends IOException {

	/** setSaveLocation was called too late to save file in new place */
    public static final int SAVE_LOCATION_ALREADY_SAVED = 1;
    /** setSaveLocation was passed a directory File where files cannot be created */
    public static final int SAVE_LOCATION_DIRECTORY_NOT_WRITEABLE = 2;
    /** setSaveLocation was passed a File with a non-existant parent */
    public static final int SAVE_LOCATION_HAS_NO_PARENT = 3; 
    /** setSaveLocation was passed a File that already exists */
    public static final int SAVE_LOCATION_ALREADY_EXISTS = 4;
    
	/**
	 * The error code of this exception.
	 */
	private int errorCode;
	/**
	 * Handle to the file that caused the exception.
	 */
	private File file;
	
	public SaveLocationException(int errorCode, File file) {
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Constructs a SaveLocationException for the specified error code.
	 * @param errorCode
	 * @param message optional more detail message for debugging purposes
	 */
	public SaveLocationException(int errorCode, File file, String message) {
		super(message);
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Returns the error code of this exception.
	 * @return
	 */
	public int getErrorCode() {
		return errorCode;
	}
	
	public File getFile() {
		return file;
	}
}
