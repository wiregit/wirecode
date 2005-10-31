package com.limegroup.gnutella.archive;

import java.util.EventObject;

import com.limegroup.gnutella.FileDetails;

public class UploadEvent extends EventObject {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadEvent.java,v 1.1.2.4 2005-10-31 22:06:09 tolsen Exp $";

	private static final long serialVersionUID = 7412297699826457995L;
	
	public static final int FILE_STARTED = 1;
	public static final int FILE_PROGRESSED = 2;
	public static final int FILE_COMPLETED = 3;
	public static final int CONNECTED = 4;

	private FileDetails _fd;
	private int _filesSent;
	private int _totalFiles;
	private long _fileBytesSent;
	private long _fileSize;
	private long _totalBytesSent;
	private long _totalSize;

	
	private final int _id;
	
	public UploadEvent(Object source, int id) {
		super(source);
		_id = id;
	}
	
	public UploadEvent( Object source, int id, FileDetails fd, 
			int filesSent, int totalFiles, 
			long fileBytesSent, long fileSize,
			long totalBytesSent, long totalSize ) {
		this( source, id );
		_fd = fd;
		_filesSent = filesSent;
		_totalFiles = totalFiles;
		_fileBytesSent = fileBytesSent;
		_fileSize = fileSize;
		_totalBytesSent = totalBytesSent;
		_totalSize = totalSize;
	}
	
	public int getFilesSent() {
		return _filesSent;
	}

	public int getTotalFiles() {
		return _totalFiles;
	}

	
	public long getFileBytesSent() {
		return _fileBytesSent;
	}
	
	public long getFileSize() {
		return _fileSize;
	}
	
	
	public long getTotalBytesSent() {
		return _totalBytesSent;
	}
	
	public long getTotalSize() {
		return _totalSize;
	}

	
	public String getFileName() {
		return _fd.getFileName();
	}

	
	public int getID() {
		return _id;
	}




}
