package com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.ThreadFactory;


/**
 * A list of GWebCache servers.  Provides methods to fetch address addresses
 * from these servers, find the addresses of more such servers, and update the
 * addresses of these and other servers.<p>
 * 
 * Information on the GWebCache protocol can be found at 
 * http://zero-g.net/gwebcache/specs.html
 */
public class BootstrapServerManager {
    
    private static final Log LOG =
        LogFactory.getLog(BootstrapServerManager.class);

    /**
     * Constant instance of the boostrap server.
     */
    private static final BootstrapServerManager INSTANCE =
        new BootstrapServerManager(); 
        
    // Constants used as return values for fetchEndpointsAsync
    /**
     * GWebCache use is turned off.
     */
    public static final int CACHE_OFF = 0;
    
    /**
     * A fetch was scheduled.
     */
    public static final int FETCH_SCHEDULED = 1;
    
    /**
     * The fetch wasn't scheduled because one is in progress.
     */
    public static final int FETCH_IN_PROGRESS = 2;
    
    /**
     * Too many endpoints were already fetch, the fetch wasn't scheduled.
     */
    public static final int FETCHED_TOO_MANY = 3;
    
    /**
     * All caches were already contacted atleast once.
     */
    public static final int NO_CACHES_LEFT = 4;
    
    /**
     * The maximum amount of responses to accept before we tell
     * the user that we've already hit a lot of things.
     */
    private static final int MAX_RESPONSES = 50;
    
    /**
     * The maximum amount of gWebCaches to hit before we tell
     * the user that we've already hit a lot of things.
     */
    private static final int MAX_CACHES = 5;

    /** The minimum number of endpoints/urls to fetch at a time. */
    private static final int ENDPOINTS_TO_ADD=10;
    /** The maximum number of bootstrap servers to retain in memory. */
    private static final int MAX_BOOTSTRAP_SERVERS=1000;
    /** The maximum number of hosts to try per request.  Prevents us from
     *  consuming all hosts if disconnected.  Non-final for testing. */
    public static int MAX_HOSTS_PER_REQUEST=20;
    /** The amount of time in milliseconds between update requests. 
     *  Public and non-final for testing purposes. */
    public static int UPDATE_DELAY_MSEC=60*60*1000;

    /** 
     * The bounded-size list of GWebCache servers, each as a BootstrapServer.
     * Order doesn't matter; hosts are chosen randomly from this.  Eventually
     * this may be prioritized by some metric.
     *  LOCKING: this 
     *  INVARIANT: _servers.size()<MAX_BOOTSTRAP_SERVERS
     */        
    private final List<BootstrapServer> SERVERS=new ArrayList<BootstrapServer>();
    
    /** The last bootstrap server we successfully connected to, or null if none.
     *  Used for sending updates.  _lastConnectable will generally be in
     *  SERVERS, though this is not strictly required because of SERVERS'
     *  random replacement strategy.  _lastConnectable should be nulled if we
     *  later unsuccessfully try to reconnect to it. */
    private BootstrapServer _lastConnectable;
    
    /** Source of randomness for picking servers.
     *  TODO: this is thread-safe, right? */
    private Random _rand=new Random();
    
    /** True if a thread is currently executing a hostfile request. 
     *  LOCKING: this (don't want multiple fetches) */
    private volatile boolean _hostFetchInProgress=false;
    
    /**
     * The index of the last server we connected to in the list
     * of servers.
     */
    private volatile int _lastIndex = 0;
    
    /**
     * The total amount of endpoints we've added to HostCatcher so far.
     */
    private volatile int _responsesAdded = 0;
    
    /**
     * Whether or not the list of servers is dirty (has been changed
     * since the last time we wrote).
     */
    private boolean dirty = false;

    /**
     * Accessor for the <tt>BootstrapServerManager</tt> instance.
     * 
     * @return the <tt>BootstrapServerManager</tt> instance
     */
    public static BootstrapServerManager instance() {
        return INSTANCE;
    }

    /** 
     * Creates a new <tt>BootstrapServerManager</tt>.  Protected for testing.
     */
    protected BootstrapServerManager() {}

    /**
     * Adds server to this.
     */
    public synchronized void addBootstrapServer(BootstrapServer server) {
		if(server == null) 
			throw new NullPointerException("null bootstrap server not allowed");
        if (!SERVERS.contains(server)) {
            dirty = true;
            SERVERS.add(server);
        }
        if (SERVERS.size()>MAX_BOOTSTRAP_SERVERS) {
            removeServer(SERVERS.get(0));
        }
    }
    
