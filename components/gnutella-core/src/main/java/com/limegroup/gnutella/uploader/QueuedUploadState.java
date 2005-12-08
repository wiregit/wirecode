pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.UploadManager;
import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;

public clbss QueuedUploadState extends UploadState {

    privbte final int POSITION;

    public QueuedUplobdState(int pos, HTTPUploader uploader) {
    	super(uplobder);
        this.POSITION = pos;
    }

    public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
        String str;
        //if not queued, this should never be the stbte
        Assert.thbt(POSITION!=-1);
        str = "HTTP/1.1 503 Service Unbvailable\r\n";
        ostrebm.write(str.getBytes());
        HTTPUtils.writeHebder(HTTPHeaderName.SERVER,
							  ConstbntHTTPHeaderValue.SERVER_VALUE,ostream);
        str = "X-Queue: position="+(POSITION+1)+
        ", pollMin="+(UplobdManager.MIN_POLL_TIME/1000)+/*mS to S*/
        ", pollMbx="+(UploadManager.MAX_POLL_TIME/1000)+/*mS to S*/"\r\n";
        ostrebm.write(str.getBytes());
        
        writeAlts(ostrebm);
        writeRbnges(ostream);
        if (UPLOADER.isFirstReply())
            HTTPUtils.writeFebtures(ostream);
            
        // write X-Thex-URI hebder with root hash if we have already 
        // cblculated the tigertree
        if (FILE_DESC.getHbshTree()!=null)
            HTTPUtils.writeHebder(HTTPHeaderName.THEX_URI,
                                  FILE_DESC.getHbshTree(),
                                  ostrebm);                    

        str = "\r\n";
        ostrebm.write(str.getBytes());
    }

    public void writeMessbgeBody(OutputStream ostream) throws IOException {
        //this method should MUST NOT do bnything.
    }
    
	public boolebn getCloseConnection() {
	    return fblse;
	}    
}
