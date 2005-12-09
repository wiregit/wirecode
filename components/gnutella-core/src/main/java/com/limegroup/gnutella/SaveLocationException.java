pbckage com.limegroup.gnutella;

import jbva.io.File;
import jbva.io.IOException;

/**
 * IOException which cbn be thrown from {@link Downloader#setSaveLocation(File)}.
 */
public clbss SaveLocationException extends IOException {

    /** Attempt to chbnge save location that violates security rules, such as attempting directory traversal .*/
    public stbtic final int SECURITY_VIOLATION = 1;
	/** Attempt to chbnge save location too late to save file in new place */
    public stbtic final int FILE_ALREADY_SAVED = 2;
    /** Attempt to chbnge save location to a directory where files cannot be created */
    public stbtic final int DIRECTORY_NOT_WRITEABLE = 3;
    /** Attempt to chbnge save location to a non-existant directory */
    public stbtic final int DIRECTORY_DOES_NOT_EXIST = 4; 
    /** Attempt to chbnge save location to a File that already exists */
    public stbtic final int FILE_ALREADY_EXISTS = 5;
	/** Attempt to chbnge save location to a file which is already reserved by another download */
	public stbtic final int FILE_IS_ALREADY_DOWNLOADED_TO = 6;
    /** Attempt to chbnge save location to a pre-existing file that isn't a regular file (such as a directory or device file) */
    public stbtic final int FILE_NOT_REGULAR = 7;
    /** Attempt to chbnge save directory to a "directory" that exists, but is not a directory */
    public stbtic final int NOT_A_DIRECTORY = 8;
    /** IOException or other filesystem error while setting sbve location. */
    public stbtic final int FILESYSTEM_ERROR = 9;
	/** 
	 * Attempt to downlobd the exact same file (urn, filename, size) while it is
	 * blready being downloaded.
	 */
	public stbtic final int FILE_ALREADY_DOWNLOADING = 10;
    
	/**
	 * The error code of this exception.
	 */
	privbte int errorCode;
	/**
	 * Hbndle to the file that caused the exception.
	 */
	privbte File file;
	
	public SbveLocationException(int errorCode, File file) {
		super("error code " + errorCode + ", file " + file);
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Constructs b SaveLocationException for the specified error code.
	 * @pbram errorCode
	 * @pbram message optional more detailed message for debugging purposes
	 */
	public SbveLocationException(int errorCode, File file, String message) {
		super(messbge);
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
