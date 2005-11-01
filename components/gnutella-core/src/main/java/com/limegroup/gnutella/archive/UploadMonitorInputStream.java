package com.limegroup.gnutella.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class UploadMonitorInputStream extends FilterInputStream {

	public static final String repositoryVersion =
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadMonitorInputStream.java,v 1.1.2.1 2005-11-01 20:01:05 tolsen Exp $";
	
	/* not sure if this class should really have references
	 * to the contribution and the uploadEvent.  this design 
	 * sort of screams out for another layer of indirection
	 * but at least this class is only package visible
	 */
	private final AbstractContribution _contribution;
	private final UploadEvent _uploadEvent;
			
	UploadMonitorInputStream(InputStream in, 
			AbstractContribution contribution, UploadEvent uploadEvent) {
		super(in);
		_contribution = contribution;
		_uploadEvent = uploadEvent;
	}

	public int read() throws IOException {
		int result = super.read();
		
		if (result != -1) {
			_uploadEvent.fileProgressedDelta( 1 );
			_contribution.processUploadEvent( _uploadEvent );
		}
		return result;
	}
	
	public int read(byte[] b) throws IOException {
		int result = super.read(b);
		
		if (result != -1) {
			_uploadEvent.fileProgressedDelta( result );
			_contribution.processUploadEvent( _uploadEvent );
		}
		return result;
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		int result = super.read(b, off, len);
		
		if (result != -1) {
			_uploadEvent.fileProgressed( off + result );
			_contribution.processUploadEvent( _uploadEvent );
		}
		return result;
	}
	
	public long skip(long n) throws IOException {
		long result = super.skip( n );
		_uploadEvent.fileProgressedDelta( result );
		_contribution.processUploadEvent( _uploadEvent );
		return result;
	}
	
	private long _markedPosition = 0;
	
	public void mark(int readlimit) {
		super.mark( readlimit );
		_markedPosition = _uploadEvent.getFileBytesSent();
	}
	
	public void reset() throws IOException {
		super.reset();
		_uploadEvent.fileProgressed( _markedPosition );
		_contribution.processUploadEvent( _uploadEvent );
	}
}
