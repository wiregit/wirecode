package com.limegroup.gnutella.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

class UploadMonitorInputStream extends FilterInputStream {

	/* not sure if this class should really have references
	 * to the contribution and the uploadEvent.  this design 
	 * sort of screams out for another layer of indirection
	 * but at least this class is only package visible
	 */
	private final AbstractContribution _uploadState;
			
	UploadMonitorInputStream(InputStream in, AbstractContribution uploadEvent) {
		super(in);
		_uploadState = uploadEvent;
	}

	public int read() throws InterruptedIOException, IOException {
		int result = super.read();
		
		if (result != -1) {
			_uploadState.fileProgressedDelta( 1 );
		}
		return result;
	}
	
	public int read(byte[] b) throws InterruptedIOException, IOException {
		int result = super.read(b);
		
		if (result != -1) {
			_uploadState.fileProgressedDelta( result );
		}
		return result;
	}
	
	public int read(byte[] b, int off, int len) 
	throws InterruptedIOException, IOException {
		int result = super.read(b, off, len);
	
		if (result != -1) {
			_uploadState.fileProgressedDelta( off + result );
		}
		return result;
	}
	
	public long skip(long n) throws IOException {
		long result = super.skip( n );
		
		
		_uploadState.fileProgressedDelta( result );
		return result;
	}
	
	private long _markedPosition = 0;
	
	public void mark(int readlimit) {
		super.mark( readlimit );
		_markedPosition = _uploadState.getFileBytesSent();
	}
	
	public void reset() throws IOException {
		super.reset();		
		_uploadState.fileProgressed( _markedPosition );
	}
}
