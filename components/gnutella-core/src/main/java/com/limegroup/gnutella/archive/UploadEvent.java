package com.limegroup.gnutella.archive;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class UploadEvent extends EventObject {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadEvent.java,v 1.1.2.5 2005-11-01 20:01:05 tolsen Exp $";

	private static final long serialVersionUID = 7412297699826457995L;
	
	public static final int NOT_CONNECTED = 0;
	public static final int CONNECTED = 1;	
	public static final int FILE_STARTED = 2;
	public static final int FILE_PROGRESSED = 3;
	public static final int FILE_COMPLETED = 4;


	private String _curFileName;
	private int _filesSent = -1;
	private int _totalFiles;
	private final Map _fileNames2Progress = new HashMap();
	private long _totalBytesSent;
	private long _totalSize;

	
	private int _id = NOT_CONNECTED;
	
	
	/**
	 * 
	 * @param source
	 * @param fileNames
	 * @param fileSizes
	 * 
	 * @throws IllegalArgumentException
	 * 		   If fileNames.length != fileSizes.length
	 */
	
	UploadEvent( Object source, String[] fileNames, long[] fileSizes ) {
		super( source );
		
		if (fileNames.length != fileSizes.length) {
			throw new IllegalArgumentException( "number of fileNames (" +
					fileNames.length + ") does not match number of fileSizes (" +
					fileSizes.length );
		}
		
	
		_totalFiles = fileNames.length;		
		
		for (int i = 0; i < fileNames.length; i++) {
			final UploadFileProgress progress = new UploadFileProgress( fileSizes[i] );
			_fileNames2Progress.put( fileNames[ i ], progress );
			_totalSize += fileSizes[i];
		}
		
	}
	
	void connected() {
		_id = CONNECTED;
	}
	
	
	void fileStarted( String fileName, long bytesSent ) {
		_curFileName = fileName;
		_id = FILE_STARTED;
		((UploadFileProgress) _fileNames2Progress.get( fileName )).setBytesSent( bytesSent );
	}
	
	void fileStarted( String fileName ) {
		fileStarted( fileName, 0 );
	}
	
	/**
	 * 
	 * @param fileName
	 * @param bytesSent
	 * 
	 * @throws IllegalStateException
	 *         If fileName does not match the current fileName
	 */
	void fileProgressed( long bytesSent ) {
		_id = FILE_PROGRESSED;
		
		final UploadFileProgress progress = (UploadFileProgress) _fileNames2Progress.get( _curFileName );
		
		// find delta		
		long delta = bytesSent - progress.getBytesSent();
		
		_totalBytesSent += delta;
		
		progress.setBytesSent( bytesSent );		
	}
	
	/**
	 * 
	 * @param fileName
	 * @param bytesSentDelta
	 * 
	 * @throws IllegalStateException
	 *         If fileName does not match the current fileName
	 */
	void fileProgressedDelta( long bytesSentDelta ) {
		_id = FILE_PROGRESSED;
		_totalBytesSent += bytesSentDelta;
		((UploadFileProgress) _fileNames2Progress.get( _curFileName )).incrBytesSent( bytesSentDelta );				
	}
	
	/**
	 * 
	 * @param fileName
	 * 
	 * @throws IllegalStateException
	 *         If fileName does not match the current fileName
	 */
	void fileCompleted() {
		_id = FILE_COMPLETED;
		
		final UploadFileProgress progress = (UploadFileProgress) _fileNames2Progress.get( _curFileName );
		progress.setBytesSent( progress.getFileSize() );
		_filesSent++;
	}

	
	
	public int getFilesSent() {
		return _filesSent;
	}

	public int getTotalFiles() {
		return _totalFiles;
	}

	
	public long getFileBytesSent() {
		return ((UploadFileProgress) _fileNames2Progress.get( _curFileName )).getBytesSent();		

	}
	
	public long getFileSize() {
		return ((UploadFileProgress) _fileNames2Progress.get( _curFileName )).getFileSize();
	}
	
	
	public long getTotalBytesSent() {
		return _totalBytesSent;
	}
	
	public long getTotalSize() {
		return _totalSize;
	}

	
	public String getFileName() {
		return _curFileName;
	}

	
	public int getID() {
		return _id;
	}

	private class UploadFileProgress {
		
		private long _fileSize;
		private long _bytesSent = 0;
		
		public UploadFileProgress( long fileSize ) {
			_fileSize = fileSize;
		}
		
		public long getFileSize() {
			return _fileSize;
		}
		
		public long getBytesSent() {
			return _bytesSent;
		}
		
		public void setBytesSent( long bytesSent ) {
			_bytesSent = bytesSent;
		}
		public void incrBytesSent( long bytesSentDelta ) {
			_bytesSent += bytesSentDelta;
		}
	}


}
