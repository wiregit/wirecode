package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.SecuritySettings;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.html.*;
import java.io.*;
import java.util.*;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to get a file view.
 */
public final class FileViewUploadState implements HTTPMessage {

    public final static byte[] BAD_PASS_REPLY = "Wrong Password!".getBytes();
    public final static byte[] MALFORMED_REQUEST_REPLY = 
        "Malformed Request!".getBytes();
    
    private final HTTPUploader _uploader;

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    public FileViewUploadState(HTTPUploader uploader) {
		this._uploader = uploader;
    }
        
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
        // make sure it has the correct beginning
        if (_uploader.hasPassword()) {
            FileListHTMLPage htmlGen = FileListHTMLPage.instance();
            String indexes = (String)_uploader.getParameters().get(UploadManager.INDEX_KEY);

            // the url may want only certain files
            if (indexes != null) {
                StringTokenizer st = new StringTokenizer(indexes, ",");
                List descs = new ArrayList(st.countTokens());
                while (st.hasMoreTokens()) {
                    try {
                        int i = Integer.parseInt(st.nextToken());
                        descs.add(RouterService.getFileManager().get(i));
                    } 
                    catch (NumberFormatException nfe) {}
                    catch (IndexOutOfBoundsException ioooe) {}
                }

                if (descs.size() > 0) {
                    FileDesc[] descArray = 
                        (FileDesc[]) descs.toArray(new FileDesc[0]);
                    BAOS.write(htmlGen.getSharedFilePage(descArray).getBytes());
                }
                else
                    BAOS.write(MALFORMED_REQUEST_REPLY);
            }
            // give everything
            else
                BAOS.write(htmlGen.getSharedFilePage().getBytes());
        }
        else
            BAOS.write(BAD_PASS_REPLY);
            
        _uploader.setFileSize(BAOS.size());
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/html; charset=ISO-8859-1\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + BAOS.size() + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        ostream.write(BAOS.toByteArray());
        _uploader.setAmountUploaded(BAOS.size());
	}
	
	public boolean getCloseConnection() {
	    return false;
	}
}