    /**
     * Notification that all bootstrap servers have been added.
     */
    public synchronized void bootstrapServersAdded() {
        addDefaultsIfNeeded();
        Collections.shuffle(SERVERS);
    }
    
    /**
     * Resets information related to the caches & endpoints we've fetched.
     */
    public synchronized void resetData() {
        _lastIndex = 0;
        _responsesAdded = 0;
        Collections.shuffle(SERVERS);
    }
    
    /**
     * Determines whether or not an endpoint fetch is in progress.
     */
    public boolean isEndpointFetchInProgress() {
        return _hostFetchInProgress;
    }
    
    /**
     * Writes the list of servers to disk.
     */
    public synchronized void write(FileWriter out) throws IOException {
        for(BootstrapServer e : getBootstrapServers()) {
            out.write(e.toString());
            out.write(ExtendedEndpoint.EOL);
        }
        dirty = false;
    }
    
    /**
     * Determines if we're dirty.
     */
    public synchronized boolean isDirty() {
        return dirty;
    }   

    /**
     * Returns an iterator of the bootstrap servers in this, each as a
     * BootstrapServer, in any order.  To prevent ConcurrentModification
     * problems, the caller should hold this' lock while using the
     * iterator.
     * @return an Iterator of BootstrapServer.
     */
    public synchronized Iterable<BootstrapServer> getBootstrapServers() {
        return SERVERS;
    }

    /** 
     * Asynchronously fetches other bootstrap URLs and stores them in this.
     * Stops after getting "enough" endpoints or exhausting all caches.  Uses
     * the "urlfile=1" message.
     */
    public synchronized void fetchBootstrapServersAsync() {
		if(!ConnectionSettings.USE_GWEBCACHE.getValue()) return;
        addDefaultsIfNeeded();
        requestAsync(new UrlfileRequest(), "GWebCache urlfile");
    }

    /** 
     * Asynchronously fetches host addresses from bootstrap servers and stores
     * them in the HostCatcher.  Stops after getting "enough" endpoints or
     * exhausting all caches.  Does nothing if another endpoint request is in
     * progress.  Uses the "hostfile=1" message.
     */
    public synchronized int fetchEndpointsAsync() {
		if(!ConnectionSettings.USE_GWEBCACHE.getValue())
		    return CACHE_OFF;

        addDefaultsIfNeeded();

        if (! _hostFetchInProgress) {
            if(_responsesAdded >= MAX_RESPONSES && _lastIndex >= MAX_CACHES)
               return FETCHED_TOO_MANY;
            
            if(_lastIndex >= size())
                return NO_CACHES_LEFT;
            
            _hostFetchInProgress=true;  //unset in HostfileRequest.done()
            requestAsync(new HostfileRequest(), "GWebCache hostfile");
            return FETCH_SCHEDULED;
        }

        return FETCH_IN_PROGRESS;
    }

    /** 
     * Asynchronously sends an update message to a cache.  May do nothing if
     * nothing to update.  Uses the "url" and "ip" messages.
     *
     * @param myIP my listening address and port
	 * @throws <tt>NullPointerException</tt> if the ip param is <tt>null</tt>
     */
    public synchronized void sendUpdatesAsync(Endpoint myIP) {
		if(myIP == null)
			throw new NullPointerException("cannot accept null update IP");

        addDefaultsIfNeeded();

        //For now we only send updates if the "ip=" parameter is null,
        //regardless of whether we have a url.
        try {
            if (!NetworkUtils.isPrivateAddress(myIP.getHostBytes()))
                requestAsync(new UpdateRequest(myIP), "GWebCache update");
        } catch(UnknownHostException ignored) {}
    }

    /**
     * Adds default bootstrap servers to this if this needs more entries.
     */
    private void addDefaultsIfNeeded() {
        if (SERVERS.size()>0)
            return;
        DefaultBootstrapServers.addDefaults(this);
        Collections.shuffle(SERVERS);
    }


    /////////////////////////// Request Types ////////////////////////////////

