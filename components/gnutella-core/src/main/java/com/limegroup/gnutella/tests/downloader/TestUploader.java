package com.limegroup.gnutella.tests.downloader;

import java.net.*;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;

public class TestUploader {    
    /** My name, for debugging */
    private final String name;

    /** Number of bytes uploaded */
    private volatile int totalUploaded;
    /** The throttle rate in kilobytes/sec */
    private volatile float rate;    
    /**The number of bytes this uploader uploads before dying*/
    private volatile int stopAfter;
    /** This is stopped. */
    private boolean stopped;
    /** switch to send incorrect bytes to simulate a bad uploader*/
    private boolean sendCorrupt;



    /** 
     * Creates a TestUploader listening on the given port.  Will upload a
     * special test file to any requesters via HTTP.  Non-blocking; starts
     * another thread to do the listening. 
     */
    public TestUploader(String name, final int port) {
        this.name=name;
        reset();
        //spawn loop();
        Thread t = new Thread() {
            public void run() {
                loop(port);
            }
        };
        t.setDaemon(true);
        t.start();        
    }

    /** 
     * Resets the rate, amount uploaded, stop byte, etc.
     */
    public void reset() {
        totalUploaded = 0;
        stopAfter = -1;
        rate = 10000;
        stopped = false;
        sendCorrupt = false;
    }

    public int amountUploaded() {
        return totalUploaded;
    }
    
    /** Sets the upload throttle rate 
     * Note: Even if the rate is set to zero the send method will send atleast 
     * one byte per second, in order to detect socket closes. 
     * @param rate kilobytes/sec. 
     */   
    public void setRate(float rate) {
        this.rate=rate;
    }


    /** Sets whether this should send bad data. */
    public void setCorruption(boolean corrupt) {
        this.sendCorrupt = corrupt;
    }

    /** 
     * Sets the number of  bytes that this should send.  This lets the user
     * simulate a broken upload.
     * @param n the number of  bytes to send, or -1 if no limit 
     */
    public void stopAfter(int n) {
        this.stopAfter = n;
    }
    

    /**
     * Repeatedly accepts connections and handles them.
     */
    private void loop(int port) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
        } catch (IOException ioe) {
            System.err.println("Couldn't bind socket to port "+port);
            return;
        }

        while(true) {
            Socket s=null;
            try {
                s = server.accept();
                //spawn thread to handle request
                final Socket mySocket=s;
                Thread runner=new Thread() {
                    public void run() {                                
                        try {
                            if (!stopped) {
                                handleRequest(mySocket);
                            }
                        } catch (IOException e) {
                            //e.printStackTrace();
                        } finally {
                            try {
                                mySocket.close();
                            } catch(IOException e) { 
                                return;
                            }
                        }//end of finally
                    }//end of run
                };
                runner.start();
            } catch (IOException e) {
                //e.printStackTrace();
                return;  //server socket closed.
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
        out.flush();

        //Write data at throttled rate.  See NormalUploadState.  TODO: use
        //ThrottledOutputStream
        for (int i=start; i<stop; ) {
            //1 second write cycle
            long startTime=System.currentTimeMillis();
            for (int j=0; j<Math.max(1,(rate*1024)) && i<stop; j++) {
                //if we are above the threshold, simulate an interrupted connection
                if (stopAfter>-1 && totalUploaded>=stopAfter) {
                    stopped=true;
                    out.flush();
                    throw new IOException();
                }
                if(sendCorrupt)
                    out.write(TestFile.getByte(i)+(byte)1);
                else
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
