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
    
    /**
     * Used when handshaking with other LimeWires. 
     */
    private String latestVersion;
    /**
     * The language specific string that contains the new features of the 
     * version discovered in the network
     */ 
    private String message = "";
    /**
     * true if message is as per the user's language  preferences.
     */
    private boolean usesLocale;
    
    private static UpdateManager INSTANCE=null;

    /**
     * Constructor, reads the latest update.xml file from the last run on the
     * network, and srores the values in latestVersion, message and usesLocale.
     * latestVersion is the only variable whose value is used after start up. 
     * The other two message and usesLocale are used only once when showing the 
     * user a message at start up. So although this class is a singleton, it's 
     * safe for the constructor to set these two values for the whole session.
     */
    private UpdateManager() {
        try {
            File file = new File(CommonUtils.getUserSettingsDir(),"update.xml");
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
            usesLocale = parser.usesLocale();
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

        if(myVersion.equalsIgnoreCase(latestVersion)) //are we equal?
            return;
        if(!isGreaterVersion(latestVersion,myVersion))
            return;
        //OK. myVersion < latestVersion
        String guiMessage = latestVersion+". "+message;
        gui.notifyUserAboutUpdate(guiMessage, CommonUtils.isPro(),usesLocale);
    }

    public void checkAndUpdate(Connection connection) {
        String nv=connection.getProperty(ConnectionHandshakeHeaders.X_VERSION);
        debug("myVersion:"+latestVersion+" theirs: "+nv);
        if(!isGreaterVersion(nv,latestVersion))
            return;        
        final Connection c = connection;
        Thread checker = new Thread() {
            public void run() {
                debug("Getting update file");
                final String UPDATE = "/update.xml";
                //if we get host or port incorrectly, we will not be able to 
                //establish a connection and just return, its fail safe. 
                InetAddress ip = c.getOrigHost();
                int port = c.getOrigPort();
                byte[] data = null;
                try {
                    URL url = new URL("HTTP",ip.getHostAddress(),port,UPDATE);
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
                    if(isGreaterVersion(newVersion,latestVersion)) {
                        debug("committing new update file");
                        synchronized(UpdateManager.this) {
                            commitVersionFile(data);//could throw an exception
                            //committed file, update the value of latestVersion
                            latestVersion = newVersion;
                            debug("commited file. Latest is:"+latestVersion);
                        }
                        //Note: At this point, the connections that are already
                        //established, still think the latest version is the
                        //what it was when it was established. But that not 
                        //such a big deal - the handshaking has 
                        //already been done.
                        //Newer connections will send out the right value.
                        //Further if a client does get a update file of a 
                        //differnt version than advertised in the handshaking, 
                        //its not a problem - the client always does its own
                        //verification.
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
     * compares newVer with oldVer. and returns true iff newVer is a newer 
     * version, false if neVer <= older.
     * <p>
     * <pre>
     * treats @version@ as the highest version possible. The danger is that 
     * we may try to get updates from all files that have  @version@ in the 
     * field. This is undesirable. So if we think the latest version is 
     * @version@ we do not put an X-Version header in the handshaking.
     * </pre>
     */
    public static boolean isGreaterVersion(String newVer, String oldVer) {
        if(newVer==null && oldVer==null)
            return false;
        if(newVer==null)//old is newer
            return false;
        if(oldVer==null) //new is newer
            return true;
        if(newVer.equals(oldVer))//same
            return false;
        if(newVer.equals("@version@")) //new is newer
            return true;
        if(oldVer.equals("@version@")) //old is newer
            return false;
        //OK. Now lets look at numbers
        int o1, o2 = -1;
        int n1, n2 = -1;
        try {
            StringTokenizer tokenizer = new StringTokenizer(oldVer,".");
            o1 = (new Integer(tokenizer.nextToken())).intValue();
            o2 = (new Integer(tokenizer.nextToken())).intValue();
            tokenizer = new StringTokenizer(newVer,".");
            n1 = (new Integer(tokenizer.nextToken())).intValue();
            n2 = (new Integer(tokenizer.nextToken())).intValue();
        } catch (Exception e) {//numberFormat or NoSuchElementException
            return false;
        }
        if(n1>o1)
            return true;
        else if(n1==o1 && n2>o2)
            return true;        
        return false;
    }

    /**
     *  writes data to signed_updateFile
     */ 
    private void commitVersionFile(byte[] data) throws IOException {
        File f = new File(CommonUtils.getUserSettingsDir(),"update.xml");
        File nf = new File(CommonUtils.getUserSettingsDir(),"update.new");
        RandomAccessFile raf = new RandomAccessFile(nf,"rw");
        raf.write(data);
        raf.close();
        boolean deleteOld = f.delete();
        if(deleteOld) {
            boolean renamed = nf.renameTo(f);//dont update latestVersion
            if(!renamed) {
                nf.delete();
                throw new IOException();
            }
        } 
        else { //delete the file. The .ver file will be unpacked
            nf.delete();
            throw new IOException();//dont update latestVersion
        }
    }
    
    private boolean debug = false;
    
    private void debug(String str) {
        if(debug)
            System.out.println(str);
    }
   
}
