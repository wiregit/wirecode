pbckage com.limegroup.gnutella.uploader;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.RandomAccessFile;

import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.util.CommonUtils;

public clbss UpdateFileState extends UploadState {
    
    //this will be populbted with the contents of the file.
    //this need not be crebted everytime we get a request for a file. 
    //rbther it should be cached and updated everytime the file gets updated.
    byte[] updbteContents;

    

    public UpdbteFileState(HTTPUploader uploader) {
    	super(uplobder);
        
    }


    public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
        //If bny of this throws an exception, we will not send the headers.
        File f = new File(CommonUtils.getUserSettingsDir(),"updbte.xml");
        RbndomAccessFile raf = new RandomAccessFile(f,"r");
        int len = (int)rbf.length();//not a very long file so no risk
        updbteContents = new byte[len];
        rbf.read(updateContents);
        rbf.close();
        //Rebd the file OK. Now send the headers. 
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostrebm.write(str.getBytes());
		str = "User-Agent: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
        str = "Content-Type: " + Constbnts.QUERYREPLY_MIME_TYPE + "\r\n";
        ostrebm.write(str.getBytes());
	    str = "Content-Length: " + updbteContents.length + "\r\n";
		ostrebm.write(str.getBytes());
		HTTPUtils.writeHebder(HTTPHeaderName.CONNECTION,
		                      ConstbntHTTPHeaderValue.CLOSE_VALUE,
		                      ostrebm);		
		str = "\r\n";
        ostrebm.write(str.getBytes());
    }
    
    public void writeMessbgeBody(OutputStream ostream) throws IOException  {
        ostrebm.write(updateContents); 
        UPLOADER.setAmountUplobded(updateContents.length);
    }
    
	public boolebn getCloseConnection() {
	    return true;
	}    
}
