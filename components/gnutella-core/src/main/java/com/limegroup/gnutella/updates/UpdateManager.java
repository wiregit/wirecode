package com.limegroup.gnutella.updates;

import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.*;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Used for parsing the signed_update_file.xml and updating any values locally.
 * Has a singleton pattern.
 */
public class UpdateManager {
    
    private String latestVersion;
    private static UpdateManager INSTANCE=null;
    

    private UpdateManager() {
        //Use a class called UpdateFileParser for this.
        //TODO1: parse the xml file from disk and find out the version.
        latestVersion = "3.0.0";//temporary solution
    }
    
    public static synchronized UpdateManager instance() {
        if(INSTANCE==null)
            INSTANCE = new UpdateManager();
        return INSTANCE;
    }
    
    public synchronized String getVersion() {
        Assert.that(latestVersion!=null,"version not initilaized");
        return latestVersion;
    }
    
    /**
     * This method will be called just once from RouterService at startup
     * and it should prompt the user if there is an update message.
     */
    void postGuiInit(ActivityCallback gui) {
//          if(latestVersion > CommonUtils.getVersion())
//              gui.promptUser();
    }

    public void checkAndUpdate(Connection connection) {
        final Connection c = connection;
        Thread checker = new Thread() {
            public void run() {
                String newVersion = 
                c.getProperty(ConnectionHandshakeHeaders.X_VERSION);
                System.out.println("Sumeet:myVersion:"+
                                   latestVersion+" theirs: "+newVersion);
                if(!isGreaterVersion(newVersion))
                     return;
                System.out.println("Sumeet...getting file.");
                final String UPDATE = "/update.xml";
                String ip = c.getOrigHost();
                int port = c.getOrigPort();
                byte[] data = null;
                //TODO1: if the ip and port are invalid, we should return
                try {
                URL url = new URL("HTTP",ip,port,UPDATE);
                HttpURLConnection connection=(HttpURLConnection)
                                                      url.openConnection();
                connection.setUseCaches(false);
                connection.setRequestProperty("User-Agent",
                                              CommonUtils.getHttpServer());
                connection.setRequestProperty(  /*no persistence*/
                HTTPHeaderName.CONNECTION.httpStringValue(), "close");
                connection.setRequestProperty("accept","text/html");//??
                
                InputStream in = connection.getInputStream();
                int len = connection.getContentLength();
                data = new byte[len];
                for(int i=0; i<len; i++) {
                    int a = in.read();
                    if(a==-1)
                         break;
                    data[i] = (byte)a;
                }
                UpdateMessageVerifier verifier=new UpdateMessageVerifier(data);
                boolean verified = verifier.verifySource();
                if(!verified) {
                    System.out.println("Sumeet: verification failed");
                    return;
                }
                System.out.println("Sumeet: verified file contents");
                String xml = new String(verifier.getMessageBytes(),"UTF-8");
                UpdateFileParser parser = new UpdateFileParser(xml);
                System.out.println("Sumeet: new version: "+parser.getVersion());
                //we checked for new version while handshaking, but we should
                //check again with the authenticated xml data.
                newVersion = parser.getVersion();
                if(newVersion==null)
                    return;
                if(isGreaterVersion(newVersion)) {
                    synchronized(UpdateManager.this) {
                        verifier.commitVersionFile();//could throw an exception
                        //update the value of latestVersion
                        latestVersion = newVersion;//we have the file committed
                    }
                }
                } catch(Exception e ) {
                    //any errors? We can't continue...forget it.
                    return;
                }
            }//end of run
        };
        checker.start();      
    }

    /**
     * compares this.latestVersion with version. and returns true if version 
     * is a newer version. 
     */
    private boolean isGreaterVersion(String version) {
        if(version==null) //no version? latestVersion is newer
            return false;
        int l1, l2 = -1;
        int v1, v2 = -1;
        try {
            StringTokenizer tokenizer = new StringTokenizer(latestVersion,".");
            l1 = (new Integer(tokenizer.nextToken())).intValue();
            l2 = (new Integer(tokenizer.nextToken())).intValue();
            tokenizer = new StringTokenizer(version,".");
            v1 = (new Integer(tokenizer.nextToken())).intValue();
            v2 = (new Integer(tokenizer.nextToken())).intValue();
        } catch (Exception e) {//numberFormat or NoSuchElementException
            return false;
        }
        if(v1>l1)
            return true;
        else if(v1==l1 && v2>l2)
            return true;        
        return false;
    }
}
