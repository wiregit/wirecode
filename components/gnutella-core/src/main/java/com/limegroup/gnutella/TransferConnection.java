package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

/** 
 * A connection for transferring a file via HTTP.  Only a subset of the
 * protocol is supported.
 */
public class TransferConnection implements Runnable {
    /* Samples session is as follows.  First client sends
     *     GET /get/0/sample.txt HTTP/1.0
     *     User-Agent: Gnutella
     *
     * Server responds with the following, substituting <LENGTH> for the
     * length of <DATA>.  I suppose "application/binary" could be changed.
     *     HTTP 200 OK
     *     Server: Gnutella
     *     Content-type:application/binary
     *     Content-length: <LENGTH>
     * 
     *     <DATA>
     *
     * See ftp://ftp.isi.edu/in-notes/rfc2616.txt for the HTTP spec.
     */

    private Socket sock;
    private BufferedReader in;
    private BufferedWriter out;
    private static final int BUFSIZE=1024; //the buffer size for IO

    private boolean upload;

    /** The file and index to transfer, or null if the file is not found. 
     *  (This way we can give useful error messages.)
     */
    private String fileName;
    private int index=-1;


    // /** Creates an outgoing tranfer connection. */
    // public TransferConnection(String host, int port, boolean upload, String filename);
    
    /**
     * Creates a connection from existing socket.
     *
     * @requires upload ==> "GET " has just been read from sock<br>
     *          !upload ==> "PUT " has just been read from sock
     * @effects  upload ==> creates new upload connection<br>
     *          !upload ==> creates new download connection<br>
     *           unless a transfer or protocol error occurs ==> IOException
     *           Note: this does not check if the file actually exists; if it doesn't
     *           a file not found message will be sent during run().
     */
    public TransferConnection(Socket sock, boolean upload) throws IOException {
	this.sock=sock;
	this.in=new BufferedReader(new InputStreamReader(sock.getInputStream()));
	this.out=new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
	this.upload=upload;
	
	//1. Try to read "<key> HTTP/X" from network (we don't care about X)
	//   TODO1: this doesn't handle files with spaces in the name. 
	//   Perhaps the standard StreamTokenizer is better than split().
	//   If split() does prove preferable, it should be put in another class.
	String line=in.readLine();
	String[] words=Main.split(line);
	if (words.length!=2)
	    throw new IOException();
	if (! words[1].startsWith("HTTP"))
	    throw new IOException();

	//2. Pull out "/get/<index>/<name>" from <key>.  If we can't, 
	//   return, but keep name==null.
	String key=words[0];
	if (! key.startsWith("/get/"))
	    return;
	int i=key.indexOf('/',5);
	if (i==-1)
	    return;
	try {
	    this.index=Integer.parseInt(key.substring(5,i));   //got index
	} catch (NumberFormatException e) {	    
	    return;
	}
	try {
	    this.fileName=key.substring(i+1);  //got fileName
	} catch (ArrayIndexOutOfBoundsException e) {
	    return;
	}
    }
	
    /** 
     * Does the transfer.
     * 
     * @modifies this, network, possibly disk (if download)
     * @effects transfers the file. If a transfer error occurs, does nothing. 
     *   (Need a callback!) 
     */
    public void run() {
	try {
	    if (upload)
		doUpload();
	    else
		doDownload();
	    shutdown();
	} catch (IOException e) {
	    System.err.println("TODO: notify appropriate authority here!");
	}
    }

    /** 
     * Aborts the transfer. 
     *
     * @modifies this
     */
    public void shutdown() {
	try {
	    sock.close();
	} catch (IOException e) { }
    }
    
    /** Returns true if this is uploading a file to a remote host, false
     *  if it is downloading a file from a remote host. */
    public boolean isUpload() {
	return upload;
    }

    /////////////////////////////// Upload ///////////////////////////////////

    /** Writes line followed by carriage return and line feed. */
    private void writeLine(String line) throws IOException {
	out.write(line+"\r\n");
    }

    private void doUpload() throws IOException {
	//TODO1: Is there a need to read in any standard HTTP headers
	//before proceding?

	//1. Look up file
	if (fileName==null) {
	    doNoSuchFile();
	    return;
	}
	FileManager fman=FileManager.getFileManager();
	FileDesc fd=null;
	try {
	    fd=(FileDesc)fman._files.get(index);
	} catch (ArrayIndexOutOfBoundsException e) { 
	    doNoSuchFile();
	    return;
	}
	if (! fd._name.equals(fileName)) { //sanity check: does index match name?
	    doNoSuchFile();
	    return;
	}
	BufferedReader fin=null;
	try {
	    //TODO1: this is potentially a security flaw.  Double-check this is right!!
	    File file=new File(fd._path); //fd._path is the fully-qualified name
	    fin=new BufferedReader(new FileReader(file));
	} catch (FileNotFoundException e) {
	    doNoSuchFile();
	    return;
	}	    
	
	//2. Write headers
	writeLine("HTTP 200 OK");
	writeLine("Server: Gnutella");
	writeLine("Content-type:application/binary"); 	//TODO1: determine based on file
	writeLine("Content-length: "+fd._size);
	writeLine("");

	//3. Read the file from disk, write to network.
	char[] buf=new char[BUFSIZE];
	while (true) {
	    int got=fin.read(buf);
	    if (got==-1)
		break;
	    out.write(buf, 0, got);
	}
	out.flush();
    }
    
    /** Sends a 404 Not Found message */
    private void doNoSuchFile() throws IOException {
	//TODO1: is this the right format?  Even if it is, can Gnutella clients handle it?
	writeLine("HTTP 404 Not Found");
	writeLine("");
    }

    ///////////////////////////// Download /////////////////////////////////

    private void doDownload() throws IOException {
	char[] buf=new char[BUFSIZE];
	//TODO1: actually write this to file!u
	System.out.println("Downloading file...");
	Writer stdout=new OutputStreamWriter(System.out);
	while (true) {
	    int got=in.read(buf,0,BUFSIZE);
	    if (got==-1)
		break;
	    stdout.write(buf,0,got);
	}
	System.out.println("...but I can't write it to disk.");
    }

}
