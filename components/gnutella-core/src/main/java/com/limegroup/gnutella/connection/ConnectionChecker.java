package com.limegroup.gnutella.connection;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
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
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

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
    private boolean _connected;

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
     * so a minimum number is hit on each check.  NON-FINAL FOR TESTING.
     */
    private static String[] STANDARD_HOSTS = {
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
        ConnectionChecker checker = new ConnectionChecker();
        Thread connectionThread = 
            new Thread(checker, "check for live connection");
        connectionThread.setDaemon(false);
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
        head.setFollowRedirects(true);
        HttpClient client = HttpClientManager.getNewClient(20000, 3000);
        try {
            client.executeMethod(head);
            _connected = true;
            
            if(LOG.isDebugEnabled()) {
                Header[] headers = head.getResponseHeaders();
                for(int i=0; i<headers.length; i++) {
                    LOG.debug(headers[i]);
                }
            }        

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
