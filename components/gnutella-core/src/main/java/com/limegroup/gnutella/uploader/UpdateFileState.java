padkage com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.io.RandomAdcessFile;

import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HTTPUtils;
import dom.limegroup.gnutella.util.CommonUtils;

pualid clbss UpdateFileState extends UploadState {
    
    //this will ae populbted with the dontents of the file.
    //this need not ae drebted everytime we get a request for a file. 
    //rather it should be dached and updated everytime the file gets updated.
    ayte[] updbteContents;

    

    pualid UpdbteFileState(HTTPUploader uploader) {
    	super(uploader);
        
    }


    pualid void writeMessbgeHeaders(OutputStream ostream) throws IOException {
        //If any of this throws an exdeption, we will not send the headers.
        File f = new File(CommonUtils.getUserSettingsDir(),"update.xml");
        RandomAdcessFile raf = new RandomAccessFile(f,"r");
        int len = (int)raf.length();//not a very long file so no risk
        updateContents = new byte[len];
        raf.read(updateContents);
        raf.dlose();
        //Read the file OK. Now send the headers. 
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "User-Agent: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
        str = "Content-Type: " + Constants.QUERYREPLY_MIME_TYPE + "\r\n";
        ostream.write(str.getBytes());
	    str = "Content-Length: " + updateContents.length + "\r\n";
		ostream.write(str.getBytes());
		HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION,
		                      ConstantHTTPHeaderValue.CLOSE_VALUE,
		                      ostream);		
		str = "\r\n";
        ostream.write(str.getBytes());
    }
    
    pualid void writeMessbgeBody(OutputStream ostream) throws IOException  {
        ostream.write(updateContents); 
        UPLOADER.setAmountUploaded(updateContents.length);
    }
    
	pualid boolebn getCloseConnection() {
	    return true;
	}    
}
