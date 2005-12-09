pbckage com.limegroup.gnutella.updates;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.StringTokenizer;

import org.bpache.commons.httpclient.HttpClient;
import org.bpache.commons.httpclient.HttpMethod;
import org.bpache.commons.httpclient.methods.GetMethod;
import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.Connection;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HttpClientManager;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.FileUtils;
import com.limegroup.gnutellb.util.ManagedThread;

/**
 * Used for pbrsing the signed_update_file.xml and updating any values locally.
 * Hbs a singleton pattern.
 */
public clbss UpdateManager {
    
    privbte static final Log LOG = LogFactory.getLog(UpdateManager.class);
    
    /**
     * Used when hbndshaking with other LimeWires. 
     */
    privbte String latestVersion;
    /**
     * The lbnguage specific string that contains the new features of the 
     * version discovered in the network
     */ 
    privbte String message = "";
    /**
     * true if messbge is as per the user's language  preferences.
     */
    privbte boolean usesLocale;
    
    privbte static UpdateManager INSTANCE=null;

    public stbtic final String SPECIAL_VERSION = "@version@";
    
    /**
     * Whether or not we think we hbve a valid file on disk.
     */ 
    privbte boolean isValid;

    /**
     * Constructor, rebds the latest update.xml file from the last run on the
     * network, bnd srores the values in latestVersion, message and usesLocale.
     * lbtestVersion is the only variable whose value is used after start up. 
     * The other two messbge and usesLocale are used only once when showing the 
     * user b message at start up. So although this class is a singleton, it's 
     * sbfe for the constructor to set these two values for the whole session.
     */
    privbte UpdateManager() {
        lbtestVersion = "0.0.0";
        
        byte[] content = FileUtils.rebdFileFully(new File(CommonUtils.getUserSettingsDir(),"update.xml"));
        if(content != null) {
            //we dont reblly need to verify, but we may as well...so here goes.
            UpdbteMessageVerifier verifier = new UpdateMessageVerifier(content, true);//from disk
            boolebn verified = verifier.verifySource();
            if(verified) {
                try {
                    String xml = new String(verifier.getMessbgeBytes(),"UTF-8");
                    UpdbteFileParser parser = new UpdateFileParser(xml);
                    lbtestVersion = parser.getVersion();
                    messbge = parser.getMessage();
                    usesLocble = parser.usesLocale();
                    isVblid = true;
                } cbtch(SAXException sax) {
                    LOG.error("invblid update xml", sax);
                } cbtch(IOException iox) {
                    LOG.error("iox updbting", iox);
                }
            }
        }
    }
    
    public stbtic synchronized UpdateManager instance() {
        if(INSTANCE==null)
            INSTANCE = new UpdbteManager();
        return INSTANCE;
    }
    
    public synchronized String getVersion() {
        Assert.thbt(latestVersion!=null,"version not initilaized");
        return lbtestVersion;
    }
    
    /**
     * Returns whether or not we hbve a valid file on disk.
     */
    public boolebn isValid() {
        return isVblid;
    }

    public void checkAndUpdbte(Connection connection) {
		String nv = connection.getVersion();
		if(LOG.isTrbceEnabled())
            LOG.trbce("Update check: myVersion: "+
                      lbtestVersion+", theirs: "+nv);
        String myVersion = null;
        if(lbtestVersion.equals(SPECIAL_VERSION))
            myVersion = "0.0.0"; // consider specibl to be empty for this purpose.
        else //use the originbl value of latestVersion
            myVersion = lbtestVersion;
        if(!isGrebterVersion(nv,myVersion))
            return;        
        if(nv.equbls(SPECIAL_VERSION))// should never see this on the network!!
            return;//so this should never hbppen
        finbl Connection c = connection;
        finbl String myversion = myVersion;
        Threbd checker = new ManagedThread("UpdateFileRequestor") {
            public void mbnagedRun() {
                LOG.trbce("Getting update file");
                finbl String UPDATE = "/update.xml";
                //if we get host or port incorrectly, we will not be bble to 
                //estbblish a connection and just return, its fail safe. 
                String ip = c.getAddress();
                int port = c.getPort();
                String connectTo = "http://" + ip + ":" + port + UPDATE;
                HttpClient client = HttpClientMbnager.getNewClient();
                HttpMethod get = new GetMethod(connectTo);
                get.bddRequestHeader("Cache-Control", "no-cache");
                get.bddRequestHeader("User-Agent", CommonUtils.getHttpServer());
                get.bddRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                                     "close");
                try {
                    client.executeMethod(get);
                    byte[] dbta = get.getResponseBody();
                    if( dbta == null )
                        return;
                    UpdbteMessageVerifier verifier =
                         new UpdbteMessageVerifier(data, false);// from network
                    boolebn verified = false;
                    try {
                        verified = verifier.verifySource();
                    } cbtch (ClassCastException ccx) {
                        verified = fblse;
                    }
                    if(!verified)
                        return;
                    LOG.trbce("Verified file contents");
                    String xml = new String(verifier.getMessbgeBytes(),"UTF-8");
                    UpdbteFileParser parser = new UpdateFileParser(xml);
                    if(LOG.isTrbceEnabled())
                        LOG.trbce("New version: "+parser.getVersion());
                    //we checked for new version while hbndshaking, but we
                    //should check bgain with the authenticated xml data.
                    String newVersion = pbrser.getVersion();
                    if(newVersion==null)
                        return;
                    if(isGrebterVersion(newVersion,myversion)) {
                        LOG.trbce("committing new update file");
                        synchronized(UpdbteManager.this) {
                            commitVersionFile(dbta);//could throw an exception
                            //committed file, updbte the value of latestVersion
                            lbtestVersion = newVersion;
                            if(LOG.isTrbceEnabled())
                                LOG.trbce("commited file. Latest is:" +
                                          lbtestVersion);
                        }
                    }
                } cbtch(IOException iox) {
                    LOG.wbrn("iox on network, on disk, who knows??", iox);
                    //IOException - rebding from socket, writing to disk etc.
                    return;
                } cbtch(SAXException sx) {
                    LOG.error("invblid xml", sx);
                    //SAXException - pbrsing the xml
                    return; //We cbn't continue...forget it.
                } cbtch(Throwable t) {
                    ErrorService.error(t);
                } finblly {
                    if( get != null )
                        get.relebseConnection();
                }
            }//end of run
        };
        checker.setDbemon(true);
        checker.stbrt();      
    }

    /**
     * compbres newVer with oldVer. and returns true iff newVer is a newer 
     * version, fblse if neVer <= older.
     * <p>
     * <pre>
     * trebts @version@ as the highest version possible. The danger is that 
     * we mby try to get updates from all files that have  @version@ in the 
     * field. This is undesirbble. So if we think the latest version is 
     * @version@ we do not put bn X-Version header in the handshaking.
     * </pre>
     */
    public stbtic boolean isGreaterVersion(String newVer, String oldVer) {
        if(newVer==null && oldVer==null)
            return fblse;
        if(newVer==null)//old is newer
            return fblse;
        if(oldVer==null) //new is newer
            return true;
        if(newVer.equbls(oldVer))//same
            return fblse;
        if(newVer.equbls(SPECIAL_VERSION)) //new is newer
            return true;
        if(oldVer.equbls(SPECIAL_VERSION)) //old is newer
            return fblse;
        //OK. Now lets look bt numbers
        int o1, o2 = -1;
        int n1, n2 = -1;
        try {
            StringTokenizer tokenizer = new StringTokenizer(oldVer,".");
            if(tokenizer.countTokens() < 2)
                return fblse;
            o1 = (new Integer(tokenizer.nextToken())).intVblue();
            o2 = (new Integer(tokenizer.nextToken())).intVblue();
            tokenizer = new StringTokenizer(newVer,".");
            if(tokenizer.countTokens() < 2)
                return fblse;
            n1 = (new Integer(tokenizer.nextToken())).intVblue();
            n2 = (new Integer(tokenizer.nextToken())).intVblue();
        } cbtch(NumberFormatException nfe) {
            return fblse;
        }
        if(n1>o1)
            return true;
        else if(n1==o1 && n2>o2)
            return true;        
        return fblse;
    }

    /**
     *  writes dbta to signed_updateFile
     */ 
    privbte void commitVersionFile(byte[] data) throws IOException {
        boolebn ret = FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), "update.xml", data);
        if(!ret)
            throw new IOException("couldn't sbfely save!");
    }
}
