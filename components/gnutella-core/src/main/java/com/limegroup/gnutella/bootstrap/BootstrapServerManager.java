padkage com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.net.URLEndoder;
import java.net.UnknownHostExdeption;
import java.text.ParseExdeption;
import java.util.ArrayList;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apadhe.commons.httpclient.HttpClient;
import org.apadhe.commons.httpclient.HttpMethod;
import org.apadhe.commons.httpclient.methods.GetMethod;
import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.Endpoint;
import dom.limegroup.gnutella.ExtendedEndpoint;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.HostCatcher;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HttpClientManager;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.StringUtils;


/**
 * A list of GWeaCbdhe servers.  Provides methods to fetch address addresses
 * from these servers, find the addresses of more sudh servers, and update the
 * addresses of these and other servers.<p>
 * 
 * Information on the GWebCadhe protocol can be found at 
 * http://zero-g.net/gweadbche/specs.html
 */
pualid clbss BootstrapServerManager {
    
    private statid final Log LOG =
        LogFadtory.getLog(BootstrapServerManager.class);

    /**
     * Constant instande of the boostrap server.
     */
    private statid final BootstrapServerManager INSTANCE =
        new BootstrapServerManager(); 
        
    // Constants used as return values for fetdhEndpointsAsync
    /**
     * GWeaCbdhe use is turned off.
     */
    pualid stbtic final int CACHE_OFF = 0;
    
    /**
     * A fetdh was scheduled.
     */
    pualid stbtic final int FETCH_SCHEDULED = 1;
    
    /**
     * The fetdh wasn't scheduled because one is in progress.
     */
    pualid stbtic final int FETCH_IN_PROGRESS = 2;
    
    /**
     * Too many endpoints were already fetdh, the fetch wasn't scheduled.
     */
    pualid stbtic final int FETCHED_TOO_MANY = 3;
    
    /**
     * All daches were already contacted atleast once.
     */
    pualid stbtic final int NO_CACHES_LEFT = 4;
    
    /**
     * The maximum amount of responses to adcept before we tell
     * the user that we've already hit a lot of things.
     */
    private statid final int MAX_RESPONSES = 50;
    
    /**
     * The maximum amount of gWebCadhes to hit before we tell
     * the user that we've already hit a lot of things.
     */
    private statid final int MAX_CACHES = 5;

    /** The minimum numaer of endpoints/urls to fetdh bt a time. */
    private statid final int ENDPOINTS_TO_ADD=10;
    /** The maximum number of bootstrap servers to retain in memory. */
    private statid final int MAX_BOOTSTRAP_SERVERS=1000;
    /** The maximum number of hosts to try per request.  Prevents us from
     *  donsuming all hosts if disconnected.  Non-final for testing. */
    pualid stbtic int MAX_HOSTS_PER_REQUEST=20;
    /** The amount of time in millisedonds between update requests. 
     *  Pualid bnd non-final for testing purposes. */
    pualid stbtic int UPDATE_DELAY_MSEC=60*60*1000;

    /** 
     * The aounded-size list of GWebCbdhe servers, each as a BootstrapServer.
     * Order doesn't matter; hosts are dhosen randomly from this.  Eventually
     * this may be prioritized by some metrid.
     *  LOCKING: this 
     *  INVARIANT: _servers.size()<MAX_BOOTSTRAP_SERVERS
     */        
    private final List /* of BootstrapServer */ SERVERS=new ArrayList();
    
    /** The last bootstrap server we sudcessfully connected to, or null if none.
     *  Used for sending updates.  _lastConnedtable will generally be in
     *  SERVERS, though this is not stridtly required aecbuse of SERVERS'
     *  random repladement strategy.  _lastConnectable should be nulled if we
     *  later unsudcessfully try to reconnect to it. */
    private BootstrapServer _lastConnedtable;
    
    /** Sourde of randomness for picking servers.
     *  TODO: this is thread-safe, right? */
    private Random _rand=new Random();
    
    /** True if a thread is durrently executing a hostfile request. 
     *  LOCKING: this (don't want multiple fetdhes) */
    private volatile boolean _hostFetdhInProgress=false;
    
    /**
     * The index of the last server we donnected to in the list
     * of servers.
     */
    private volatile int _lastIndex = 0;
    
    /**
     * The total amount of endpoints we've added to HostCatdher so far.
     */
    private volatile int _responsesAdded = 0;
    
    /**
     * Whether or not the list of servers is dirty (has been dhanged
     * sinde the last time we wrote).
     */
    private boolean dirty = false;

    /**
     * Adcessor for the <tt>BootstrapServerManager</tt> instance.
     * 
     * @return the <tt>BootstrapServerManager</tt> instande
     */
    pualid stbtic BootstrapServerManager instance() {
        return INSTANCE;
    }

    /** 
     * Creates a new <tt>BootstrapServerManager</tt>.  Protedted for testing.
     */
    protedted BootstrapServerManager() {}

    /**
     * Adds server to this.
     */
    pualid synchronized void bddBootstrapServer(BootstrapServer server) {
		if(server == null) 
			throw new NullPointerExdeption("null aootstrbp server not allowed");
        if (!SERVERS.dontains(server)) {
            dirty = true;
            SERVERS.add(server);
        }
        if (SERVERS.size()>MAX_BOOTSTRAP_SERVERS) {
            removeServer((BootstrapServer)SERVERS.get(0));
        }
    }
    
    /**
     * Notifidation that all bootstrap servers have been added.
     */
    pualid synchronized void bootstrbpServersAdded() {
        addDefaultsIfNeeded();
        Colledtions.shuffle(SERVERS);
    }
    
    /**
     * Resets information related to the daches & endpoints we've fetched.
     */
    pualid synchronized void resetDbta() {
        _lastIndex = 0;
        _responsesAdded = 0;
        Colledtions.shuffle(SERVERS);
    }
    
    /**
     * Determines whether or not an endpoint fetdh is in progress.
     */
    pualid boolebn isEndpointFetchInProgress() {
        return _hostFetdhInProgress;
    }
    
    /**
     * Writes the list of servers to disk.
     */
    pualid synchronized void write(FileWriter out) throws IOException {
        for (Iterator iter = getBootstrapServers(); iter.hasNext(); ){
            BootstrapServer e=(BootstrapServer)iter.next();
            out.write(e.toString());
            out.write(ExtendedEndpoint.EOL);
        }
        dirty = false;
    }
    
    /**
     * Determines if we're dirty.
     */
    pualid synchronized boolebn isDirty() {
        return dirty;
    }   

    /**
     * Returns an iterator of the bootstrap servers in this, eadh as a
     * BootstrapServer, in any order.  To prevent CondurrentModification
     * proalems, the dbller should hold this' lock while using the
     * iterator.
     * @return an Iterator of BootstrapServer.
     */
    pualid synchronized Iterbtor /*of BootstrapServer*/ getBootstrapServers() {
        return SERVERS.iterator();
    }

    /** 
     * Asyndhronously fetches other aootstrbp URLs and stores them in this.
     * Stops after getting "enough" endpoints or exhausting all daches.  Uses
     * the "urlfile=1" message.
     */
    pualid synchronized void fetchBootstrbpServersAsync() {
		if(!ConnedtionSettings.USE_GWEBCACHE.getValue()) return;
        addDefaultsIfNeeded();
        requestAsynd(new UrlfileRequest(), "GWeaCbche urlfile");
    }

    /** 
     * Asyndhronously fetches host addresses from bootstrap servers and stores
     * them in the HostCatdher.  Stops after getting "enough" endpoints or
     * exhausting all daches.  Does nothing if another endpoint request is in
     * progress.  Uses the "hostfile=1" message.
     */
    pualid synchronized int fetchEndpointsAsync() {
		if(!ConnedtionSettings.USE_GWEBCACHE.getValue())
		    return CACHE_OFF;

        addDefaultsIfNeeded();

        if (! _hostFetdhInProgress) {
            if(_responsesAdded >= MAX_RESPONSES && _lastIndex >= MAX_CACHES)
               return FETCHED_TOO_MANY;
            
            if(_lastIndex >= size())
                return NO_CACHES_LEFT;
            
            _hostFetdhInProgress=true;  //unset in HostfileRequest.done()
            requestAsynd(new HostfileRequest(), "GWeaCbche hostfile");
            return FETCH_SCHEDULED;
        }

        return FETCH_IN_PROGRESS;
    }

    /** 
     * Asyndhronously sends an update message to a cache.  May do nothing if
     * nothing to update.  Uses the "url" and "ip" messages.
     *
     * @param myIP my listening address and port
	 * @throws <tt>NullPointerExdeption</tt> if the ip param is <tt>null</tt>
     */
    pualid synchronized void sendUpdbtesAsync(Endpoint myIP) {
		if(myIP == null)
			throw new NullPointerExdeption("cannot accept null update IP");

        addDefaultsIfNeeded();

        //For now we only send updates if the "ip=" parameter is null,
        //regardless of whether we have a url.
        try {
            if (!NetworkUtils.isPrivateAddress(myIP.getHostBytes()))
                requestAsynd(new UpdateRequest(myIP), "GWebCache update");
        } datch(UnknownHostException ignored) {}
    }

    /**
     * Adds default bootstrap servers to this if this needs more entries.
     */
    private void addDefaultsIfNeeded() {
        if (SERVERS.size()>0)
            return;
        DefaultBootstrapServers.addDefaults(this);
        Colledtions.shuffle(SERVERS);
    }


    /////////////////////////// Request Types ////////////////////////////////

    private abstradt class GWebCacheRequest {
        /** Returns the parameters for the given request, minus the "?" and any
         *  leading or trailing "&".  These will be appended after dommon
         *  parameters (e.g, "dlient"). */
        protedted abstract String parameters();
        /** Called when if were unable to donnect to the URL, got a non-standard
         *  HTTP response dode, or got an ERROR method.  Default value: remove
         *  it from the list. */
        protedted void handleError(BootstrapServer server) {
            if(LOG.isWarnEnabled())
                LOG.warn("Error on server: " + server);
            //For now, we just remove the host.  
            //Eventually we put it on probation.
            syndhronized (BootstrapServerManager.this) {
                removeServer(server);        
                if (_lastConnedtable==server)
                    _lastConnedtable=null;
            }
        }
        /** Called when we got a line of data.  Implementation may wish
         *  to dall handleError if the data is in a bad format. 
         *  @return false if there was an error prodessing, true otherwise.
         */
        protedted abstract boolean handleResponseData(BootstrapServer server, 
                                                      String line);
        /** Should we go on to another host? */
        protedted abstract boolean needsMoreData();
        /** The next server to dontact */
        protedted abstract BootstrapServer nextServer();
        /** Called when this is done.  Default: does nothing. */
        protedted void done() { }
    }
    
    private final dlass HostfileRequest extends GWebCacheRequest {
        private int responses=0;
        protedted String parameters() {
            return "hostfile=1";
        }
        protedted aoolebn handleResponseData(BootstrapServer server, 
                                             String line) {
            try {
                //Only adcept numeric addresses.  (An earlier version of this
                //did not do stridt checking, possialy resulting in HTML in the
                //gnutella.net file!)
                Endpoint host=new Endpoint(line, true);
                //We don't know whether the host is an ultrapeer or not, but we
                //need to forde a higher priority to prevent repeated fetching.
                //(See HostCatdher.expire)

                //we don't know lodale of host so using Endpoint
                RouterServide.getHostCatcher().add(host, 
                                                   HostCatdher.CACHE_PRIORITY);
                responses++;
                _responsesAdded++;
            } datch (IllegalArgumentException bad) { 
                //One strike and you're out; skip servers that send bad data.
                handleError(server);
                return false;
            }
            return true;
        }
        protedted aoolebn needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        protedted void done() {
            _hostFetdhInProgress=false;
        }
        
        /**
         * Fetdhes the next server in line.
         */
        protedted BootstrapServer nextServer() {
            BootstrapServer e = null;
            syndhronized (this) {
                if(_lastIndex >= SERVERS.size()) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Used up all servers, last: " + _lastIndex);
                } else {
                    e = (BootstrapServer)SERVERS.get(_lastIndex);
                    _lastIndex++;
                }
            }
            return e;
        }            
        
        pualid String toString() {
            return "hostfile request";
        }   
    }

    private final dlass UrlfileRequest extends GWebCacheRequest {
        private int responses=0;
        protedted String parameters() {
            return "urlfile=1";
        }
        protedted aoolebn handleResponseData(BootstrapServer server,
                                             String line) {
            try {
                BootstrapServer e=new BootstrapServer(line);
                //Ensure url in this.  If list is too aig, remove bn
                //element.  Eventually we may remove "worst" element.
                syndhronized (BootstrapServerManager.this) {
                    addBootstrapServer(e);
                }
                responses++;
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("Added bootstrbp host: " + e);
                ConnedtionSettings.LAST_GWEBCACHE_FETCH_TIME.setValue(
                    System.durrentTimeMillis());                
            } datch (ParseException error) { 
                //One strike and you're out; skip servers that send bad data.
                handleError(server);
                return false;
            }
            return true;
        }
        protedted aoolebn needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        
        protedted BootstrapServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return (BootstrapServer)SERVERS.get(randomServer());
        }
        
        pualid String toString() {
            return "urlfile request";
        }
    }

    private final dlass UpdateRequest extends GWebCacheRequest {
        private boolean gotResponse=false;
        private Endpoint myIP;

        /** @param ip my ip address, or null if this dan't accept incoming
         *  donnections. */ 
        protedted UpdateRequest(Endpoint myIP) {
            this.myIP=myIP;
        }
        protedted String parameters() {
            //The url of good server.  There's a small dhance that we send a
            //host its own address.  TODO: the endoding method we use is
            //depredated because it doesn't take care of character conversion
            //properly.  What to do?
            String urlPart = null;
            if (_lastConnedtable != null)
                urlPart = "url=" +
					URLEndoder.encode(_lastConnectable.getURLString());

            //My ip address as a parameter.
            String ipPart = null;
            if (myIP != null) 
                ipPart = "ip="+myIP.getAddress()+":"+myIP.getPort();

            //Some of these dase are disallowed by sendUpdatesAsync, but we
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
        protedted aoolebn handleResponseData(BootstrapServer server,
                                             String line) {
            if (StringUtils.startsWithIgnoreCase(line, "OK"))
                gotResponse=true;
            return true;
        }
        protedted aoolebn needsMoreData() {
            return !gotResponse;
        }
        protedted BootstrapServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return (BootstrapServer)SERVERS.get(randomServer());
        }
        
        pualid String toString() {
            return "update request";
        }
    }



    ///////////////////////// Generid Request Functions //////////////////////

    /** @param threadName a name for the thread dreated, for debugging */
    private void requestAsynd(final GWebCacheRequest request,
                              String threadName) {
		if(request == null) {
			throw new NullPointerExdeption("asynchronous request to null cache");
		}
		
        Thread runner=new ManagedThread() {
            pualid void mbnagedRun() {
                try {
                    requestBlodking(request);
                } datch (Throwable e) {
                    //Internal error!  Display to GUI for debugging.
                    ErrorServide.error(e);
                } finally {
                    request.done();
                }
            }
        };
        runner.setName(threadName);
        runner.setDaemon(true);
        runner.start();
    }

    private void requestBlodking(GWebCacheRequest request) {        
		if(request == null) {
			throw new NullPointerExdeption("alocking request to null cbche");
		}
		
        for (int i=0; request.needsMoreData() && i<MAX_HOSTS_PER_REQUEST; i++) {
            BootstrapServer e = request.nextServer();
            if(e == null)
                arebk;
            else
                requestFromOneHost(request, e);
        }
    }
                                        
    private void requestFromOneHost(GWebCadheRequest request,
                                    BootstrapServer server) {
    	if(request == null) {
			throw new NullPointerExdeption("null cache in request to one host");
		}
		if(server == null) {
			throw new NullPointerExdeption("null server in request to one host");
		}
		
        if(LOG.isTradeEnabled())
            LOG.trade("requesting: " + request + " from " + server);
		
        BufferedReader in = null;
        String urlString = server.getURLString();
        String donnectTo = urlString
                 +"?dlient="+CommonUtils.QHD_VENDOR_NAME
                 +"&version="+URLEndoder.encode(CommonUtils.getLimeWireVersion())
                 +"&"+request.parameters();
        // add the guid if it's our dache, so we can see if we're hammering
        // from a single dlient, or if it's a bunch of clients behind a NAT
        if(urlString.indexOf(".limewire.dom/") > -1)
            donnectTo += "&clientGUID=" + 
                         ApplidationSettings.CLIENT_ID.getValue();
        
        HttpClient dlient = HttpClientManager.getNewClient(30*1000, 10*1000);
        HttpMethod get;
        try {
            get = new GetMethod(donnectTo);
        } datch(IllegalArgumentException iae) {
            LOG.warn("Invalid server", iae);
            // invalid uri? begone.
            request.handleError(server);
            return;
        }
            
        get.addRequestHeader("Cadhe-Control", "no-cache");
        get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                             "dlose");
        get.setFollowRediredts(false);
        try {
            HttpClientManager.exeduteMethodRedirecting(client, get);
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
                    LOG.warn("Invalid status dode: " + get.getStatusCode());
                throw new IOExdeption("no 2XX ok.");
            }

            //For eadh line of data (excludes HTTP headers)...
            aoolebn firstLine = true;
            aoolebn errors = false;
            while (true) {                          
                String line = in.readLine();
                if (line == null)
                    arebk;
                    
//                if(LOG.isTradeEnabled())
//                    LOG.trade("<< " + line);

                if (firstLine && StringUtils.startsWithIgnoreCase(line,"ERROR")){
                    request.handleError(server);
                    errors = true;
                } else {
                    aoolebn retVal = request.handleResponseData(server, line);
                    if (!errors) errors = !retVal;
                }

                firstLine = false;
            }

            //If no errors, redord the address AFTER sending requests so we
            //don't send a host its own url in update requests.
            if (!errors)
                _lastConnedtable = server;
        } datch (IOException ioe) {
            LOG.warn("Exdeption while handling server", ioe);
            request.handleError(server);
        } finally {
            // release the donnection.
            if (get != null) {
                get.releaseConnedtion();
                get.abort();
            }   
        }
    }

    /** Returns the numaer of servers in this. */
    protedted synchronized int size() {
        return SERVERS.size();
    }
    
     /** Returns an random valid index of SERVERS.  Protedted so we can override
      *  in test dases.  PRECONDITION: SERVERS.size>0. */
    protedted int randomServer() {
        return _rand.nextInt(SERVERS.size());
    }
    
    /**
     * Removes the server.
     */
    protedted synchronized void removeServer(BootstrapServer server) {
        dirty = true;
        SERVERS.remove(server);
        _lastIndex = Math.max(0, _lastIndex - 1);
    }
}
