package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * Specialized class that attempts to connect to a rotating list of well-known
 * Internet addresses to check whether or not this host has a live connection
 * to the Internet.
 */
public final class ConnectionChecker implements Runnable {

    /**
     * Flag for whether or not we know for sure that we're connected from
     * successfully connecting to an external host.
     */
    private volatile boolean _connected;

    /**
     * Variable for the number of unsuccessful connection attempts.
     */
    private int _unsuccessfulAttempts;

    /**
     * Log for logging this class.
     */
    private static final Log LOG =
        LogFactory.getLog(ConnectionChecker.class);
        

    /**
     * Array of standard internet hosts to connect to when determining whether
     * or not the user has a live Internet connection.  These are randomized
     * so a minimum number is hit on each check.  Note that we only hit one 
     * random server per test and that we only test the connection if we have
     * ample evidence that the users machine is no longer connected, resulting
     * in minimal traffic to these sites.  NON-FINAL FOR TESTING.
     */
    private static String[] STANDARD_HOSTS = {
        "http://www.wanadoo.fr",
        "http://www.tiscali.com",
        "http://www.ntt.com",
        "http://www.tonline.com",
        "http://www.download.com",
        "http://www.ibm.com",
        "http://www.sun.com",
        "http://www.apple.com",
        "http://www.ebay.com",
        "http://www.sun.com",
        "http://www.monster.com",
        "http://www.uunet.com",
        "http://www.real.com",
        "http://www.level3.com",
        "http://www.microsoft.com",
        "http://www.sco.com",
        "http://www.google.com",
        "http://www.cnn.com",
        "http://www.amazon.com",
        "http://www.espn.com", 
        "http://www.yahoo.com",
        "http://www.oracle.com",
        "http://www.dell.com",
        "http://www.ge.com",
        "http://www.sprint.com",
        "http://www.att.com",
        "http://www.mci.com",
        "http://www.cisco.com",
        "http://www.intel.com",
        "http://www.motorola.com",
        "http://www.hp.com",
        "http://www.gateway.com",
        "http://www.sony.com",
        "http://www.ford.com",
        "http://www.gm.com",
        "http://www.aol.com",
        "http://www.verizon.com",
        "http://www.passport.com",
        "http://www.go.com",
        "http://www.overture.com",
        "http://www.earthlink.net",
        "http://www.bellsouth.net",
        "http://www.excite.com",
        "http://www.paypal.com",
        "http://www.altavista.com",
        "http://www.weather.com",
        "http://www.mapquest.com",
        "http://www.geocities.com",
        "http://www.juno.com",
        "http://www.msnbc.com",
        "http://www.lycos.com",
        "http://www.comcast.com",
        "http://www.overture.com",
    };
    
    /**
     * Private constructor ensures that only this class can create instances of
     * itself.
     */
    private ConnectionChecker() {}

    /**
     * Creates a new <tt>ConnectionChecker</tt> instance that checks for a live
     * internet connection.  If the checker determines that there is no active 
     * connection, it will notify the <tt>ConnectionManager</tt> to take
     * appropriate action.
     * 
     * @return a new <tt>ConnectionChecker</tt> instance
     */
    public static ConnectionChecker checkForLiveConnection() {
        LOG.trace("checking for live connection");

        ConnectionChecker checker = new ConnectionChecker();
        Thread connectionThread = 
        new ManagedThread(checker, "check for live connection");
        connectionThread.setDaemon(true);
        connectionThread.start();
        return checker;
    }

    /**
     * Checks for a live internet connection.
     */
    public void run() {
        try {
            List hostList = Arrays.asList(STANDARD_HOSTS);
            
            // Add some randomization.
            Collections.shuffle(hostList);
            
            Iterator iter = hostList.iterator();
            while(iter.hasNext()) {
                String curHost = (String)iter.next();        
                connectToHost(curHost);
                
                // Break out of the loop if we've already discovered that we're 
                // connected -- we only need to successfully connect to one host
                // to know for sure that we're up.
                if(_connected) {
                    return;
                }
                
                // Stop if we've failed to connect to more than 2 of the hosts
                // that should be up all of the time.  We do this to make extra
                // sure the user's connection is down.  If it is down, trying
                // multiple times adds no load to the test servers.
                if(_unsuccessfulAttempts > 2) {
                    RouterService.getConnectionManager().noInternetConnection(); 
                    return;   
                }
            }
        } catch(Throwable t) {
            // Report any unhandled errors.
            ErrorService.error(t);
        }
    }
    
    /**
     * Determines whether or not we have connected to an external host, 
     * verifying that we have an internet connection.
     * 
     * @return <tt>true</tt> if we have created a successful connection, 
     *  otherwise <tt>false</tt>
     */
    public boolean hasConnected() {
        return _connected;
    }

    /**
     * Connects to an individual host.
     * 
     * @param host the host to connect to
     */
    private void connectToHost(String host) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("connecting to: "+host);
        }
        
        HttpMethod head = new HeadMethod(host);
        head.addRequestHeader("Cache-Control", "no-cache");
        head.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        head.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
            "close");
        head.setFollowRedirects(false);
        HttpClient client = HttpClientManager.getNewClient(20000, 3000);
        try {
            client.executeMethod(head);
            _connected = true;     
        } catch (IOException e) {
            LOG.warn("Exception while handling server", e);
            _unsuccessfulAttempts++;
        } finally {
            //Reclaim this connection
            head.releaseConnection();
            //Close it -- just to make sure it's closed.
            head.abort();
        }        
    }
}
