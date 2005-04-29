package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;

/**
 * IOException which can be thrown from {@link Downloader#setSaveLocation(File)}.
 */
public class SaveLocationException extends IOException {

    /** Attempt to change save location that violates security rules, such as attempting directory traversal .*/
    public static final int SECURITY_VIOLATION = 1;
	/** Attempt to change save location too late to save file in new place */
    public static final int FILE_ALREADY_SAVED = 2;
    /** Attempt to change save location to a directory where files cannot be created */
    public static final int DIRECTORY_NOT_WRITEABLE = 3;
    /** Attempt to change save location to a non-existant directory */
    public static final int DIRECTORY_DOES_NOT_EXIST = 4; 
    /** setSaveLocation was passed a File that already exists */
    public static final int FILE_ALREADY_EXISTS = 5;
    /** Attempt to change save location to a "directory" that exists, but is not a directory */
    public static final int NOT_A_DIRECTORY = 6;
    /** Attempt to change save location to a directory with a non-existant parent */
    public static final int LOCATION_HAS_NO_PARENT = 7;
    
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
