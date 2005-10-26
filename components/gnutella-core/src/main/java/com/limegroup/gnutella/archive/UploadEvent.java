package com.limegroup.gnutella.archive;

import java.util.EventObject;

import com.limegroup.gnutella.FileDesc;

public class UploadEvent extends EventObject {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadEvent.java,v 1.1.2.1 2005-10-26 20:02:48 tolsen Exp $";

	private static final long serialVersionUID = 7412297699826457995L;

	private FileDesc _fd;
	private long _bytesRead;
	private long _fileSize;
	
	public UploadEvent(Object source) {
		super(source);
		// TODO Auto-generated constructor stub
	}
	
	public UploadEvent() {
		this( null );
	}
	
	public UploadEvent( FileDesc fd, long bytesRead, long fileSize ) {
		this();
		_fd = fd;
		_bytesRead = bytesRead;
		_fileSize = fileSize;
	}
	
	public long getBytesRead() {
		return _bytesRead;
	}
	
	public long getFileSize() {
		return _fileSize;
	}
	
	public FileDesc getFD() {
		return _fd;
	}
	




}
