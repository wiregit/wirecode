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

    private static long DELAY = 7*60*60*1000;//7 hours
    
    private static UpdateManager INSTANCE=null;

    public static final String SPECIAL_VERSION = "@version@";
    
    /**
     * Whether or not we think we have a valid file on disk.
     */ 
    private boolean isValid;

    /**
     * Constructor, reads the latest update.xml file from the last run on the
     * network, and srores the values in latestVersion, message and usesLocale.
     * latestVersion is the only variable whose value is used after start up. 
     * The other two message and usesLocale are used only once when showing the 
     * user a message at start up. So although this class is a singleton, it's 
     * safe for the constructor to set these two values for the whole session.
     */
    private UpdateManager() {
        RandomAccessFile raf = null;
        try {
            File file =
                new File(CommonUtils.getUserSettingsDir(),"update.xml");
            raf = new RandomAccessFile(file,"r");
            byte[] content = new byte[(int)raf.length()];
            raf.readFully(content);
            //we dont really need to verify, but we may as well...so here goes.
            UpdateMessageVerifier verifier = 
                          new UpdateMessageVerifier(content, true);//from disk
            boolean verified = false;
            try {
                verified = verifier.verifySource();
            } catch(ClassCastException ccx) {
                verified = false;
            }
            if(!verified) {
                latestVersion = CommonUtils.getLimeWireVersion();
                return;
            }
            String xml = new String(verifier.getMessageBytes(),"UTF-8");
            UpdateFileParser parser = new UpdateFileParser(xml);
            latestVersion = parser.getVersion();
            message = parser.getMessage();
            usesLocale = parser.usesLocale();
            isValid = true;
        } catch(SAXException sax) {
            latestVersion = CommonUtils.getLimeWireVersion();
            isValid = false;
        } catch(IOException iox) {
            latestVersion = CommonUtils.getLimeWireVersion();
            isValid = false;
        } catch(VerifyError error) {
            // report the bug, but still allow things to continue.
            // otherwise LimeWire is rendered completely useless
            // (it won't connect to anyone because it can't build the
            //  DefaultHeaders)
            latestVersion = CommonUtils.getLimeWireVersion();
            isValid = false;
            ErrorService.error(error);
        } finally {
            if(raf != null) {
                try {
                    raf.close();
                } catch(IOException ignored) {}
            }
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
     * Returns whether or not we have a valid file on disk.
     */
    public boolean isValid() {
        return isValid;
    }
    
    /**
     * Prompts the user about an update if one is available.
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
        String guiMessage = latestVersion + ". " + message;
        RouterService.getCallback().
            notifyUserAboutUpdate(guiMessage, CommonUtils.isPro(), usesLocale);
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
        Thread checker = new ManagedThread("UpdateFileRequestor") {
            public void managedRun() {
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
                         new UpdateMessageVerifier(data, false);// from network
                    boolean verified = false;
                    try {
                        verified = verifier.verifySource();
                    } catch (ClassCastException ccx) {
                        verified = false;
                    }
                    if(!verified)
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
                        
                        //OK. We don't want to DDOS ourselves so lets sleep 
                        //for a random amount of time.
                        //Find the publish time of the update.xml file
                        long makeTime = parser.getTimestamp();
                        if(makeTime == -1)//timestammp not in file?? huh??
                            makeTime = System.currentTimeMillis();
                        
                        //if the file has just been released, sleep randomly up
                        //to seven hours, otherwise if it's been seven hours
                        //since the file was published, we can show the user and
                        //update right now.
                        if(System.currentTimeMillis() < makeTime+DELAY) {
                            Random rand = new Random();
                            long sleep = rand.nextLong() % DELAY;
                            if(sleep<0)
                                sleep = -1*sleep;
                            try {
                                Thread.sleep(sleep);
                            } catch(InterruptedException ignored) {}
                        }
                        RouterService.getCallback().indicateNewVersion();
                    }
                } catch(IOException iox) {
                    //IOException - reading from socket, writing to disk etc.
                    return;
                } catch(SAXException sx) {
                    //SAXException - parsing the xml
                    return; //We can't continue...forget it.
                } catch(Throwable t) {
                    ErrorService.error(t);
                } finally {
                    if( get != null )
                        get.releaseConnection();
                }
            }//end of run
        };
        checker.setDaemon(true);
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
            if(tokenizer.countTokens() < 2)
                return false;
            o1 = (new Integer(tokenizer.nextToken())).intValue();
            o2 = (new Integer(tokenizer.nextToken())).intValue();
            tokenizer = new StringTokenizer(newVer,".");
            if(tokenizer.countTokens() < 2)
                return false;
            n1 = (new Integer(tokenizer.nextToken())).intValue();
            n2 = (new Integer(tokenizer.nextToken())).intValue();
        } catch(NumberFormatException nfe) {
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
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(nf,"rw");
            raf.write(data);
        } finally {
            if(raf != null) {
                try {
                    raf.close();
                } catch(IOException ignored) {}
            }
        }
        boolean deleteOld = f.delete();
        if(deleteOld) {
            boolean renamed = nf.renameTo(f);//dont update latestVersion
            if(!renamed) {
                isValid = false;
                nf.delete();
                throw new IOException();
            } else 
                isValid = true; //everything went a-okay, we have a valid file.
        } 
        else { //delete the file. The .ver file will be unpacked
            nf.delete();
            throw new IOException();//dont update latestVersion
        }
    }
}
