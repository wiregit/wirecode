package com.limegroup.gnutella.updates;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.*;
import org.xml.sax.*;
import java.net.*;
import java.util.*;
import java.io.*;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Used for parsing the signed_update_file.xml and updating any values locally.
 * Has a singleton pattern.
 */
public class UpdateManager {
    
    private static final Log LOG = LogFactory.getLog(UpdateManager.class);
    
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

    public static final String SPECIAL_VERSION = "@version@";

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
        } catch(SAXException sax) {
            latestVersion = CommonUtils.getLimeWireVersion();
        } catch(IOException iox) {
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
    public boolean displayUpdate() {
        String myVersion = CommonUtils.getLimeWireVersion();
        if(latestVersion.equals(SPECIAL_VERSION))
            return false;
        if(myVersion.equalsIgnoreCase(latestVersion)) //are we equal?
            return false;
        if(!isGreaterVersion(latestVersion,myVersion))
            return false;
        //OK. myVersion < latestVersion
        String guiMessage = latestVersion+". "+message;
        RouterService.getCallback().
            notifyUserAboutUpdate(guiMessage, CommonUtils.isPro(),usesLocale);
        return true;
    }

    public void checkAndUpdate(Connection connection) {
		String nv = connection.getVersion();
		if(LOG.isTraceEnabled())
            LOG.trace("Update check: myVersion: "+
                      latestVersion+", theirs: "+nv);
        String myVersion = null;
        if(latestVersion.equals(SPECIAL_VERSION))
            //if I have @version@ on disk check against my real version
            myVersion = CommonUtils.getLimeWireVersion();
        else //use the original value of latestVersion
            myVersion = latestVersion;
        if(!isGreaterVersion(nv,myVersion))
            return;        
        if(nv.equals(SPECIAL_VERSION))// should never see this on the network!!
            return;//so this should never happen
        final Connection c = connection;
        final String myversion = myVersion;
        Thread checker = new Thread("UpdateFileRequestor") {
            public void run() {
                LOG.trace("Getting update file");
                final String UPDATE = "/update.xml";
                //if we get host or port incorrectly, we will not be able to 
                //establish a connection and just return, its fail safe. 
                String ip = c.getAddress();
                int port = c.getPort();
                String connectTo = "http://" + ip + ":" + port + UPDATE;
                HttpMethod get = new GetMethod(connectTo);
                get.addRequestHeader("Cache-Control", "no-cache");
                get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
                get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                                     "close");
                HttpClient client = HttpClientManager.getNewClient();
                try {
                    client.executeMethod(get);
                    byte[] data = get.getResponseBody();
                    if( data == null )
                        return;
                    UpdateMessageVerifier verifier =
                        new UpdateMessageVerifier(data);
                    if(!verifier.verifySource())
                        return;
                    LOG.trace("Verified file contents");
                    String xml = new String(verifier.getMessageBytes(),"UTF-8");
                    UpdateFileParser parser = new UpdateFileParser(xml);
                    if(LOG.isTraceEnabled())
                        LOG.trace("New version: "+parser.getVersion());
                    //we checked for new version while handshaking, but we
                    //should check again with the authenticated xml data.
                    String newVersion = parser.getVersion();
                    if(newVersion==null)
                        return;
                    if(isGreaterVersion(newVersion,myversion)) {
                        LOG.trace("committing new update file");
                        synchronized(UpdateManager.this) {
                            commitVersionFile(data);//could throw an exception
                            //committed file, update the value of latestVersion
                            latestVersion = newVersion;
                            if(LOG.isTraceEnabled())
                                LOG.trace("commited file. Latest is:" +
                                          latestVersion);
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
                        
                        //Emprically, the LimeWire network finds out about
                        //updates very rapidly, so the reason to wait until the
                        //end of the session, is reduced. Further we update
                        //through the website, so we can tell the user that a
                        //newer version exits, and tell her to check the website
                        //without necessarily telling them what the new version
                        //is. So a small indicator on the GUI will do just fine.
                        String runningVersion=CommonUtils.getLimeWireVersion();
                        if(!isGreaterVersion(newVersion, runningVersion))
                            return; //runningVersion <= newVersion -- no message
                        RouterService.getCallback().indicateNewVersion();
                    }
                } catch(IOException iox) {
                    //IOException - reading from socket, writing to disk etc.
                    return;
                } catch(SAXException sx) {
                    //SAXException - parsing the xml
                    return; //We can't continue...forget it.
                } finally {
                    if( get != null )
                        get.releaseConnection();
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
        if(newVer.equals(SPECIAL_VERSION)) //new is newer
            return true;
        if(oldVer.equals(SPECIAL_VERSION)) //old is newer
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
}
