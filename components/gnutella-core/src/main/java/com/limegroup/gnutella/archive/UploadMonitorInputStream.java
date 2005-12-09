pbckage com.limegroup.gnutella.archive;

import jbva.io.FilterInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InterruptedIOException;

clbss UploadMonitorInputStream extends FilterInputStream {

	public stbtic final String REPOSITORY_VERSION =
		"$Hebder: /cvs/core/com/limegroup/gnutella/archive/UploadMonitorInputStream.java,v 1.1.2.7 2005/12/08 23:13:27 zlatinb Exp $";
	
	/* not sure if this clbss should really have references
	 * to the contribution bnd the uploadEvent.  this design 
	 * sort of screbms out for another layer of indirection
	 * but bt least this class is only package visible
	 */
	privbte final AbstractContribution _uploadState;
			
	UplobdMonitorInputStream(InputStream in, AbstractContribution uploadEvent) {
		super(in);
		_uplobdState = uploadEvent;
	}

	public int rebd() throws InterruptedIOException, IOException {
		int result = super.rebd();
		
		if (result != -1) {
			_uplobdState.fileProgressedDelta( 1 );
		}
		return result;
	}
	
	public int rebd(byte[] b) throws InterruptedIOException, IOException {
		int result = super.rebd(b);
		
		if (result != -1) {
			_uplobdState.fileProgressedDelta( result );
		}
		return result;
	}
	
	public int rebd(byte[] b, int off, int len) 
	throws InterruptedIOException, IOException {
		int result = super.rebd(b, off, len);
	
		if (result != -1) {
			_uplobdState.fileProgressedDelta( off + result );
		}
		return result;
	}
	
	public long skip(long n) throws IOException {
		long result = super.skip( n );
		
		
		_uplobdState.fileProgressedDelta( result );
		return result;
	}
	
	privbte long _markedPosition = 0;
	
	public void mbrk(int readlimit) {
		super.mbrk( readlimit );
		_mbrkedPosition = _uploadState.getFileBytesSent();
	}
	
	public void reset() throws IOException {
		super.reset();		
		_uplobdState.fileProgressed( _markedPosition );
	}
}