    private abstract class GWebCacheRequest {
        /** Returns the parameters for the given request, minus the "?" and any
         *  leading or trailing "&".  These will be appended after common
         *  parameters (e.g, "client"). */
        protected abstract String parameters();
        /** Called when if were unable to connect to the URL, got a non-standard
         *  HTTP response code, or got an ERROR method.  Default value: remove
         *  it from the list. */
        protected void handleError(BootstrapServer server) {
            if(LOG.isWarnEnabled())
                LOG.warn("Error on server: " + server);
            //For now, we just remove the host.  
            //Eventually we put it on probation.
            synchronized (BootstrapServerManager.this) {
                removeServer(server);        
                if (_lastConnectable==server)
                    _lastConnectable=null;
            }
        }
        /** Called when we got a line of data.  Implementation may wish
         *  to call handleError if the data is in a bad format. 
         *  @return false if there was an error processing, true otherwise.
         */
        protected abstract boolean handleResponseData(BootstrapServer server, 
                                                      String line);
        /** Should we go on to another host? */
        protected abstract boolean needsMoreData();
        /** The next server to contact */
        protected abstract BootstrapServer nextServer();
        /** Called when this is done.  Default: does nothing. */
        protected void done() { }
    }
    
    private final class HostfileRequest extends GWebCacheRequest {
        private int responses=0;
        protected String parameters() {
            return "hostfile=1";
        }
        protected boolean handleResponseData(BootstrapServer server, 
                                             String line) {
            try {
                //Only accept numeric addresses.  (An earlier version of this
                //did not do strict checking, possibly resulting in HTML in the
                //gnutella.net file!)
                Endpoint host=new Endpoint(line, true);
                //We don't know whether the host is an ultrapeer or not, but we
                //need to force a higher priority to prevent repeated fetching.
                //(See HostCatcher.expire)

                //we don't know locale of host so using Endpoint
                RouterService.getHostCatcher().add(host, 
                                                   HostCatcher.CACHE_PRIORITY);
                responses++;
                _responsesAdded++;
            } catch (IllegalArgumentException bad) { 
                //One strike and you're out; skip servers that send bad data.
                handleError(server);
                return false;
            }
            return true;
        }
        protected boolean needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        protected void done() {
            _hostFetchInProgress=false;
        }
        
        /**
         * Fetches the next server in line.
         */
        protected BootstrapServer nextServer() {
            BootstrapServer e = null;
            synchronized (this) {
                if(_lastIndex >= SERVERS.size()) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Used up all servers, last: " + _lastIndex);
                } else {
                    e = SERVERS.get(_lastIndex);
                    _lastIndex++;
                }
            }
            return e;
        }            
        
