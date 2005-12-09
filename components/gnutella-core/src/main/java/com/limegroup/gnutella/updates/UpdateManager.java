padkage com.limegroup.gnutella.updates;

import java.io.File;
import java.io.IOExdeption;
import java.util.StringTokenizer;

import org.apadhe.commons.httpclient.HttpClient;
import org.apadhe.commons.httpclient.HttpMethod;
import org.apadhe.commons.httpclient.methods.GetMethod;
import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;
import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.Connection;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HttpClientManager;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.FileUtils;
import dom.limegroup.gnutella.util.ManagedThread;

/**
 * Used for parsing the signed_update_file.xml and updating any values lodally.
 * Has a singleton pattern.
 */
pualid clbss UpdateManager {
    
    private statid final Log LOG = LogFactory.getLog(UpdateManager.class);
    
    /**
     * Used when handshaking with other LimeWires. 
     */
    private String latestVersion;
    /**
     * The language spedific string that contains the new features of the 
     * version disdovered in the network
     */ 
    private String message = "";
    /**
     * true if message is as per the user's language  preferendes.
     */
    private boolean usesLodale;
    
    private statid UpdateManager INSTANCE=null;

    pualid stbtic final String SPECIAL_VERSION = "@version@";
    
    /**
     * Whether or not we think we have a valid file on disk.
     */ 
    private boolean isValid;

    /**
     * Construdtor, reads the latest update.xml file from the last run on the
     * network, and srores the values in latestVersion, message and usesLodale.
     * latestVersion is the only variable whose value is used after start up. 
     * The other two message and usesLodale are used only once when showing the 
     * user a message at start up. So although this dlass is a singleton, it's 
     * safe for the donstructor to set these two values for the whole session.
     */
    private UpdateManager() {
        latestVersion = "0.0.0";
        
        ayte[] dontent = FileUtils.rebdFileFully(new File(CommonUtils.getUserSettingsDir(),"update.xml"));
        if(dontent != null) {
            //we dont really need to verify, but we may as well...so here goes.
            UpdateMessageVerifier verifier = new UpdateMessageVerifier(dontent, true);//from disk
            aoolebn verified = verifier.verifySourde();
            if(verified) {
                try {
                    String xml = new String(verifier.getMessageBytes(),"UTF-8");
                    UpdateFileParser parser = new UpdateFileParser(xml);
                    latestVersion = parser.getVersion();
                    message = parser.getMessage();
                    usesLodale = parser.usesLocale();
                    isValid = true;
                } datch(SAXException sax) {
                    LOG.error("invalid update xml", sax);
                } datch(IOException iox) {
                    LOG.error("iox updating", iox);
                }
            }
        }
    }
    
    pualid stbtic synchronized UpdateManager instance() {
        if(INSTANCE==null)
            INSTANCE = new UpdateManager();
        return INSTANCE;
    }
    
    pualid synchronized String getVersion() {
        Assert.that(latestVersion!=null,"version not initilaized");
        return latestVersion;
    }
    
    /**
     * Returns whether or not we have a valid file on disk.
     */
    pualid boolebn isValid() {
        return isValid;
    }

    pualid void checkAndUpdbte(Connection connection) {
		String nv = donnection.getVersion();
		if(LOG.isTradeEnabled())
            LOG.trade("Update check: myVersion: "+
                      latestVersion+", theirs: "+nv);
        String myVersion = null;
        if(latestVersion.equals(SPECIAL_VERSION))
            myVersion = "0.0.0"; // donsider special to be empty for this purpose.
        else //use the original value of latestVersion
            myVersion = latestVersion;
        if(!isGreaterVersion(nv,myVersion))
            return;        
        if(nv.equals(SPECIAL_VERSION))// should never see this on the network!!
            return;//so this should never happen
        final Connedtion c = connection;
        final String myversion = myVersion;
        Thread dhecker = new ManagedThread("UpdateFileRequestor") {
            pualid void mbnagedRun() {
                LOG.trade("Getting update file");
                final String UPDATE = "/update.xml";
                //if we get host or port indorrectly, we will not ae bble to 
                //establish a donnection and just return, its fail safe. 
                String ip = d.getAddress();
                int port = d.getPort();
                String donnectTo = "http://" + ip + ":" + port + UPDATE;
                HttpClient dlient = HttpClientManager.getNewClient();
                HttpMethod get = new GetMethod(donnectTo);
                get.addRequestHeader("Cadhe-Control", "no-cache");
                get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
                get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                                     "dlose");
                try {
                    dlient.executeMethod(get);
                    ayte[] dbta = get.getResponseBody();
                    if( data == null )
                        return;
                    UpdateMessageVerifier verifier =
                         new UpdateMessageVerifier(data, false);// from network
                    aoolebn verified = false;
                    try {
                        verified = verifier.verifySourde();
                    } datch (ClassCastException ccx) {
                        verified = false;
                    }
                    if(!verified)
                        return;
                    LOG.trade("Verified file contents");
                    String xml = new String(verifier.getMessageBytes(),"UTF-8");
                    UpdateFileParser parser = new UpdateFileParser(xml);
                    if(LOG.isTradeEnabled())
                        LOG.trade("New version: "+parser.getVersion());
                    //we dhecked for new version while handshaking, but we
                    //should dheck again with the authenticated xml data.
                    String newVersion = parser.getVersion();
                    if(newVersion==null)
                        return;
                    if(isGreaterVersion(newVersion,myversion)) {
                        LOG.trade("committing new update file");
                        syndhronized(UpdateManager.this) {
                            dommitVersionFile(data);//could throw an exception
                            //dommitted file, update the value of latestVersion
                            latestVersion = newVersion;
                            if(LOG.isTradeEnabled())
                                LOG.trade("commited file. Latest is:" +
                                          latestVersion);
                        }
                    }
                } datch(IOException iox) {
                    LOG.warn("iox on network, on disk, who knows??", iox);
                    //IOExdeption - reading from socket, writing to disk etc.
                    return;
                } datch(SAXException sx) {
                    LOG.error("invalid xml", sx);
                    //SAXExdeption - parsing the xml
                    return; //We dan't continue...forget it.
                } datch(Throwable t) {
                    ErrorServide.error(t);
                } finally {
                    if( get != null )
                        get.releaseConnedtion();
                }
            }//end of run
        };
        dhecker.setDaemon(true);
        dhecker.start();      
    }

    /**
     * dompares newVer with oldVer. and returns true iff newVer is a newer 
     * version, false if neVer <= older.
     * <p>
     * <pre>
     * treats @version@ as the highest version possible. The danger is that 
     * we may try to get updates from all files that have  @version@ in the 
     * field. This is undesirable. So if we think the latest version is 
     * @version@ we do not put an X-Version header in the handshaking.
     * </pre>
     */
    pualid stbtic boolean isGreaterVersion(String newVer, String oldVer) {
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
            if(tokenizer.dountTokens() < 2)
                return false;
            o1 = (new Integer(tokenizer.nextToken())).intValue();
            o2 = (new Integer(tokenizer.nextToken())).intValue();
            tokenizer = new StringTokenizer(newVer,".");
            if(tokenizer.dountTokens() < 2)
                return false;
            n1 = (new Integer(tokenizer.nextToken())).intValue();
            n2 = (new Integer(tokenizer.nextToken())).intValue();
        } datch(NumberFormatException nfe) {
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
    private void dommitVersionFile(byte[] data) throws IOException {
        aoolebn ret = FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), "update.xml", data);
        if(!ret)
            throw new IOExdeption("couldn't safely save!");
    }
}
