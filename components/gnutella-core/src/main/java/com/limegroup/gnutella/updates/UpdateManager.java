package com.limegroup.gnutella.updates;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.LimeWireUtils;

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
        latestVersion = "0.0.0";
        
        byte[] content = FileUtils.readFileFully(new File(CommonUtils.getUserSettingsDir(),"update.xml"));
        if(content != null) {
            //we dont really need to verify, but we may as well...so here goes.
            UpdateMessageVerifier verifier = new UpdateMessageVerifier(content, true);//from disk
            boolean verified = verifier.verifySource();
            if(verified) {
                try {
                    String xml = new String(verifier.getMessageBytes(),"UTF-8");
                    UpdateFileParser parser = new UpdateFileParser(xml);
                    latestVersion = parser.getVersion();
                    isValid = true;
                } catch(IOException iox) {
                    LOG.error("iox updating", iox);
                }
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

    public void checkAndUpdate(Connection connection) {
		String nv = connection.getVersion();
		if(LOG.isTraceEnabled())
            LOG.trace("Update check: myVersion: "+
                      latestVersion+", theirs: "+nv);
        String myVersion = null;
        if(latestVersion.equals(SPECIAL_VERSION))
            myVersion = "0.0.0"; // consider special to be empty for this purpose.
        else //use the original value of latestVersion
            myVersion = latestVersion;
        if(!isGreaterVersion(nv,myVersion))
            return;        
        if(nv.equals(SPECIAL_VERSION))// should never see this on the network!!
            return;//so this should never happen
        final Connection c = connection;
        final String myversion = myVersion;
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                LOG.trace("Getting update file");
                final String UPDATE = "/update.xml";
                //if we get host or port incorrectly, we will not be able to 
                //establish a connection and just return, its fail safe. 
                String ip = c.getAddress();
                int port = c.getPort();
                String connectTo = "http://" + ip + ":" + port + UPDATE;
                HttpClient client = HttpClientManager.getNewClient();
                HttpMethod get = new GetMethod(connectTo);
                get.addRequestHeader("Cache-Control", "no-cache");
                get.addRequestHeader("User-Agent", LimeWireUtils.getHttpServer());
                get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                                     "close");
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
                    }
                } catch(IOException iox) {
                    LOG.warn("iox on network, on disk, who knows??", iox);
                    //IOException - reading from socket, writing to disk etc.
                    return;
                } catch(Throwable t) {
                    ErrorService.error(t);
                } finally {
                    if( get != null )
                        get.releaseConnection();
                }
            }//end of run
        }, "UpdateFileRequestor");
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
        boolean ret = FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), "update.xml", data);
        if(!ret)
            throw new IOException("couldn't safely save!");
    }
}
