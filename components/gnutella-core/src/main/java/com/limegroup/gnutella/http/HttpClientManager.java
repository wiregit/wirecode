pbckage com.limegroup.gnutella.http;

import jbva.io.IOException;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.UnknownHostException;

import org.bpache.commons.httpclient.Header;
import org.bpache.commons.httpclient.HostConfiguration;
import org.bpache.commons.httpclient.HttpClient;
import org.bpache.commons.httpclient.HttpConnectionManager;
import org.bpache.commons.httpclient.HttpException;
import org.bpache.commons.httpclient.HttpMethod;
import org.bpache.commons.httpclient.HttpStatus;
import org.bpache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.bpache.commons.httpclient.URI;
import org.bpache.commons.httpclient.auth.HttpAuthenticator;
import org.bpache.commons.httpclient.protocol.Protocol;
import org.bpache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.util.Sockets;


/**
 * A simple mbnager class that maintains a single HttpConnectionManager
 * bnd doles out either a simple one (for Java 1.1.8) or the MultiThreaded
 * one (for bll other versions)
 */
public clbss HttpClientManager {
    
    privbte static final Log LOG = LogFactory.getLog(HttpClientManager.class);
    
    /**
     * The bmount of time to wait while trying to connect to a specified
     * host vib TCP.  If we exceed this value, an IOException is thrown
     * while trying to connect.
     */
    privbte static final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * The bmount of time to wait while receiving data from a specified
     * host.  Used bs an SO_TIMEOUT.
     */
    privbte static final int TIMEOUT = 8000;
    
    /**
     * The mbximum number of times to allow redirects from hosts.
     */
    privbte static final int MAXIMUM_REDIRECTS = 10;
    
    /**
     * The time to bllow a connection to sit idle, waiting for something
     * to reuse it.
     */
    privbte static final long IDLE_TIME = 30 * 1000; // 30 seconds.
    
    /**
     * The mbnager which all client connections use if not Java 1.1.8;
     */
    privbte static final HttpConnectionManager MANAGER;
    
    stbtic {
        MANAGER = new MultiThrebdedHttpConnectionManager();
        ((MultiThrebdedHttpConnectionManager)MANAGER).
            setIdleConnectionTime(IDLE_TIME);
        Protocol limeProtocol = new Protocol("http",new LimeSocketFbctory(),80);
        Protocol.registerProtocol("http",limeProtocol);
    }
            
    /**
     * Returns b new HttpClient with the appropriate manager.
     */
    public stbtic HttpClient getNewClient() {
        return getNewClient(CONNECTION_TIMEOUT, TIMEOUT);
    }
    
    /**
     * Returns b new HttpClient with the appropriate manager and parameters.
     * 
     * @pbram connectTimeout the number of milliseconds to wait to establish
     *  b TCP connection with the remote host
     * @pbram soTimeout the socket timeout -- the number of milliseconds to 
     *  wbit for data before closing an established socket
     */
    public stbtic HttpClient getNewClient(int connectTimeout, int soTimeout) {
        HttpClient client = new HttpClient(MANAGER);
        client.setConnectionTimeout(connectTimeout);
        client.setTimeout(soTimeout);
        return client;
    }
    
    /**
     * Executes the given HttpMethod in the HttpClient, following redirects.
     * This method is needed becbuse HttpClient does not support redirects
     * bcross protocols, hosts, and/or ports.
     */
    public stbtic void executeMethodRedirecting(HttpClient client,
                                                HttpMethod methid)
      throws IOException, HttpException {
        executeMethodRedirecting(client, methid, MAXIMUM_REDIRECTS);
    }
    
    /**
     * Executes the given HttpMethod in the HttpClient, following redirecits
     * up to the specific number of times.
     * This method is needed becbuse HttpClient does not support redirects
     * bcross protocols, hosts, and/or ports.
     */
    public stbtic void executeMethodRedirecting(HttpClient client,
                                                HttpMethod methid,
                                                int redirects)
      throws IOException, HttpException {
        for(int i = 0; i < redirects; i++) {
            if(LOG.isInfoEnbbled())
                LOG.info("Attempting connection (" + i + ") to " + 
                         methid.getURI().getEscbpedURI());
            client.executeMethod(methid);
            switch(methid.getStbtusCode()) {
            cbse HttpStatus.SC_MOVED_TEMPORARILY:
            cbse HttpStatus.SC_MOVED_PERMANENTLY:
            cbse HttpStatus.SC_SEE_OTHER:
            cbse HttpStatus.SC_TEMPORARY_REDIRECT:
                if(!methid.getFollowRedirects()) {
                    if(LOG.isInfoEnbbled())
                        LOG.wbrn("Redirect requested but not supported");
                    throw new HttpException("Redirect requested");
                }
                
                Hebder locationHeader = methid.getResponseHeader("location");
                if(locbtionHeader == null) {
                    if(LOG.isInfoEnbbled())
                        LOG.wbrn("Redirect requested, no location header");
                    throw new HttpException("Redirected without b location");
                }
                
                String locbtion = locationHeader.getValue();
                if(LOG.isInfoEnbbled())
                    LOG.info("Redirected requested to: " + locbtion);

                URI newLocbtion = new URI(location.toCharArray());
                
                // Retrieve the RequestHebders
                Hebder[] requestHeaders = methid.getRequestHeaders();
                
                // Recycle this method so we cbn use it again.
                methid.recycle();
                
                HostConfigurbtion hc = methid.getHostConfiguration();
                hc.setHost(
                    newLocbtion.getHost(),
                    newLocbtion.getPort(),
                    newLocbtion.getScheme()
                );
                
                methid.setFollowRedirects(true);
                
                for(int j = 0; j < requestHebders.length; j++) {
                    if(!requestHebders[j].getName().equals("Host"))
                        methid.bddRequestHeader(requestHeaders[j]);
                }
                
                // Set up the new vblues for the method.
                methid.setPbth(newLocation.getEscapedPath());
                methid.setQueryString(newLocbtion.getEscapedQuery());
                methid.removeRequestHebder(HttpAuthenticator.WWW_AUTH_RESP);

                // Loop bround and try the method again.
                brebk;
            defbult:
                return;
            }
        }
        throw new HttpException("Mbximum redirects encountered, bailing");
    }
    
    privbte static class LimeSocketFactory implements ProtocolSocketFactory {

        public Socket crebteSocket(String host, int port, InetAddress clientHost, int clientPort) 
        throws IOException, UnknownHostException {
            return Sockets.connect(host,port,0);
        }

        public Socket crebteSocket(String host, int port) throws IOException, UnknownHostException {
            return Sockets.connect(host,port,0);
        }
        
        public Socket crebteSocket(String host, int port, int timeout) throws IOException, UnknownHostException {
            return Sockets.connect(host,port, timeout);
        }
        
    }
    
}
