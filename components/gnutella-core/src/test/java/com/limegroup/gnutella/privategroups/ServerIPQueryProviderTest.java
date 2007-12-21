package com.limegroup.gnutella.privategroups;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.TestCase;

import org.jivesoftware.smack.XMPPException;
import org.limewire.util.BaseTestCase;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A test class to that tests for proper parsing of ServerIPQuery objects
 */
public class ServerIPQueryProviderTest extends BaseTestCase {

    public ServerIPQueryProviderTest(String name) {
        super(name);
    }

    private String username = "ServerIPQueryParseIQTestUser";
    
    //test to make sure the Parsing method works for ServerIPQueryProvider. In a nutshell, the parsing method
    //parses the XML and creates a new ServerIPQuery object by extracting the username
    public void testparseIQ() throws IOException, XmlPullParserException{
        
       ServerIPQuery result;
       
       ServerSocket serverSocket = new ServerSocket(9999);
       
       Socket clientSocket = new Socket("127.0.0.1", 9999);
       
       
       new Thread(new ServerSocketHandler(serverSocket.accept())).start();
       
       
       
       Reader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
       
       
       MXParser parser = new MXParser();
       parser.setInput(reader);
       
       ServerIPQueryProvider ipProvider = new ServerIPQueryProvider();
       while(true){
           if(reader.ready())
            try {
                result = (ServerIPQuery) ipProvider.parseIQ(parser); 
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
       }
       assertEquals(username, result.getUsername());
    }

    private class ServerSocketHandler implements Runnable{

        private Socket connection;
        private String serveripqueryTestString = "<serveripquery xmlns=\"jabber:iq:serveripquery\"><username>"+ username+"</username></serveripquery>";
        
        public ServerSocketHandler(Socket connection){
            this.connection = connection;
        }
        public void run() {
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(connection.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pw.println(serveripqueryTestString);
        }
    }
}
