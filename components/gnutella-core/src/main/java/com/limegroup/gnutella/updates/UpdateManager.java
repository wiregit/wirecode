package com.limegroup.gnutella.updates;

import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.*;
import org.xml.sax.*;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Used for parsing the signed_update_file.xml and updating any values locally.
 * Has a singleton pattern.
 */
public class UpdateManager {
    
    private String latestVersion;
    private String message = "";
    private static UpdateManager INSTANCE=null;
    

    private UpdateManager() {
        try {
            File file = new File("lib\\update.xml");
            RandomAccessFile f=new RandomAccessFile(file,"r");
            byte[] content = new byte[(int)f.length()];
            f.readFully(content);
            f.close();
            //we dont really need to verify, but we may as well...so here goes.
            UpdateMessageVerifier verifier = new UpdateMessageVerifier(content);
            boolean verified = verifier.verifySource();
            if(!verified) {
                latestVersion = CommonUtils.getLimeWireVersion();
                return;
            }
            String xml = new String(verifier.getMessageBytes(),"UTF-8");
            UpdateFileParser parser = new UpdateFileParser(xml);
            latestVersion = parser.getVersion();
            message = parser.getMessage();
        } catch(Exception e) {//SAXException or IOException, we react similarly
            latestVersion = CommonUtils.getLimeWireVersion();
        } 
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
    public void postGuiInit(ActivityCallback gui) {
        String myVersion = CommonUtils.getLimeWireVersion();
        if(myVersion.equalsIgnoreCase(latestVersion))//equal version
            return;
        if(isGreaterVersion(myVersion))
            return;
        //OK. myVersion < latestVersion
        String guiMessage = latestVersion+". "+message;
        gui.notifyUserAboutUpdate(guiMessage, CommonUtils.isPro());
    }

    public void checkAndUpdate(Connection connection) {
        String nv=connection.getProperty(ConnectionHandshakeHeaders.X_VERSION);
        debug("myVersion:"+latestVersion+" theirs: "+nv);
        if(!isGreaterVersion(nv))
            return;        
        final Connection c = connection;
        Thread checker = new Thread() {
            public void run() {
                System.out.println("Sumeet...getting file.");
                final String UPDATE = "/update.xml";
                //if we get host or port incorrectly, we will not be able to 
                //establish a connection and just return, its fail safe. 
                String ip = c.getOrigHost();
                int port = c.getOrigPort();
                byte[] data = null;
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
                    debug("Content len "+len);
                    data = new byte[len];
                    int BUF_LEN = 1024;//buffer size
                    byte[] buf = new byte[BUF_LEN];
                    int iters = len%BUF_LEN==0 ? len/BUF_LEN : (len/BUF_LEN)+1;
                    ByteReader byteReader = new ByteReader(in);
                    int totalRead = 0;
                    for(int i=0; i<iters; i++) {
                        int left = len - totalRead;
                        int a = byteReader.read(buf,0,Math.min(BUF_LEN,left));
                        if(a==-1)
                            break;
                        System.arraycopy(buf,0,data,totalRead,a);
                        totalRead += a;
                    }
                    UpdateMessageVerifier verifier=new 
                                                   UpdateMessageVerifier(data);
                    boolean verified = verifier.verifySource();
                    if(!verified) {
                        debug("Verification failed");
                        return;
                    }
                    debug("Verified file contents");
                    String xml = new String(verifier.getMessageBytes(),"UTF-8");
                    UpdateFileParser parser = new UpdateFileParser(xml);
                    debug("New version: "+parser.getVersion());
                    //we checked for new version while handshaking, but we
                    //should check again with the authenticated xml data.
                    String newVersion = parser.getVersion();
                    if(newVersion==null)
                        return;
                    if(isGreaterVersion(newVersion)) {
                        synchronized(UpdateManager.this) {
                            commitVersionFile(data);//could throw an exception
                            //committed file, update the value of latestVersion
                            latestVersion = newVersion;
                        }
                    }
                } catch(Exception e ) {
                    //MalformedURLException - while creating URL
                    //IOException - reading from socket, writing to disk etc.
                    //SAXException - parsing the xml
                    return; //any errors? We can't continue...forget it.
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

    /**
     *  writes data to signed_updateFile
     */ 
    private void commitVersionFile(byte[] data) throws IOException {
        File f = new File("lib\\update.xml");
        File nf = new File("lib\\update.new");
        RandomAccessFile raf = new RandomAccessFile(nf,"rw");
        raf.write(data);
        boolean deleted = nf.renameTo(f);
        if(!deleted)
            throw new IOException();
    }
    
    private boolean debug = true;
    
    private void debug(String str) {
        if(debug)
            System.out.println(str);
    }
    
}