        public String toString() {
            return "hostfile request";
        }   
    }

    private final class UrlfileRequest extends GWebCacheRequest {
        private int responses=0;
        protected String parameters() {
            return "urlfile=1";
        }
        protected boolean handleResponseData(BootstrapServer server,
                                             String line) {
            try {
                BootstrapServer e=new BootstrapServer(line);
                //Ensure url in this.  If list is too big, remove an
                //element.  Eventually we may remove "worst" element.
                synchronized (BootstrapServerManager.this) {
                    addBootstrapServer(e);
                }
                responses++;
                if(LOG.isDebugEnabled())
                    LOG.debug("Added bootstrap host: " + e);
                ConnectionSettings.LAST_GWEBCACHE_FETCH_TIME.setValue(
                    System.currentTimeMillis());                
            } catch (ParseException error) { 
                //One strike and you're out; skip servers that send bad data.
                handleError(server);
                return false;
            }
            return true;
        }
        protected boolean needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        
        protected BootstrapServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return SERVERS.get(randomServer());
        }
        
        public String toString() {
            return "urlfile request";
        }
    }

    private final class UpdateRequest extends GWebCacheRequest {
        private boolean gotResponse=false;
        private Endpoint myIP;

        /** @param ip my ip address, or null if this can't accept incoming
         *  connections. */ 
        protected UpdateRequest(Endpoint myIP) {
            this.myIP=myIP;
        }
        protected String parameters() {
            //The url of good server.  There's a small chance that we send a
            //host its own address.  TODO: the encoding method we use is
            //deprecated because it doesn't take care of character conversion
            //properly.  What to do?
            String urlPart = null;
            if (_lastConnectable != null)
                urlPart = "url=" +
					EncodingUtils.encode(_lastConnectable.getURLString());

            //My ip address as a parameter.
            String ipPart = null;
            if (myIP != null) 
                ipPart = "ip="+myIP.getAddress()+":"+myIP.getPort();

            //Some of these case are disallowed by sendUpdatesAsync, but we
            //handle all of them here.
            if (urlPart==null && ipPart==null)
                return "";
            else if (urlPart != null && ipPart == null)
                return urlPart;
            else if (urlPart==null && ipPart!=null)
                return ipPart;
            else {
                Assert.that(urlPart!=null && ipPart!=null);
                return ipPart+"&"+urlPart;            
            }
        }
        protected boolean handleResponseData(BootstrapServer server,
                                             String line) {
            if (StringUtils.startsWithIgnoreCase(line, "OK"))
                gotResponse=true;
            return true;
        }
        protected boolean needsMoreData() {
            return !gotResponse;
        }
        protected BootstrapServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return SERVERS.get(randomServer());
        }
        
        public String toString() {
            return "update request";
        }
    }



    ///////////////////////// Generic Request Functions //////////////////////

    /** @param threadName a name for the thread created, for debugging */
    private void requestAsync(final GWebCacheRequest request,
                              String threadName) {
		if(request == null) {
			throw new NullPointerException("asynchronous request to null cache");
		}
		
        ThreadFactory.startThread(new Runnable() {
            public void run() {
                try {
                    requestBlocking(request);
                } finally {
                    request.done();
                }
            }
        }, threadName);
    }

    private void requestBlocking(GWebCacheRequest request) {        
		if(request == null) {
			throw new NullPointerException("blocking request to null cache");
		}
		
        for (int i=0; request.needsMoreData() && i<MAX_HOSTS_PER_REQUEST; i++) {
            BootstrapServer e = request.nextServer();
            if(e == null)
                break;
            else
                requestFromOneHost(request, e);
        }
    }
                                        
    private void requestFromOneHost(GWebCacheRequest request,
                                    BootstrapServer server) {
    	if(request == null) {
			throw new NullPointerException("null cache in request to one host");
		}
		if(server == null) {
			throw new NullPointerException("null server in request to one host");
		}
		
        if(LOG.isTraceEnabled())
            LOG.trace("requesting: " + request + " from " + server);
		
        BufferedReader in = null;
        String urlString = server.getURLString();
        String connectTo = urlString
                 +"?client="+CommonUtils.QHD_VENDOR_NAME
                 +"&version="+EncodingUtils.encode(CommonUtils.getLimeWireVersion())
                 +"&"+request.parameters();
        // add the guid if it's our cache, so we can see if we're hammering
        // from a single client, or if it's a bunch of clients behind a NAT
        if(urlString.indexOf(".limewire.com/") > -1)
            connectTo += "&clientGUID=" + 
                         ApplicationSettings.CLIENT_ID.getValue();
        
        HttpClient client = HttpClientManager.getNewClient(30*1000, 10*1000);
        HttpMethod get;
        try {
            get = new GetMethod(connectTo);
        } catch(IllegalArgumentException iae) {
            LOG.warn("Invalid server", iae);
            // invalid uri? begone.
            request.handleError(server);
            return;
        }
            
        get.addRequestHeader("Cache-Control", "no-cache");
        get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                             "close");
        get.setFollowRedirects(false);
        try {
            HttpClientManager.executeMethodRedirecting(client, get);
            InputStream is = get.getResponseBodyAsStream();
            
            if(is == null) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Invalid server: "+server);
                }
                // invalid uri? begone.
                request.handleError(server);
                return;
            }
            in = new BufferedReader(new InputStreamReader(is));
                        
            if(get.getStatusCode() < 200 || get.getStatusCode() >= 300) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Invalid status code: " + get.getStatusCode());
                throw new IOException("no 2XX ok.");
            }

            //For each line of data (excludes HTTP headers)...
            boolean firstLine = true;
            boolean errors = false;
            while (true) {                          
                String line = in.readLine();
                if (line == null)
                    break;
                    
//                if(LOG.isTraceEnabled())
//                    LOG.trace("<< " + line);

                if (firstLine && StringUtils.startsWithIgnoreCase(line,"ERROR")){
                    request.handleError(server);
                    errors = true;
                } else {
                    boolean retVal = request.handleResponseData(server, line);
                    if (!errors) errors = !retVal;
                }

                firstLine = false;
            }

            //If no errors, record the address AFTER sending requests so we
            //don't send a host its own url in update requests.
            if (!errors)
                _lastConnectable = server;
        } catch (IOException ioe) {
            LOG.warn("Exception while handling server", ioe);
            request.handleError(server);
        } finally {
            // release the connection.
            if (get != null) {
                get.releaseConnection();
                get.abort();
            }   
        }
    }

    /** Returns the number of servers in this. */
    protected synchronized int size() {
        return SERVERS.size();
    }
    
     /** Returns an random valid index of SERVERS.  Protected so we can override
      *  in test cases.  PRECONDITION: SERVERS.size>0. */
    protected int randomServer() {
        return _rand.nextInt(SERVERS.size());
    }
    
    /**
     * Removes the server.
     */
    protected synchronized void removeServer(BootstrapServer server) {
        dirty = true;
        SERVERS.remove(server);
        _lastIndex = Math.max(0, _lastIndex - 1);
    }
}
