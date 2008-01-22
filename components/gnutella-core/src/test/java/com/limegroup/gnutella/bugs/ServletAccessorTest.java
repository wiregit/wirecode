package com.limegroup.gnutella.bugs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.util.URLDecoder;

public class ServletAccessorTest extends BaseTestCase {
    
    // The stack trace of the error.
    private Throwable PROBLEM;

    //The name of the thread the error occurred in
    private String CURRENT_THREAD_NAME;   
    
    //port the server is listening on
    private final int PORT=4445;
    
    private final ThreadFactory FACTORY;
        
    private final String COMMENT_USER_ENTERED = "Testing User Comments";
    
    String userComments = "";
        
    //The amount of time to wait should the server be down (got it from RemoteClientInfo)     
    private static final long FAILURE_TIME = 60 * 60 * 1000; // 1 hour
    		
    
    public ServletAccessorTest(String name) {
        super(name);
        FACTORY = ExecutorsHelper.daemonThreadFactory(name);
    }

    public void testGetRemoteBugInfo() {                                     
            //creates and starts a new thread for the server to run on
            Thread serverSocket = FACTORY.newThread(new TestSocket());
            serverSocket.start();            
            
            //creates a new error which will be logged and send to a server
            PROBLEM = new Throwable("Testing Get Remote Bug Info");        
            CURRENT_THREAD_NAME = Thread.currentThread().getName();        
            LocalClientInfo info = new LocalClientInfo(PROBLEM, CURRENT_THREAD_NAME, null, false, null);
            info.addUserComments(COMMENT_USER_ENTERED);                                  
            ServletAccessor SA = new ServletAccessor(true, "http://localhost:"+PORT);            
            RemoteClientInfo RCI = SA.getRemoteBugInfo(info);

            try {
                serverSocket.join();                
            } catch (InterruptedException ex) { }
            
            //makes sure the comment user entered is received by the server
            try {
                assertEquals(COMMENT_USER_ENTERED, URLDecoder.decode(userComments));
            } catch (IOException ex){ }
            
            //makes sure the server is not down and no any error occurred during the process of
            //sending data to the server and getting back a response
            assertNotEquals(FAILURE_TIME, RCI.getNextAnyBugTime());
    }
    
    /** The runnable that processes the queue. */
    private class TestSocket implements Runnable {
        public void run() {           
     
            try {                
                //ServerSocket ss = new ServerSocket(port, 100, InetAddress.getByName("10.254.0.246")); //if not using localhost
                //sets up the server and socket
                ServerSocket ss = new ServerSocket(PORT);
                Socket s = ss.accept();
                
                //prepares and receives data from the client
                InputStream in = s.getInputStream();
                BufferedReader BR = new BufferedReader(new InputStreamReader(in));
                String input = null;
                int length = 0; //length of the content received from the client
                
                //processes data
                while((input=BR.readLine()).length()!=0) {
                    if(input.toLowerCase().startsWith("content-length")) {
                        length = Integer.parseInt(input.substring(input.indexOf(": ")+2));
                    }
                }
                
                //collects content
                char [] content = new char[length];
                BR.read(content, 0, length);
                String contentAsString = new String(content);                
                
                //extracts the comments entered by the user
                userComments = contentAsString.substring(contentAsString.indexOf("&63=")+4);                
                
                //prepares and sends the response to the client
                OutputStream out = s.getOutputStream();
                String response;
                response = "HTTP/1.1 200 OK\r\n";                    
                out.write(response.getBytes());                    
                out.flush();                    
                response = "Connection: Close\r\n\r\n";
                out.write(response.getBytes());
                out.flush();
                s.close();                
            } catch (IOException ex) {                
                fail(ex);
            }
            
        }
    }
}
