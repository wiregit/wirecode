package com.limegroup.gnutella.tests.downloader;

import java.net.*;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;

public class TestUploader {    
    /** My name, for debugging */
    private String name;
    /** Number of bytes uploaded */
    private volatile int totalUploaded;
    /** The throttle rate in kilobytes/sec */
    private volatile int rate=10000;

    /** 
     * Creates a TestUploader listening on the given port.  Will upload a
     * special test file to any requesters via HTTP.  Non-blocking; starts
     * another thread to do the listening. 
     */
    public TestUploader(String name, final int port) {
        this.name=name;
        //spawn loop();
        Thread t = new Thread() {
            public void run() {
                loop(port);
            }
        };
        t.setDaemon(true);
        t.start();        
    }

    public int amountUploaded() {
        return totalUploaded;
    }

    public void clearAmountUploaded() {
        totalUploaded = 0;
    }
    
    /** Sets the upload throttle rate 
      * @param rate kilobytes/sec. */   
    public void setRate(int rate) {
        this.rate=rate;
    }

    
    /**
     * Repeatedly accepts connections and handles them.
     */
    private void loop(int port) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
        } catch (IOException ioe) {
            return;
        }

        while(true) {
            Socket s=null;
            try {
                s = server.accept();
                handleRequest(s); //TODO: could use thread per request
            } catch (IOException e) {
                if (s!=null)
                    try {
                        s.close();
                    } catch(IOException i) {}
            }
        }
    }
    
    
    private void handleRequest(Socket socket) throws IOException {
        //Find the region of the file to upload.  If a Range request is present,
        //use that.  Otherwise, send the whole file.  Skip all other headers.
        //TODO2: later we should check here for HTTP1.1
        //TODO2: Later we should also check the validity of the requests
        BufferedReader input = 
            new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream output = 
            new BufferedOutputStream(socket.getOutputStream());
        int start = 0;
        int stop = TestFile.length();
        while (true) {
            String line=input.readLine();
            if (line==null)
                throw new IOException("Unexpected close");
            if (line.equals(""))
                break;

            int i=line.indexOf("Range:");
            Assert.that(i<=0, "Range should be at the beginning or not at all");
            if (i==0) {
                IntPair p = null;
                try {
                    p=parseRange(line);
                } catch (Exception e) { 
                    Assert.that(false, "Bad Range request: \""+line+"\"");
                }
                start=p.a;
                stop=p.b;;
            }
        }        
        //System.out.println(System.currentTimeMillis()+" "+name+" "+start+" - "+stop);

        //Send the data.
        send(output, start, stop);
    }

    private void send(OutputStream out, int start, int stop) 
            throws IOException {
        //Write header, stolen from NormalUploadState.writeHeader()
		String str = "HTTP 200 OK \r\n";
		out.write(str.getBytes());
		str = "Content-length:"+ (stop - start) + "\r\n";
		out.write(str.getBytes());	   
		if (start != 0) {
            //Note that HTTP stop values are INCLUSIVE.  Our internal values
            //are EXCLUSIVE.  Hence the -1.
			str = "Content-range: bytes " + start  +
			"-" + (stop-1) + "/" + TestFile.length() + "\r\n"; 
			out.write(str.getBytes());
		}
        str = "\r\n";
		out.write(str.getBytes());
        
        //Write data at throttled rate.  See NormalUploadState.  TODO: use ThrottledOutputStream
        for (int i=start; i<stop; ) {
            //1 second write cycle
            long startTime=System.currentTimeMillis();
            for (int j=0; j<(rate*1024) && i<stop; j++) {
                out.write(TestFile.getByte(i));
                totalUploaded++;
                i++;
            }
            long elapsed=System.currentTimeMillis()-startTime;
            long wait=1000-elapsed;
            if (wait>0)
                try { Thread.sleep(wait); } catch (InterruptedException e) { }
        }
        out.flush();
    }

    /**
     * Returns start (inclusive) and stop range (exclusive) from the given Range
     * header.  In other words, "Range; 2-5" will return [2, 6). Extracted from
     * HTTPUploader.
     *
     * @exception IndexOutOfBoundsException parse error
     * @exception NumberFormatException parse error
     * @exception IOException socket closed
     */
    private IntPair parseRange(String str) 
            throws IndexOutOfBoundsException, 
                   NumberFormatException, IOException {
        int start=0;
        int stop=TestFile.length();
        //Set 'sub' to the value after the "bytes=" or "bytes ".  Note
        //that we don't validate the data between "Range:" and the
        //bytes.
        String sub;
        String second;
        try {
            int i=str.indexOf("bytes");    //TODO: use constant
            if (i<0)
                throw new IOException();
            i+=6;                          //TODO: use constant
            sub = str.substring(i);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        // remove the white space
        sub = sub.trim();   
        char c;
        // get the first character
        try {
            c = sub.charAt(0);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        // - n  
        if (c == '-') {  
            // String second;
            try {
                second = sub.substring(1);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            second = second.trim();
            try {
                //A range request for "-3" means return the last 3 bytes
                //of the file.  (LW used to incorrectly return bytes
                //0-3.)  
                start = Math.max(0, TestFile.length()-Integer.parseInt(second));
            } catch (NumberFormatException e) {
                throw new IOException();
            }
        }
        else {                
            // m - n or 0 -
            int dash = sub.indexOf("-");
            String first;
            try {
                first = sub.substring(0, dash);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            first = first.trim();
            try {
                start = java.lang.Integer.parseInt(first);
            } catch (NumberFormatException e) {
                throw new IOException();
            }
            try {
                second = sub.substring(dash+1);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            second = second.trim();
            if (!second.equals("")) 
                try {
                    stop = java.lang.Integer.parseInt(second)+1; //sic
                } catch (NumberFormatException e) {
                    throw new IOException();
                }
        }
        return new IntPair(start, stop);
    }
}
