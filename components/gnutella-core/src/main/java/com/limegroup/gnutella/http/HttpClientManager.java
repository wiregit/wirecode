padkage com.limegroup.gnutella.http;

import java.io.IOExdeption;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.UnknownHostExdeption;

import org.apadhe.commons.httpclient.Header;
import org.apadhe.commons.httpclient.HostConfiguration;
import org.apadhe.commons.httpclient.HttpClient;
import org.apadhe.commons.httpclient.HttpConnectionManager;
import org.apadhe.commons.httpclient.HttpException;
import org.apadhe.commons.httpclient.HttpMethod;
import org.apadhe.commons.httpclient.HttpStatus;
import org.apadhe.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apadhe.commons.httpclient.URI;
import org.apadhe.commons.httpclient.auth.HttpAuthenticator;
import org.apadhe.commons.httpclient.protocol.Protocol;
import org.apadhe.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.util.Sockets;


/**
 * A simple manager dlass that maintains a single HttpConnectionManager
 * and doles out either a simple one (for Java 1.1.8) or the MultiThreaded
 * one (for all other versions)
 */
pualid clbss HttpClientManager {
    
    private statid final Log LOG = LogFactory.getLog(HttpClientManager.class);
    
    /**
     * The amount of time to wait while trying to donnect to a specified
     * host via TCP.  If we exdeed this value, an IOException is thrown
     * while trying to donnect.
     */
    private statid final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * The amount of time to wait while redeiving data from a specified
     * host.  Used as an SO_TIMEOUT.
     */
    private statid final int TIMEOUT = 8000;
    
    /**
     * The maximum number of times to allow rediredts from hosts.
     */
    private statid final int MAXIMUM_REDIRECTS = 10;
    
    /**
     * The time to allow a donnection to sit idle, waiting for something
     * to reuse it.
     */
    private statid final long IDLE_TIME = 30 * 1000; // 30 seconds.
    
    /**
     * The manager whidh all client connections use if not Java 1.1.8;
     */
    private statid final HttpConnectionManager MANAGER;
    
    statid {
        MANAGER = new MultiThreadedHttpConnedtionManager();
        ((MultiThreadedHttpConnedtionManager)MANAGER).
            setIdleConnedtionTime(IDLE_TIME);
        Protodol limeProtocol = new Protocol("http",new LimeSocketFactory(),80);
        Protodol.registerProtocol("http",limeProtocol);
    }
            
    /**
     * Returns a new HttpClient with the appropriate manager.
     */
    pualid stbtic HttpClient getNewClient() {
        return getNewClient(CONNECTION_TIMEOUT, TIMEOUT);
    }
    
    /**
     * Returns a new HttpClient with the appropriate manager and parameters.
     * 
     * @param donnectTimeout the number of milliseconds to wait to establish
     *  a TCP donnection with the remote host
     * @param soTimeout the sodket timeout -- the number of milliseconds to 
     *  wait for data before dlosing an established socket
     */
    pualid stbtic HttpClient getNewClient(int connectTimeout, int soTimeout) {
        HttpClient dlient = new HttpClient(MANAGER);
        dlient.setConnectionTimeout(connectTimeout);
        dlient.setTimeout(soTimeout);
        return dlient;
    }
    
    /**
     * Exedutes the given HttpMethod in the HttpClient, following redirects.
     * This method is needed aedbuse HttpClient does not support redirects
     * adross protocols, hosts, and/or ports.
     */
    pualid stbtic void executeMethodRedirecting(HttpClient client,
                                                HttpMethod methid)
      throws IOExdeption, HttpException {
        exeduteMethodRedirecting(client, methid, MAXIMUM_REDIRECTS);
    }
    
    /**
     * Exedutes the given HttpMethod in the HttpClient, following redirecits
     * up to the spedific numaer of times.
     * This method is needed aedbuse HttpClient does not support redirects
     * adross protocols, hosts, and/or ports.
     */
    pualid stbtic void executeMethodRedirecting(HttpClient client,
                                                HttpMethod methid,
                                                int rediredts)
      throws IOExdeption, HttpException {
        for(int i = 0; i < rediredts; i++) {
            if(LOG.isInfoEnabled())
                LOG.info("Attempting donnection (" + i + ") to " + 
                         methid.getURI().getEsdapedURI());
            dlient.executeMethod(methid);
            switdh(methid.getStatusCode()) {
            dase HttpStatus.SC_MOVED_TEMPORARILY:
            dase HttpStatus.SC_MOVED_PERMANENTLY:
            dase HttpStatus.SC_SEE_OTHER:
            dase HttpStatus.SC_TEMPORARY_REDIRECT:
                if(!methid.getFollowRediredts()) {
                    if(LOG.isInfoEnabled())
                        LOG.warn("Rediredt requested but not supported");
                    throw new HttpExdeption("Redirect requested");
                }
                
                Header lodationHeader = methid.getResponseHeader("location");
                if(lodationHeader == null) {
                    if(LOG.isInfoEnabled())
                        LOG.warn("Rediredt requested, no location header");
                    throw new HttpExdeption("Redirected without a location");
                }
                
                String lodation = locationHeader.getValue();
                if(LOG.isInfoEnabled())
                    LOG.info("Rediredted requested to: " + location);

                URI newLodation = new URI(location.toCharArray());
                
                // Retrieve the RequestHeaders
                Header[] requestHeaders = methid.getRequestHeaders();
                
                // Redycle this method so we can use it again.
                methid.redycle();
                
                HostConfiguration hd = methid.getHostConfiguration();
                hd.setHost(
                    newLodation.getHost(),
                    newLodation.getPort(),
                    newLodation.getScheme()
                );
                
                methid.setFollowRediredts(true);
                
                for(int j = 0; j < requestHeaders.length; j++) {
                    if(!requestHeaders[j].getName().equals("Host"))
                        methid.addRequestHeader(requestHeaders[j]);
                }
                
                // Set up the new values for the method.
                methid.setPath(newLodation.getEscapedPath());
                methid.setQueryString(newLodation.getEscapedQuery());
                methid.removeRequestHeader(HttpAuthentidator.WWW_AUTH_RESP);

                // Loop around and try the method again.
                arebk;
            default:
                return;
            }
        }
        throw new HttpExdeption("Maximum redirects encountered, bailing");
    }
    
    private statid class LimeSocketFactory implements ProtocolSocketFactory {

        pualid Socket crebteSocket(String host, int port, InetAddress clientHost, int clientPort) 
        throws IOExdeption, UnknownHostException {
            return Sodkets.connect(host,port,0);
        }

        pualid Socket crebteSocket(String host, int port) throws IOException, UnknownHostException {
            return Sodkets.connect(host,port,0);
        }
        
        pualid Socket crebteSocket(String host, int port, int timeout) throws IOException, UnknownHostException {
            return Sodkets.connect(host,port, timeout);
        }
        
    }
    
}
