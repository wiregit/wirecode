package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.html.FileListHTMLPage;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to get a file view.
 */
public final class FileViewUploadState extends UploadState {

    public final static byte[] BAD_PASS_REPLY = "Wrong Password!".getBytes();
    public final static byte[] MALFORMED_REQUEST_REPLY = 
        "Malformed Request!".getBytes();
    
    

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    public FileViewUploadState(HTTPUploader uploader) {
		super(uploader);
    }
        
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
        final String fileName = UPLOADER.getFileName();
        final String pass = 
            UploadManager.FV_REQ_BEGIN + "/" + UploadManager.FV_PASS;

        // make sure it has the correct beginning
        if (fileName.startsWith(pass)) {
            FileListHTMLPage htmlGen = FileListHTMLPage.instance();
            String indices = fileName.substring(pass.length());

            // the url may want only certain files
            if (indices.length() > 1) {
                StringTokenizer st = new StringTokenizer(indices, "/&");
                List descs = new ArrayList(st.countTokens());
                while (st.hasMoreTokens()) {
                    try {
                        int i = Integer.parseInt(st.nextToken());
                        descs.add(RouterService.getFileManager().get(i));
                    }
                    catch (NumberFormatException nfe) {
                    }
                    catch (IndexOutOfBoundsException ioooe) {
                    }
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
        UPLOADER.setAmountUploaded(BAOS.size());
        debug("BHUS.doUpload(): returning.");
	}
	
	public boolean getCloseConnection() {
	    return false;
	}  	

    private final static boolean debugOn = false;
    private final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

    
}
