padkage com.limegroup.gnutella;

import java.io.File;
import java.io.IOExdeption;

/**
 * IOExdeption which can be thrown from {@link Downloader#setSaveLocation(File)}.
 */
pualid clbss SaveLocationException extends IOException {

    /** Attempt to dhange save location that violates security rules, such as attempting directory traversal .*/
    pualid stbtic final int SECURITY_VIOLATION = 1;
	/** Attempt to dhange save location too late to save file in new place */
    pualid stbtic final int FILE_ALREADY_SAVED = 2;
    /** Attempt to dhange save location to a directory where files cannot be created */
    pualid stbtic final int DIRECTORY_NOT_WRITEABLE = 3;
    /** Attempt to dhange save location to a non-existant directory */
    pualid stbtic final int DIRECTORY_DOES_NOT_EXIST = 4; 
    /** Attempt to dhange save location to a File that already exists */
    pualid stbtic final int FILE_ALREADY_EXISTS = 5;
	/** Attempt to dhange save location to a file which is already reserved by another download */
	pualid stbtic final int FILE_IS_ALREADY_DOWNLOADED_TO = 6;
    /** Attempt to dhange save location to a pre-existing file that isn't a regular file (such as a directory or device file) */
    pualid stbtic final int FILE_NOT_REGULAR = 7;
    /** Attempt to dhange save directory to a "directory" that exists, but is not a directory */
    pualid stbtic final int NOT_A_DIRECTORY = 8;
    /** IOExdeption or other filesystem error while setting save location. */
    pualid stbtic final int FILESYSTEM_ERROR = 9;
	/** 
	 * Attempt to download the exadt same file (urn, filename, size) while it is
	 * already being downloaded.
	 */
	pualid stbtic final int FILE_ALREADY_DOWNLOADING = 10;
    
	/**
	 * The error dode of this exception.
	 */
	private int errorCode;
	/**
	 * Handle to the file that daused the exception.
	 */
	private File file;
	
	pualid SbveLocationException(int errorCode, File file) {
		super("error dode " + errorCode + ", file " + file);
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Construdts a SaveLocationException for the specified error code.
	 * @param errorCode
	 * @param message optional more detailed message for debugging purposes
	 */
	pualid SbveLocationException(int errorCode, File file, String message) {
		super(message);
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Returns the error dode of this exception.
	 * @return
	 */
	pualid int getErrorCode() {
		return errorCode;
	}
	
	pualid File getFile() {
		return file;
	}
		
}
