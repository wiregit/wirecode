package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.ThreadFactory;

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
     * Whether we have tried to work around SP2 cutting us off.
     */
    private boolean _triedSP2Workaround;

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
        "www.wanadoo.fr",
        "www.tiscali.com",
        "www.ntt.com",
        "www.tonline.com",
        "www.download.com",
        "www.ibm.com",
        "www.sun.com",
        "www.apple.com",
        "www.ebay.com",
        "www.sun.com",
        "www.monster.com",
        "www.uunet.com",
        "www.real.com",
        "www.microsoft.com",
        "www.sco.com",
        "www.google.com",
        "www.cnn.com",
        "www.amazon.com",
        "www.espn.com", 
        "www.yahoo.com",
        "www.oracle.com",
        "www.dell.com",
        "www.ge.com",
        "www.sprint.com",
        "www.att.com",
        "www.mci.com",
        "www.cisco.com",
        "www.intel.com",
        "www.motorola.com",
        "www.hp.com",
        "www.gateway.com",
        "www.sony.com",
        "www.ford.com",
        "www.gm.com",
        "www.aol.com",
        "www.verizon.com",
        "www.passport.com",
        "www.go.com",
        "www.overture.com",
        "www.earthlink.net",
        "www.bellsouth.net",
        "www.excite.com",
        "www.paypal.com",
        "www.altavista.com",
        "www.weather.com",
        "www.mapquest.com",
        "www.geocities.com",
        "www.juno.com",
        "www.msnbc.com",
        "www.lycos.com",
        "www.comcast.com",
    };
    
    /**
     * Private constructor ensures that only this class can create instances of
     * itself.
     */
    private ConnectionChecker() {}

    private static ConnectionChecker current;
    
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

        ConnectionChecker checker;
        boolean startThread = false;
        synchronized(ConnectionChecker.class) {
            if (current == null) {
                startThread = true;
                current = new ConnectionChecker();
            }
            checker = current;
        }
        
        // Only create a new thread if one isn't alive.
        if(startThread) {
            LOG.debug("Starting a new connection-checker thread");
            ThreadFactory.startThread(checker, "check for live connection");
        }
        
        return checker;
    }

    /**
     * Checks for a live internet connection.
     */
    public synchronized void run() {
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
                    LOG.debug("Connection exists.");
                    
                    // if we did disconnect as an attempt to work around SP2, connect now.
                    if (_triedSP2Workaround && !RouterService.isConnected() && !RouterService.isConnecting()) {
                        LOG.debug("Reconnecting RouterService");
                        RouterService.connect();
                    }
                    return;
                }
                
                // Stop if we've failed to connect to more than 2 of the hosts
                // that should be up all of the time.  We do this to make extra
                // sure the user's connection is down.  If it is down, trying
                // multiple times adds no load to the test servers.
                if(_unsuccessfulAttempts > 2) {
                    LOG.debug("Failed connection check more than twice.");
                    if (_triedSP2Workaround || !CommonUtils.isWindowsXP()) {
                        RouterService.getConnectionManager().noInternetConnection();
                        return;
                    } else {
                        _triedSP2Workaround = true;
                        trySP2Workaround();
                    }
                }
            }
            
        } finally {
            synchronized(ConnectionChecker.class) {
                current = null;
            }
        }
    }
    
    private void trySP2Workaround() {
        if (hasNoTransfers() && udpIsDead())
            return; // really disconnected
        else
            killAndSleep(); // otherwise shut off all attempts until sp2's limit times out
    }
    
    /**
     * @return true if we don't have any transfers going at non-zero speed
     */
    private boolean hasNoTransfers(){
        RouterService.getDownloadManager().measureBandwidth();
        float down = RouterService.getDownloadManager().getMeasuredBandwidth();
        
        if (down != 0)
            return false;
        
        RouterService.getUploadManager().measureBandwidth();
        float up = RouterService.getUploadManager().getMeasuredBandwidth();
        
        return up == 0;
    }
    
    /**
     * @return if we think that udp traffic is dead
     */
    private boolean udpIsDead() {
        PingRequest ping = PingRequest.createUDPPing();
        Collection hosts = RouterService.getPreferencedHosts(false,"en",50);
        UDPPinger myPinger = RouterService.getHostCatcher().getPinger();
        UDPChecker checker = new UDPChecker();
        
        // send some hosts to be ranked
        myPinger.rank(hosts,checker,checker,ping);
        long now = System.currentTimeMillis();
        synchronized(checker) {
            try {
                // since there may be other udp packets backed up to be sent,
                // check every second if we have received something, and if so
                // cancel the hosts we sent.
                for (int i = 0; i < 5; i++) {
                    checker.wait(1000);
                    if (UDPService.instance().getLastReceivedTime() > now) {
                        checker.received = true;
                        return false;
                    }
                }
            } catch (InterruptedException ignored){}
        }
        return !checker.received;
    }
    
    /**
     * Terminates all attempts to open new sockets
     */
    private void killAndSleep() {
        RouterService.disconnect();
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException ignored){}
        _unsuccessfulAttempts = 0;
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
        if(LOG.isDebugEnabled())
            LOG.debug("Checking for connection with host: " + host);
        
        try  {
        	InetAddress.getByName(host); // die fast if unresolvable
            Observer observer = new Observer();
            synchronized(observer) {
                Socket s = Sockets.connect(host, 80, 20000, observer);
                LOG.debug("Waiting for callback...");
                try {
                    observer.wait(40000);
                } catch(InterruptedException e) {}
                if(!observer.hasResponse()) {
                    LOG.debug("No response!");
                    // only consider unsuccesful if we were able to remove it
                    // 'cause if it couldn't be removed, a response is still pending...
                    if(Sockets.removeConnectObserver(observer)) {
                        LOG.debug("Removed observer");
                        _unsuccessfulAttempts++;
                        IOUtils.close(s);
                    }
                }
            }
        } catch (IOException bad) {
            LOG.debug("failed to resolve name", bad);
        	_unsuccessfulAttempts++;
        }
    }
    
    private class Observer implements ConnectObserver {
        boolean response = false;
        
        // unused.
        public void handleIOException(IOException iox) {}

        // Yay, we're connected.    
        public synchronized void handleConnect(Socket socket) throws IOException {
            if(!response) {
                LOG.debug("Socket connected OK");
                
                response = true;
                _connected = true;
                notify();
                
                IOUtils.close(socket);
            }
        }

        public synchronized void shutdown() {
            if(!response) {
                LOG.debug("Socket failed to connect");
                
                response = true;
                _unsuccessfulAttempts++;
                notify();
            }
        }
        
        public boolean hasResponse() {
            return response;
        }
        
    }
    
    private class UDPChecker implements MessageListener, Cancellable {
        volatile boolean received;
        public boolean isCancelled() {
            return received;
        }
        public void processMessage(Message m, ReplyHandler handler) {
            received = true;
            synchronized(this) {
                notify();
            }
        }
        
        public void registered(byte[] guid) {}
        
        public void unregistered(byte[] guid) {}
    }
}
